package com.group29.mobileoffloading;

import android.content.Context;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.group29.mobileoffloading.listeners.WorkerStatusListener;
import com.group29.mobileoffloading.DataModels.Worker;
import com.group29.mobileoffloading.CustomListAdapters.WorkingWorkersAdapter;
import com.group29.mobileoffloading.BackgroundLoopers.DeviceInfoBroadcaster;
import com.group29.mobileoffloading.Helpers.NearbySingleton;
import com.group29.mobileoffloading.Helpers.WorkAllocator;
import com.group29.mobileoffloading.BackgroundLoopers.WorkerListener;
import com.group29.mobileoffloading.DataModels.AvailableWorker;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.DataModels.WorkInfo;
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.FlushToFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import needle.Needle;

public class Master extends AppCompatActivity {

    private RecyclerView rvWorkers;

    private final HashMap<String, WorkerListener> workerListenersMap = new HashMap<>();

    private final ArrayList<com.group29.mobileoffloading.DataModels.Worker> workers = new ArrayList<>();
    private WorkingWorkersAdapter workingWorkersAdapter;
    
    private final int rows1 = 40;
    private final int cols1 = 40;
    private final int rows2 = 40;
    private final int cols2 = 40;

    private int[][] matrix1;
    private int[][] matrix2;

    private WorkAllocator workAllocator;

    private int workAmount;
    private Handler handler;
    private Runnable runnable;
    private DeviceInfoBroadcaster deviceInfoBroadcaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        Log.d("MasterDiscovery", "Starting computing matrix multiplication on only master");
        TextView power_consumed_master_tv = findViewById(R.id.power_consumed_master_tv);
        power_consumed_master_tv.setText("Power Consumption for Master Node: null");
        BatteryManager mBatteryManager =
                (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        Long initialEnergyMaster =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        computeMatrixMultiplicationOnMaster();
        Long finalEnergyMaster =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        Long energyConsumedMaster = Math.abs(initialEnergyMaster-finalEnergyMaster);
        power_consumed_master_tv.setText("Power Consumption for Master Node: " +Long.toString(energyConsumedMaster)+ " nWh");
        Log.d("MasterDiscovery", "Completed computing matrix multiplication on only master");

        unpackBundle();
        bindViews();
        setAdapters();
        init();
        setupDeviceBatteryStatsCollector();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopWorkerStatusSubscribers();
        deviceInfoBroadcaster.stop();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startWorkerStatusSubscribers();
        deviceInfoBroadcaster.start();
        handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
    }

    @Override
    public void onBackPressed() {
        for (com.group29.mobileoffloading.DataModels.Worker w : workers) {
            updateWorkerConnectionStatus(w.getEndpointId(), Constants.WorkStatus.DISCONNECTED);
            workAllocator.removeWorker(w.getEndpointId());
            NearbySingleton.getInstance(getApplicationContext()).disconnectFromEndpoint(w.getEndpointId());
        }
        super.onBackPressed();
        finish();
    }

    private void init() {
        int totalPartitions = rows1 * cols2;
        matrix1 = generateMatrix(rows1, cols1);
        matrix2 = generateMatrix(rows2, cols2);
        TextView totalPart = findViewById(R.id.work_array_partitions_tv);
        totalPart.setText("Number of Work Partitions:" + totalPartitions);

        workAllocator = new WorkAllocator(getApplicationContext(), workers, matrix1, matrix2, workerExecutionTime -> {
            ((TextView)findViewById(R.id.worker_execution_time_tv)).setText("Execution time for Worker Nodes: " + workerExecutionTime + "ms");
        });
        workAllocator.beginDistributedComputation();
    }

    public int[][] generateMatrix(int num_rows, int num_cols){
        int [][] matrix = new int[num_rows][num_cols];
        Random rand = new Random();

        for(int i = 0; i < num_rows; i++){
            for(int j = 0 ; j < num_cols; j++){
                matrix[i][j] = rand.nextInt(200);
            }
        }
        return matrix;
    }


    private void bindViews() {
        rvWorkers = findViewById(R.id.workers_recycle_view);
        SimpleItemAnimator itemAnimator = (SimpleItemAnimator) rvWorkers.getItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
    }


    private void setAdapters() {
        workingWorkersAdapter = new WorkingWorkersAdapter(this, workers);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvWorkers.setLayoutManager(linearLayoutManager);

        rvWorkers.setAdapter(workingWorkersAdapter);
        workingWorkersAdapter.notifyDataSetChanged();
    }


    private void unpackBundle() {
        try {
            Bundle bundle = getIntent().getExtras();

            ArrayList<AvailableWorker> availableWorkers = (ArrayList<AvailableWorker>) bundle.getSerializable(Constants.CONNECTED_DEVICES);
            addToWorkers(availableWorkers);
            Log.d("CHECK", "Added a connected Device as worker");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void addToWorkers(ArrayList<AvailableWorker> availableWorkers) {
        for (AvailableWorker availableWorker : availableWorkers) {
            com.group29.mobileoffloading.DataModels.Worker worker = new com.group29.mobileoffloading.DataModels.Worker();
            worker.setEndpointId(availableWorker.getEndpointId());
            worker.setEndpointName(availableWorker.getEndpointName());

            WorkInfo workStatus = new WorkInfo();
            workStatus.setStatusInfo(Constants.WorkStatus.WORKING);

            worker.setWorkStatus(workStatus);
            worker.setDeviceStats(new DeviceInfo(0,false,0.0,0.0));

            workers.add(worker);
        }
    }

    private void computeMatrixMultiplicationOnMaster() {
        matrix1 = generateMatrix(rows1, cols1);
        matrix2 = generateMatrix(rows2, cols2);
        Needle.onBackgroundThread().execute(() -> {
            long startTime = System.currentTimeMillis();
            int[][] mul = new int[rows1][cols2];
            for (int i = 0; i < rows1; i++) {
                for (int j = 0; j < cols2; j++) {
                    mul[i][j] = 0;
                    for (int k = 0; k < cols1; k++) {
                        mul[i][j] += matrix1[i][k] * matrix2[k][j];
                    }
                }
            }
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            FlushToFile.writeTextToFile(getApplicationContext(), "exec_time_master_alone.txt", false, totalTime + "ms");
            TextView master = findViewById(R.id.master_execution_time_tv);
            master.setText("Execution time for Master Node: " + totalTime + "ms");
        });
    }



    private void setupDeviceBatteryStatsCollector() {
        deviceInfoBroadcaster = new DeviceInfoBroadcaster(getApplicationContext(), null, Constants.UPDATE_INTERVAL_UI);
        handler = new Handler();
        runnable = () -> {
            String deviceStatsStr = DeviceInfoBroadcaster.getBatteryLevel(this) + "%"
                    + "\t" + (DeviceInfoBroadcaster.isPluggedIn(this) ? "CHARGING" : "NOT CHARGING");
            FlushToFile.writeTextToFile(getApplicationContext(), "master_battery.txt", true, deviceStatsStr);
            handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
        };
    }


    private void updateWorkerConnectionStatus(String nodeIdString, String status) {
        Log.d("DISCONNECTED----", nodeIdString);
        for (int i = 0; i < workers.size(); i++) {

            Log.d("DISCONNECTED--", workers.get(i).getEndpointId());
            if (workers.get(i).getEndpointId().equals(nodeIdString)) {
                workers.get(i).getWorkStatus().setStatusInfo(status);
                workingWorkersAdapter.notifyDataSetChanged();
                break;
            }
        }
    }


    private void startWorkerStatusSubscribers() {
        for (com.group29.mobileoffloading.DataModels.Worker worker : workers) {
            if (workerListenersMap.containsKey(worker.getEndpointId())) {
                continue;
            }

            WorkerListener workerListener = new WorkerListener(getApplicationContext(), worker.getEndpointId(), new WorkerStatusListener() {
                @Override
                public void onWorkStatusReceived(String nodeIdString, WorkInfo workStatus) {

                    if (workStatus.getStatusInfo().equals(Constants.WorkStatus.DISCONNECTED)) {
                        updateWorkerConnectionStatus(nodeIdString, Constants.WorkStatus.DISCONNECTED);
                        workAllocator.removeWorker(nodeIdString);
                        NearbySingleton.getInstance(getApplicationContext()).rejectConnection(nodeIdString);
                    } else {
                        updateWorkerStatus(nodeIdString, workStatus);
                    }
                    workAllocator.checkWorkCompletion(getWorkAmount());
                }

                @Override
                public void onDeviceStatsReceived(String nodeIdString, DeviceInfo deviceStats) {
                    updateWorkerStatus(nodeIdString, deviceStats);

                    String deviceStatsStr = deviceStats.getBatteryPercentage() + "%"
                            + "\t" + (deviceStats.isCharging() ? "CHARGING" : "NOT CHARGING")
                            + "\t\t" + deviceStats.getLatitude()
                            + "\t" + deviceStats.getLongitude();
                    FlushToFile.writeTextToFile(getApplicationContext(), nodeIdString + ".txt", true, deviceStatsStr);
                    Log.d("MASTER_ACTIVITY", "WORK AMOUNT: " + getWorkAmount());
                    workAllocator.checkWorkCompletion(getWorkAmount());
                }
            });

            workerListener.start();
            workerListenersMap.put(worker.getEndpointId(), workerListener);
        }
    }


    private int getWorkAmount() {
        int sum = 0;
        for (Worker worker : workers) {
            sum += worker.getWorkAmount();

        }
        return sum;
    }

    private void updateWorkerStatus(String nodeIdString, WorkInfo workStatus) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(nodeIdString)) {
                worker.setWorkStatus(workStatus);

                if (workStatus.getStatusInfo().equals(Constants.WorkStatus.WORKING) && workAllocator.isItNewWork(workStatus.getPartitionIndexInfo())) {
                    workers.get(i).setWorkAmount(workers.get(i).getWorkAmount() + 1);
                    workAmount += 1;
                }

                workAllocator.updateWorkStatus(worker, workStatus);

                workingWorkersAdapter.notifyItemChanged(i);
                break;
            }
        }

    }

    private void updateWorkerStatus(String nodeIdString, DeviceInfo deviceStats) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(nodeIdString)) {
                worker.setDeviceStats(deviceStats);
                Location masterLocation = DeviceInfoBroadcaster.getLocation(this);
                if ((deviceStats.getLatitude() == 0.0 || deviceStats.getLongitude() != 0.0) && masterLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(masterLocation.getLatitude(), masterLocation.getLongitude(),
                            deviceStats.getLatitude(), deviceStats.getLongitude(), results);
                    Log.d("MASTER_ACTIVITY", "Master Location: " + masterLocation.getLatitude() + ", " + masterLocation.getLongitude());
                    Log.d("MASTER_ACTIVITY", "Master Distance: " + results[0]);
                    worker.setDistanceFromMaster(results[0]);
                }

                workingWorkersAdapter.notifyItemChanged(i);
            }
        }
    }

    private void stopWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            WorkerListener workerListener = workerListenersMap.get(worker.getEndpointId());
            if (workerListener != null) {
                workerListener.stop();
                workerListenersMap.remove(worker.getEndpointId());
            }
        }
    }


}