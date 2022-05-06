package com.group29.mobileoffloading;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;
import com.group29.mobileoffloading.BackgroundLoopers.DeviceInfoBroadcaster;
import com.group29.mobileoffloading.Helpers.NearbySingleton;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;

public class WorkerBroadcastingActivity extends AppCompatActivity {
    public static final String MASTER_NODE_ID_BUNDLE_KEY = "MASTER_NODE_ID_BUNDLE_KEY";
    private String workerId;
    private String masterNodeId = "";
    private ClientConnectionListener connectionListener;
    private DeviceInfoBroadcaster deviceInfoBroadcaster;
    private Handler handler;
    private Runnable runnable;
    private AdvertisingOptions advertisingOptions;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_broadcasting);
        this.advertisingOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        workerId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        setDeviceId("Device ID: " + workerId);

        //Start Advertisement
        deviceInfoBroadcaster = new DeviceInfoBroadcaster(getApplicationContext(), null);

        connectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
                
                masterNodeId = id;
                showDialog(connectionInfo.getEndpointName());
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
                
            }

            @Override
            public void onDisconnected(String id) {
                
                finish();
            }
        };

        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            refreshCardData();
            handler.postDelayed(runnable, 7000);
        };
    }

    void setState(String text) {
        ((TextView) findViewById(R.id.worker_broadcasting_state_tv)).setText(text);
    }

    void setDeviceId(String text) {

    }

    void refreshCardData() {
        TextView st = findViewById(R.id.worker_broadcasting_battery_percentage_tv);
        st.setText("Level: " + DeviceInfoBroadcaster.getBatteryLevel(this) + "%");
        ((TextView) findViewById(R.id.worker_broadcasting_charging_tv)).setText(String.format("Plugged In: %s", DeviceInfoBroadcaster.isPluggedIn(this) ? "true" : "false"));
        TextView latitude_tv = findViewById(R.id.worker_broadcasting_latitude_tv);
        TextView longitude_tv = findViewById(R.id.worker_broadcasting_longitude_tv);
        if (DeviceInfoBroadcaster.getLocation(this) != null) {
            latitude_tv.setText(String.format("Latitude: %s", DeviceInfoBroadcaster.getLocation(this).getLatitude()));
            longitude_tv.setText("Longitude: " + DeviceInfoBroadcaster.getLocation(this).getLongitude());
        } else {
            latitude_tv.setText("Latitude: null");
            longitude_tv.setText("Longitude: null");
        }
    }

    void showDialog(String masterNodeID) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    NearbySingleton.getInstance(getApplicationContext()).acceptConnection(masterNodeId);
                    startWorkerComputation();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    NearbySingleton.getInstance(getApplicationContext()).rejectConnection(masterNodeId);
                    break;
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(WorkerBroadcastingActivity.this);
        builder.setMessage("Do you want to pair with master node" + masterNodeID + "?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }


    @Override
    protected void onResume() {
        setState("Initializing...");
        super.onResume();
        NearbySingleton.getInstance(getApplicationContext()).advertise(workerId, advertisingOptions).addOnSuccessListener(command -> {
            
            setState("Discoverable by all devices");
        }).addOnFailureListener(c -> {
            if (((ApiException) c).getStatusCode() == 8001) {
                
                setState("Discoverable by all devices");
            } else {
                setState("Failed to host device");
            }
        });
        NearbySingleton.getInstance(getApplicationContext()).registerClientConnectionListener(connectionListener);
        
        deviceInfoBroadcaster.begin();
        handler.postDelayed(runnable, 7000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbySingleton.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        
        deviceInfoBroadcaster.destroy();
        handler.removeCallbacks(runnable);
    }

    private void startWorkerComputation() {
        Intent intent = new Intent(getApplicationContext(), WorkerActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(MASTER_NODE_ID_BUNDLE_KEY, masterNodeId);
        intent.putExtras(bundle);
        startActivity(intent);
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
        
        finish();
    }

    @Override
    public void onBackPressed() {
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
        
        if (!masterNodeId.equals("")) {
            NearbySingleton.getInstance(getApplicationContext()).disconnectFromEndpoint(masterNodeId);
            NearbySingleton.getInstance(getApplicationContext()).rejectConnection(masterNodeId);
        }
        finish();
        super.onBackPressed();
    }
}