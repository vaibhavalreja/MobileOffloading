package com.group29.mobileoffloading;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.BackgroundLoopers.DeviceInfoBroadcaster;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.DataModels.WorkData;
import com.group29.mobileoffloading.DataModels.WorkInfo;
import com.group29.mobileoffloading.Helpers.NearbySingleton;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.listeners.PayloadListener;
import com.group29.mobileoffloading.stateVariables.WorkerStateVariables;
import com.group29.mobileoffloading.utilities.DataPacketStringKeys;
import com.group29.mobileoffloading.utilities.DataTransfer;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;
import java.util.HashSet;

public class WorkerActivity extends AppCompatActivity {
    BatteryManager mBatteryManager = null;
    Long initialEnergyWorker, finalEnergyWorker, energyConsumedWorker;
    private String masterId;
    private DeviceInfoBroadcaster deviceStatsPublisher;
    private ClientConnectionListener connectionListener;
    private PayloadListener payloadCallback;
    private int currentPartitionIndex;
    private HashSet<Integer> finishedWork = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);
        findViewById(R.id.worker_stop_working_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WorkInfo workStatus = new WorkInfo();
                workStatus.setPartitionIndexInfo(currentPartitionIndex);
                workStatus.setStatusInfo(WorkerStateVariables.DISCONNECTED);

                ClientPayLoad tPayload1 = new ClientPayLoad();
                tPayload1.setTag(DataPacketStringKeys.WORK_STATUS);
                tPayload1.setData(workStatus);

                DataTransfer.sendPayload(getApplicationContext(), masterId, tPayload1);
                navBack();
            }
        });
        extractBundle();
        startDeviceStatsPublisher();
        setConnectionCallback();
        connectToMaster();

        mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        initialEnergyWorker =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        Log.d("WORKER_COMPUTATION", "Capturing power consumption");
    }

    public void setStatusText(String text) {
        ((TextView) findViewById(R.id.worker_state_tv)).setText(text);
    }

    public void onWorkFinished(String text) {
        ((TextView) findViewById(R.id.worker_state_tv)).setText(text);
        ((TextView) findViewById(R.id.worker_power_consumed_tv)).setText("Power Consumed : " + energyConsumedWorker + " nWh");
    }

    private void extractBundle() {
        Bundle bundle = getIntent().getExtras();
        this.masterId = bundle.getString(WorkerBroadcastingActivity.MASTER_NODE_ID_BUNDLE_KEY);
    }

    private void startDeviceStatsPublisher() {
        deviceStatsPublisher = new DeviceInfoBroadcaster(getApplicationContext(), masterId);
    }

    private void connectToMaster() {
        payloadCallback = new PayloadListener() {
            @Override
            public void onPayloadReceived(@NonNull String nodeIdString, @NonNull Payload payload) {
                startWorking(payload);
            }
        };
        NearbySingleton.getInstance(getApplicationContext()).acceptConnection(masterId);
    }

    private void setConnectionCallback() {
        connectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
            }

            @Override
            public void onDisconnected(String id) {
                navBack();
            }
        };
    }

    private void navBack() {
        finishedWork = new HashSet<>();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NearbySingleton.getInstance(getApplicationContext()).registerPayloadListener(payloadCallback);
        NearbySingleton.getInstance(getApplicationContext()).registerClientConnectionListener(connectionListener);
        deviceStatsPublisher.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbySingleton.getInstance(getApplicationContext()).unregisterPayloadListener(payloadCallback);
        NearbySingleton.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        deviceStatsPublisher.stop();
    }

    @Override
    public void finish() {
        super.finish();
        NearbySingleton.getInstance(getApplicationContext()).disconnectFromEndpoint(masterId);
        currentPartitionIndex = 0;
    }

    public void startWorking(Payload payload) {
        WorkInfo workStatus = new WorkInfo();
        ClientPayLoad sendPayload = new ClientPayLoad();
        sendPayload.setTag(DataPacketStringKeys.WORK_STATUS);

        try {
            ClientPayLoad receivedPayload = PayloadConverter.fromPayload(payload);
            if (receivedPayload.getTag().equals(DataPacketStringKeys.WORK_DATA)) {
                setStatusText("Work status: Working");

                WorkData workData = (WorkData) receivedPayload.getData();
                int dotProduct = calculateDotProduct(workData.getRows(), workData.getCols());

                Log.d("WORKER_COMPUTATION", "Partition Index: " + workData.getPartitionIndex());
                finishedWork.add(workData.getPartitionIndex());
                currentPartitionIndex = workData.getPartitionIndex();

                workStatus.setPartitionIndexInfo(workData.getPartitionIndex());
                workStatus.setResultInfo(dotProduct);

                workStatus.setStatusInfo(WorkerStateVariables.WORKING);
                sendPayload.setData(workStatus);
                DataTransfer.sendPayload(getApplicationContext(), masterId, sendPayload);

            } else if (receivedPayload.getTag().equals(DataPacketStringKeys.FAREWELL)) {
                // end measuring energy level
                finalEnergyWorker =
                        mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
                energyConsumedWorker = Math.abs(initialEnergyWorker - finalEnergyWorker);
                onWorkFinished("Work Done !!");
                Log.d("WORKER_COMPUTATION", "Work Done");
                workStatus.setStatusInfo(WorkerStateVariables.FINISHED);
                sendPayload.setData(workStatus);
                DataTransfer.sendPayload(getApplicationContext(), masterId, sendPayload);
                deviceStatsPublisher.stop();

            } else if (receivedPayload.getTag().equals(DataPacketStringKeys.DISCONNECTED)) {
                navBack();
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int calculateDotProduct(int[] a, int[] b) {
        int product = 0;
        for (int i = 0; i < a.length; i++) {
            product += (a[i] * b[i]);
        }
        return product;
    }

}

