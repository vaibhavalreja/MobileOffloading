package com.group29.mobileoffloading;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.group29.mobileoffloading.BackgroundLoopers.DataInterface;
import com.group29.mobileoffloading.CustomListAdapters.AvailableWorkersAdapter;
import com.group29.mobileoffloading.DataModels.AvailableWorker;
import com.group29.mobileoffloading.DataModels.NodeDataPayload;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.Helpers.NearbySingleton;
import com.group29.mobileoffloading.Helpers.WorkAllocator;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.listeners.NodeDataListener;
import com.group29.mobileoffloading.utilities.DataPacketStringKeys;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;
import java.util.ArrayList;

public class MasterSearchActivity extends AppCompatActivity {

    // STATE VARIABLES
    private final String REQUEST_STATE_ATTEMPTING = "REQUEST_STATE_ATTEMPTING";
    private final String REQUEST_STATE_ACCEPTED = "REQUEST_STATE_ACCEPTED";
    private final String REQUEST_STATE_REJECTED = "REQUEST_STATE_REJECTED";
    private final ArrayList<AvailableWorker> availableWorkerDevices = new ArrayList<>();
    private RecyclerView available_workers_rv;
    private AvailableWorkersAdapter availableWorkersAdapter;
    private ClientConnectionListener clientConnectionListener;
    private NodeDataListener nodeDataListener;
    private DiscoveryOptions discoveryOptions;
    private String masterNodeId;

    @Override
    protected void onPause() {
        super.onPause();
        setState("Not searching");
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
        NearbySingleton.getInstance(getApplicationContext()).unregisterPayloadListener(nodeDataListener);
        NearbySingleton.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMasterDiscovery();
        NearbySingleton.getInstance(getApplicationContext()).registerPayloadListener(nodeDataListener);
        NearbySingleton.getInstance(getApplicationContext()).registerClientConnectionListener(clientConnectionListener);
    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_search);
        masterNodeId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        this.discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();

        findViewById(R.id.master_distribute_task_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<AvailableWorker> readyDevices = getDevicesInReadyState();
                if (readyDevices.size() == 0) {
                    
                    Toast.makeText(getApplicationContext(), "No worker Available at the moment", Toast.LENGTH_LONG).show();
                    onBackPressed();
                } else {
                    
                    Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
                    startMasterActivity(readyDevices);
                    finish();
                }
            }
        });
        available_workers_rv = findViewById(R.id.rv_connected_devices);
        availableWorkersAdapter = new AvailableWorkersAdapter(this, availableWorkerDevices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        available_workers_rv.setLayoutManager(linearLayoutManager);

        available_workers_rv.setAdapter(availableWorkersAdapter);
        availableWorkersAdapter.notifyDataSetChanged();

        nodeDataListener = new NodeDataListener() {
            @Override
            public void onDataReceived(String nodeIdString, Payload payload) {
                
                try {
                    NodeDataPayload tPayload = PayloadConverter.fromPayload(payload);
                    if (tPayload.getTag().equals(DataPacketStringKeys.DEVICE_STATS)) {
                        updateDeviceStats(nodeIdString, (DeviceInfo) tPayload.getData());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };


        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String nodeIdString, ConnectionInfo connectionInfo) {
                
                NearbySingleton.getInstance(getApplicationContext()).acceptConnection(nodeIdString);
            }

            @Override
            public void onConnectionResult(String nodeIdString, ConnectionResolution connectionResolution) {

                

                int statusCode = connectionResolution.getStatus().getStatusCode();
                if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    
                    updateConnectedDeviceRequestStatus(nodeIdString, REQUEST_STATE_ACCEPTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                    
                    updateConnectedDeviceRequestStatus(nodeIdString, REQUEST_STATE_REJECTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_ERROR) {
                    
                    removeConnectedDevice(nodeIdString, true);
                }
            }

            @Override
            public void onDisconnected(String nodeIdString) {
                
                removeConnectedDevice(nodeIdString, true);
            }
        };


    }

    private ArrayList<AvailableWorker> getDevicesInReadyState() {
        ArrayList<AvailableWorker> res = new ArrayList<>();
        for (int i = 0; i < availableWorkerDevices.size(); i++) {
            if (availableWorkerDevices.get(i).getRequestStatus().equals(REQUEST_STATE_ACCEPTED)) {
                if (availableWorkerDevices.get(i).getDeviceStats().getBatteryPercentage() > WorkAllocator.ThresholdsHolder.MINIMUM_BATTERY_LEVEL) {
                    res.add(availableWorkerDevices.get(i));
                } else {
                    NodeDataPayload tPayload = new NodeDataPayload();
                    tPayload.setTag(DataPacketStringKeys.DISCONNECTED);

                    DataInterface.sendToDevice(getApplicationContext(), availableWorkerDevices.get(i).getEndpointId(), tPayload);
                }
            } else {
                
                NodeDataPayload tPayload = new NodeDataPayload();
                tPayload.setTag(DataPacketStringKeys.DISCONNECTED);

                DataInterface.sendToDevice(getApplicationContext(), availableWorkerDevices.get(i).getEndpointId(), tPayload);
            }

        }
        return res;
    }

    private void updateConnectedDeviceRequestStatus(String nodeIdString, String status) {
        for (int i = 0; i < availableWorkerDevices.size(); i++) {
            if (availableWorkerDevices.get(i).getEndpointId().equals(nodeIdString)) {
                availableWorkerDevices.get(i).setRequestStatus(status);
                
                availableWorkersAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void startMasterDiscovery() {
        
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String nodeIdString, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                
                
                

                AvailableWorker availableWorker = new AvailableWorker();
                availableWorker.setEndpointId(nodeIdString);
                availableWorker.setEndpointName(discoveredEndpointInfo.getEndpointName());
                availableWorker.setRequestStatus(REQUEST_STATE_ATTEMPTING);
                availableWorker.setDeviceStats(new DeviceInfo(0, false, 0.0, 0.0));

                availableWorkerDevices.add(availableWorker);
                availableWorkersAdapter.notifyItemChanged(availableWorkerDevices.size() - 1);

                

                NearbySingleton.getInstance(getApplicationContext()).requestConnection(nodeIdString, masterNodeId);
                

            }

            @Override
            public void onEndpointLost(@NonNull String nodeIdString) {
                
                
                removeConnectedDevice(nodeIdString, false);
            }
        };

        Nearby.getConnectionsClient(getApplicationContext())
                .startDiscovery(getPackageName(), endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener((unused) -> {
                    
                })
                .addOnFailureListener((Exception e) -> {
                    
                    e.printStackTrace();
                })
                .addOnSuccessListener((unused) -> {
                    setState("Finding");
                })
                .addOnFailureListener(command -> {
                    if (((ApiException) command).getStatusCode() == 8002) {
                        setState("Finding");
                    } else {
                        setState("Error - try to enable location");
                        
                        finish();
                    }
                    command.printStackTrace();
                });
    }

    private void removeConnectedDevice(String nodeIdString, boolean forceRemove) {

        for (int i = 0; i < availableWorkerDevices.size(); i++) {
            boolean checkStatus = forceRemove || !availableWorkerDevices.get(i).getRequestStatus().equals(REQUEST_STATE_ACCEPTED);
            if (availableWorkerDevices.get(i).getEndpointId().equals(nodeIdString) && checkStatus) {
                
                availableWorkerDevices.remove(i);
                availableWorkersAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void updateDeviceStats(String nodeIdString, DeviceInfo deviceStats) {
        canAssign(deviceStats);
        for (int i = 0; i < availableWorkerDevices.size(); i++) {
            if (availableWorkerDevices.get(i).getEndpointId().equals(nodeIdString)) {
                availableWorkerDevices.get(i).setDeviceStats(deviceStats);

                availableWorkerDevices.get(i).setRequestStatus(REQUEST_STATE_ACCEPTED);
                availableWorkersAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    void canAssign(DeviceInfo deviceStats) {
        Button distributeWorkButton = findViewById(R.id.master_distribute_task_button);
        distributeWorkButton.setEnabled(deviceStats.getBatteryPercentage() > WorkAllocator.ThresholdsHolder.MINIMUM_BATTERY_LEVEL);
    }

    void setState(String text) {
        ((TextView) findViewById(R.id.master_search_tv)).setText(text);
    }

    @Override
    public void finish() {
        super.finish();
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void startMasterActivity(ArrayList<AvailableWorker> availableWorkerDevices) {
        Intent intent = new Intent(getApplicationContext(), Master.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Master.AVAILABLE_DEVICES_LIST_BUNDLE_KEY, availableWorkerDevices);
        intent.putExtras(bundle);
        
        startActivity(intent);
        finish();
    }

}