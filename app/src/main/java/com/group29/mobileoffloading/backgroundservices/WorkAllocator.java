package com.group29.mobileoffloading.backgroundservices;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.DataModels.WorkData;
import com.group29.mobileoffloading.DataModels.WorkInfo;
import com.group29.mobileoffloading.DataModels.Worker;
import com.group29.mobileoffloading.listeners.ComputationListener;
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.FlushToFile;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class WorkAllocator {
    private final Hashtable<Integer, Integer> partitionResults = new Hashtable<>();
    private final Context context;
    private final ArrayList<Worker> workers = new ArrayList<>();
    private final int[][] matrix1;
    private final int[][] matrix2;
    private final int[][] matrix2Transpose; //This is required for computation
    private final int rows1;
    private final int cols1;
    private final int rows2;
    private final int cols2;
    private final int totalpartitions;
    private final PriorityQueue<Worker> workerQueue;
    private final BlockingQueue<Integer> workQueue = new LinkedBlockingDeque<>();
    private final ComputationListener workCallback;
    private boolean bye = false;
    private Handler handler;
    private Runnable runnable;
    private long beginTime;

    public WorkAllocator(Context context, ArrayList<Worker> workers, int[][] matrix1, int[][] matrix2, ComputationListener uiCallback) {
        this.context = context;
        this.workers.addAll(workers);

        this.matrix1 = matrix1;
        this.matrix2 = matrix2;

        this.matrix2Transpose = calculateTranspose(matrix2);

        this.rows1 = matrix1.length;
        this.rows2 = matrix2.length;

        this.cols1 = matrix1[0].length;
        this.cols2 = matrix2[0].length;

        this.workerQueue = new PriorityQueue<Worker>(this.workers.size(), new WorkerPriorityChecker());
        this.totalpartitions = this.rows1 * this.cols2;
        this.workCallback = uiCallback;
    }

    public int[][] calculateTranspose(int[][] matrix) {
        int num_rows = matrix.length;
        int num_cols = matrix[0].length;
        int[][] transpose = new int[num_cols][num_rows];

        for (int i = 0; i < num_rows; i++) {
            for (int j = 0; j < num_cols; j++) {
                transpose[j][i] = matrix[i][j];
            }
        }
        return transpose;
    }

    public void beginDistributedComputation() {
        beginTime = System.currentTimeMillis();

        sendWorkersToQueue(); //addWorkersToQueue

        addWorkToQueue(); //addWorkToQueue
        initiateWorkAssignment();
        startWorkAllocation(); //startWorkScheduler

    }

    private void startWorkAllocation() {
        handler = new Handler();
        runnable = () -> {
            if (partitionResults.size() == totalpartitions) {
                sendByeToWorkers();
            } else {
                addWorkToQueue();
                allocateWork();
                handler.postDelayed(runnable, 1000);
            }
        };

        handler.postDelayed(runnable, 1000);
    }


    private void initiateWorkAssignment() {
        for (int i = 0; i < workerQueue.size(); i++) {
            allocateWork();
        }
    }

    public void allocateWork() {
        if (workerQueue.size() > 0 && workQueue.size() > 0 && partitionResults.size() != totalpartitions) {
            Worker worker = workerQueue.poll();
            int partitionIndex = workQueue.poll();

            while (!workers.contains(worker)) {
                if (workerQueue.size() == 0) {
                    return;
                }
                worker = workerQueue.poll();
            }
            while (partitionResults.containsKey(partitionIndex)) {
                if (workQueue.size() == 0) {
                    return;
                }
                partitionIndex = workQueue.poll();
            }
            int row1 = partitionIndex / cols2;
            int col2 = partitionIndex % cols2;

            int[] rows = matrix1[row1];
            int[] cols = matrix2Transpose[col2];

            WorkData workData = new WorkData();
            workData.setPartitionIndex(partitionIndex);
            workData.setRows(rows);
            workData.setCols(cols);

            ClientPayLoad payload = new ClientPayLoad();
            payload.setTag(Constants.PayloadTags.WORK_DATA);
            payload.setData(workData);
            try {
                //TODO find out communication here : Done
                Payload payload1 = PayloadConverter.toPayload(payload);
                Connector.sendToDevice(context, worker.getEndpointId(), payload1);

            } catch (Exception e) { //IOException e
                if (!isAnyWrokerRunning(worker)) {
                    workerQueue.add(worker);
                }
                workQueue.add(partitionIndex);

                Toast.makeText(context, "Something unexpected happened : Mostly connection break", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }
    }

    public void setNewWorkers(ArrayList<Worker> workers) {
        this.workers.clear();
        this.workers.addAll(workers);
    }

    public void removeWorker(String endpointId) {
        for (int i = 0; i < workers.size(); i++) {
            if (workers.get(i).getEndpointId().equals(endpointId)) {
                workers.remove(i);
                break;
            }
        }
    }

    public boolean isItNewWork(int partitionIndex) {
        return !partitionResults.containsKey(partitionIndex);
    }

    private boolean isAnyWrokerRunning(Worker worker) {
        /* Check if the worker is in workers list */

        for (int i = 0; i < workers.size(); i++) {
            if (worker.getEndpointId().equals(workers.get(i).getEndpointId())) {
                return true;
            }
        }
        return false;
    }

    public void checkWorkCompletion(int workAmount) {
        Log.d("Checking", "totalpartitions :" + totalpartitions + " partitionResults.size() " + partitionResults.size());
        if (workAmount == totalpartitions) {
            sendByeToWorkers();
        } else if (partitionResults.size() == totalpartitions) {
            sendByeToWorkers();
        }
    }

    public void updateWorkStatus(Worker worker, WorkInfo workInfo) {
        if (partitionResults.size() == totalpartitions) {
            return; //finished all work :)
        }
        if (worker == null || workInfo == null) {
            return; // initialization is not done yet
        }

        if (workInfo.getStatusInfo().equals(Constants.WorkStatus.WORKING)) {
            partitionResults.put(workInfo.getPartitionIndexInfo(), workInfo.getResultInfo());
        }

        if (workInfo.getStatusInfo().equals(Constants.WorkStatus.FAILED) || workInfo.getStatusInfo().equals(Constants.WorkStatus.DISCONNECTED)) {
            addWorkToQueue(workInfo.getPartitionIndexInfo());
        }

        if (!workInfo.getStatusInfo().equals(Constants.WorkStatus.DISCONNECTED)) {
            addWorkersToQueue(worker);
        }

        if (partitionResults.size() != totalpartitions) {
            allocateWork();
        } else {
            sendByeToWorkers();
        }
    }

    private void sendByeToWorkers() {
        if (bye) {
            return;
        }

        bye = true;
        handler.removeCallbacks(runnable);

        for (Worker worker : workers) {
            if (!worker.getWorkStatus().getStatusInfo().equals(Constants.WorkStatus.DISCONNECTED)) {
                ClientPayLoad payload = new ClientPayLoad();
                payload.setTag(Constants.PayloadTags.FAREWELL);

                //TODO: Send data to device on connection. Done
                Connector.sendToDevice(context, worker.getEndpointId(), payload);
                NearbySingleton.getInstance(context).rejectConnection(worker.getEndpointId());
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTimeElapsed = endTime - beginTime;
        FlushToFile.writeTextToFile(context, "exectution_time_of_distributed_approach.txt", false, totalTimeElapsed + " ms");
        FlushToFile.writeMatrixToFile(context, "matrix1.txt", matrix1);
        FlushToFile.writeMatrixToFile(context, "matrix2.txt", matrix2);


        int[][] resultMatrix = new int[rows1][cols1];
        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < cols2; j++) {
                resultMatrix[i][j] = partitionResults.get(i * cols2 + j);
            }
        }

        FlushToFile.writeMatrixToFile(context, "result_matrix.txt", resultMatrix);
        this.workCallback.onWorkCompleted(totalTimeElapsed);
    }

    private void addWorkersToQueue(Worker worker) {
        workerQueue.add(worker);
    }

    private void sendWorkersToQueue() {
        for (Worker worker : workers) {
            workerQueue.add(worker);
        }
    }

    private void addWorkToQueue() {
        for (int i = 0; i < totalpartitions; i++) {
            if (!partitionResults.containsKey(i)) {
                workQueue.add(i);
            }
        }
    }

    private void addWorkToQueue(int partitionIndex) {
        if (!partitionResults.containsKey(partitionIndex)) {
            workQueue.add(partitionIndex);
        }
    }

    public final static class ThresholdsHolder {
        /* worker that has more battery than other workers gets more priority
           Example: worker1 = 70
                    worker2 = 29
                    worker1 gets more priority and task is assigned to worker1.
           Also, we check if the device is connected to power cable and is charging
         */
        public final static int BATTERY_LEVEL_DIFFERENCE = 20;

        /* If battery level is below this */
        public final static int MINIMUM_BATTERY_LEVEL = 5;
    }

    private class WorkerPriorityChecker implements Comparator<Worker> {
        @Override
        public int compare(Worker worker1, Worker worker2) {

            if ((worker1.getDeviceStats().getBatteryLevel() - worker2.getDeviceStats().getBatteryLevel()) > ThresholdsHolder.BATTERY_LEVEL_DIFFERENCE) {
                return worker1.getDeviceStats().getBatteryLevel() - worker2.getDeviceStats().getBatteryLevel();
            }

            if (worker1.getDeviceStats().isCharging() && worker2.getDeviceStats().isCharging()) {
                return worker1.getDeviceStats().getBatteryLevel() - worker2.getDeviceStats().getBatteryLevel();
            } else if (worker1.getDeviceStats().isCharging()) {
                return 1;
            } else {
                return -1;
            }
        }
    }


}
