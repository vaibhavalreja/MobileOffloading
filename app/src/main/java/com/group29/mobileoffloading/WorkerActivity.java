package com.group29.mobileoffloading;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.BackgroundLoopers.DeviceInfoBroadcaster;
import com.group29.mobileoffloading.DataModels.NodeDataPayload;
import com.group29.mobileoffloading.DataModels.WorkData;
import com.group29.mobileoffloading.DataModels.WorkDataforWorker;
import com.group29.mobileoffloading.Helpers.NearbySingleton;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.listeners.NodeDataListener;
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
    private NodeDataListener payloadCallback;
    private int currentPartitionIndex;
    private HashSet<Integer> finishedWork = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);
        findViewById(R.id.worker_stop_working_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WorkDataforWorker workStatus = new WorkDataforWorker();
                workStatus.setPartitionIndexInfo(currentPartitionIndex);
                workStatus.setStatusInfo(WorkerStateVariables.DISCONNECTED);

                NodeDataPayload tPayload1 = new NodeDataPayload();
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
        payloadCallback = new NodeDataListener() {
            @Override
            public void onDataReceived(@NonNull String nodeIdString, @NonNull Payload payload) {
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
        deviceStatsPublisher.begin();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbySingleton.getInstance(getApplicationContext()).unregisterPayloadListener(payloadCallback);
        NearbySingleton.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        deviceStatsPublisher.destroy();
    }

    @Override
    public void finish() {
        super.finish();
        NearbySingleton.getInstance(getApplicationContext()).disconnectFromEndpoint(masterId);
        currentPartitionIndex = 0;
    }

    public void startWorking(Payload payload) {
        WorkDataforWorker workStatus = new WorkDataforWorker();
        NodeDataPayload sendPayload = new NodeDataPayload();
        sendPayload.setTag(DataPacketStringKeys.WORK_STATUS);

        try {
            NodeDataPayload receivedPayload = PayloadConverter.fromPayload(payload);
            if (receivedPayload.getTag().equals(DataPacketStringKeys.WORK_DATA)) {
                setStatusText("Work status: Working");

                WorkData workData = (WorkData) receivedPayload.getData();
                int dotProduct = calculateDotProduct(workData.getRows(), workData.getCols());

                
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
                
                workStatus.setStatusInfo(WorkerStateVariables.FINISHED);
                sendPayload.setData(workStatus);
                DataTransfer.sendPayload(getApplicationContext(), masterId, sendPayload);
                deviceStatsPublisher.destroy();

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

