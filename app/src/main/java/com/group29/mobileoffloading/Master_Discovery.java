package com.group29.mobileoffloading;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.group29.mobileoffloading.CustomListAdapters.ConnectedDevicesAdapter;
import com.group29.mobileoffloading.backgroundservices.Connector;
import com.group29.mobileoffloading.backgroundservices.NearbySingleton;
import com.group29.mobileoffloading.backgroundservices.WorkAllocator;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.listeners.PayloadListener;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.DataModels.ConnectedDevice;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Master_Discovery extends AppCompatActivity {

    private RecyclerView rvConnectedDevices;
    private ConnectedDevicesAdapter connectedDevicesAdapter;
    private List<ConnectedDevice> connectedDevices = new ArrayList<>();
    private ClientConnectionListener clientConnectionListener;
    private PayloadListener payloadListener;
    private DiscoveryOptions discoveryOptions;
    private String masterNodeId;

    @Override
    protected void onPause() {
        super.onPause();
        setState("Not searching");
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
        NearbySingleton.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
        NearbySingleton.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMasterDiscovery();
        NearbySingleton.getInstance(getApplicationContext()).registerPayloadListener(payloadListener);
        NearbySingleton.getInstance(getApplicationContext()).registerClientConnectionListener(clientConnectionListener);
    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_discovery);
        masterNodeId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        this.discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();

        rvConnectedDevices = findViewById(R.id.rv_connected_devices);
        connectedDevicesAdapter = new ConnectedDevicesAdapter(this, connectedDevices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvConnectedDevices.setLayoutManager(linearLayoutManager);

        rvConnectedDevices.setAdapter(connectedDevicesAdapter);
        connectedDevicesAdapter.notifyDataSetChanged();

        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                Log.d("MASTER_DISCOVERY", "PayloadListener -  onPayloadReceived");
                try {
                    ClientPayLoad tPayload = PayloadConverter.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.DEVICE_STATS)) {
                        updateDeviceStats(endpointId, (DeviceInfo) tPayload.getData());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("MASTER_DISCOVERY", "PayloadListener -  onPayloadTransferUpdate");
            }
        };


        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionInitiated");
                NearbySingleton.getInstance(getApplicationContext()).acceptConnection(endpointId);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult" + endpointId);

                int statusCode = connectionResolution.getStatus().getStatusCode();
                if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - ACCEPTED");
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.ACCEPTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - REJECTED");
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.REJECTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_ERROR) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - ERROR");
                    removeConnectedDevice(endpointId, true);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onDisconnected " + endpointId);
                removeConnectedDevice(endpointId, true);
            }
        };


    }


    public void assignTasks(View view) {
        ArrayList<ConnectedDevice> readyDevices = getDevicesInReadyState();
        if (readyDevices.size() == 0) {
            Log.d("TEST","no devices");
            Toast.makeText(getApplicationContext(), "No worker Available at the moment", Toast.LENGTH_LONG).show();
            onBackPressed();
        } else {
            Log.d("TEST","herer");
            Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
            startMasterActivity(readyDevices);
            finish();
        }
    }

    private ArrayList<ConnectedDevice> getDevicesInReadyState() {
        ArrayList<ConnectedDevice> res = new ArrayList<>();
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getRequestStatus().equals(Constants.RequestStatus.ACCEPTED)) {
                if (connectedDevices.get(i).getDeviceStats().getBatteryLevel() > WorkAllocator.ThresholdsHolder.MINIMUM_BATTERY_LEVEL) {
                    res.add(connectedDevices.get(i));
                } else {
                    ClientPayLoad tPayload = new ClientPayLoad();
                    tPayload.setTag(Constants.PayloadTags.DISCONNECTED);

                    Connector.sendToDevice(getApplicationContext(), connectedDevices.get(i).getEndpointId(), tPayload);
                }
            } else {
                Log.d("MASTER_DISCOVERY", "LOOPING");
                ClientPayLoad tPayload = new ClientPayLoad();
                tPayload.setTag(Constants.PayloadTags.DISCONNECTED);

                Connector.sendToDevice(getApplicationContext(), connectedDevices.get(i).getEndpointId(), tPayload);
            }

        }
        return res;
    }

    private void updateConnectedDeviceRequestStatus(String endpointId, String status) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.get(i).setRequestStatus(status);
                Log.d("MASTER_DISCOVERY", "Status of end point set to "+status);
                connectedDevicesAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void startMasterDiscovery() {
        Log.d("MASTER_DISCOVERY", "Starting Master Discovery");
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("MASTER_DISCOVERY", "ENDPOINT FOUND " +endpointId);
                Log.d("MASTER_DISCOVERY", endpointId);
                Log.d("MASTER_DISCOVERY", discoveredEndpointInfo.getServiceId() + " " + discoveredEndpointInfo.getEndpointName());

                ConnectedDevice connectedDevice = new ConnectedDevice();
                connectedDevice.setEndpointId(endpointId);
                connectedDevice.setEndpointName(discoveredEndpointInfo.getEndpointName());
                connectedDevice.setRequestStatus(Constants.RequestStatus.PENDING);
                connectedDevice.setDeviceStats(new DeviceInfo());

                connectedDevices.add(connectedDevice);
                connectedDevicesAdapter.notifyItemChanged(connectedDevices.size() - 1);

                Log.d("MASTER_DISCOVERY", "Added end point to connected devices : " +endpointId);

                NearbySingleton.getInstance(getApplicationContext()).requestConnection(endpointId, masterNodeId);
                Log.d("MASTER_DISCOVERY", "Requested connection for : " +endpointId);

            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                Log.d("MASTER_DISCOVERY", "ENDPOINT LOST");
                Log.d("MASTER_DISCOVERY", endpointId);
                removeConnectedDevice(endpointId, false);
            }
        };

        Nearby.getConnectionsClient(getApplicationContext())
                .startDiscovery(getPackageName(), endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener((unused) -> {
                    Log.d("MASTER", "DISCOVERY IN PROGRESS");
                })
                .addOnFailureListener((Exception e) -> {
                    Log.d("MASTER", "DISCOVERING FAILED");
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
                        Log.d("TEST", "discovery failed");
                        finish();
                    }
                    command.printStackTrace();
                });
    }

    private void removeConnectedDevice(String endpointId, boolean forceRemove) {

        for (int i = 0; i < connectedDevices.size(); i++) {
            boolean checkStatus = forceRemove ? true :  !connectedDevices.get(i).getRequestStatus().equals(Constants.RequestStatus.ACCEPTED);
            if (connectedDevices.get(i).getEndpointId().equals(endpointId) && checkStatus) {
                Log.d("MASTER_DISCOVERY", "Removed end point from connected devices " + endpointId );
                connectedDevices.remove(i);
                connectedDevicesAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void updateDeviceStats(String endpointId, DeviceInfo deviceStats) {
        canAssign(deviceStats);
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.get(i).setDeviceStats(deviceStats);

                connectedDevices.get(i).setRequestStatus(Constants.RequestStatus.ACCEPTED);
                connectedDevicesAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    void canAssign(DeviceInfo deviceStats) {
        Button assignButton = findViewById(R.id.assignTask);
        assignButton.setEnabled(deviceStats.getBatteryLevel() > WorkAllocator.ThresholdsHolder.MINIMUM_BATTERY_LEVEL);
    }

    void setState(String text) {
        ((TextView) findViewById(R.id.discovery)).setText(text);
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

    private void startMasterActivity(ArrayList<ConnectedDevice> connectedDevices) {
        Intent intent = new Intent(getApplicationContext(), Master.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.CONNECTED_DEVICES, connectedDevices);
        intent.putExtras(bundle);
        Log.d("TEST","STARTM");
        startActivity(intent);
        finish();
    }

}