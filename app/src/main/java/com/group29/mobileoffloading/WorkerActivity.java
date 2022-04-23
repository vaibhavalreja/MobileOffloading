package com.group29.mobileoffloading;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;
import com.group29.mobileoffloading.backgroundservices.DeviceInfoBroadcaster;
import com.group29.mobileoffloading.backgroundservices.NearbySingleton;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.utilities.Constants;

public class WorkerActivity extends AppCompatActivity {
    private String workerId;
    private String masterId = "";
    private ClientConnectionListener connectionListener;
    private DeviceInfoBroadcaster deviceInfoBroadcaster;
    private Handler handler;
    private Runnable runnable;
    private AdvertisingOptions advertisingOptions;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);
        this.advertisingOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        workerId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        setDeviceId("Device ID: " + workerId);

        //Start Advertisement
        deviceInfoBroadcaster = new DeviceInfoBroadcaster(getApplicationContext(), null, Constants.UPDATE_INTERVAL_UI);

        connectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
                Log.d("WORKER", "Connection Received: " + id + " Endpoint name: " + connectionInfo.getEndpointName());
                masterId = id;
                showDialog(connectionInfo.getEndpointName());
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
                Log.d("WORKER", "Connection Accepted By: " + id + " " + connectionResolution.getStatus());
            }

            @Override
            public void onDisconnected(String id) {
                Log.d("WORKER", "Connection Disconnected: " + id);
                finish();
            }
        };

        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            refreshCardData();
            handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
        };
    }

    void setState(String text) {
        ((TextView) findViewById(R.id.statusText)).setText(text);
    }

    void setDeviceId(String text) {

    }

    void refreshCardData() {
        TextView st = findViewById(R.id.percentage);
        st.setText("Level: " + DeviceInfoBroadcaster.getBatteryLevel(this) + "%");
        TextView st2 = findViewById(R.id.plugged);
        st2.setText(String.format("Plugged In: %s", DeviceInfoBroadcaster.isPluggedIn(this) ? "Plugged In" : "Not Charging"));
        if (DeviceInfoBroadcaster.getLocation(this) != null) {
            TextView la = findViewById(R.id.latitude);
            la.setText(String.format("Latitude: %s", DeviceInfoBroadcaster.getLocation(this).getLatitude()));
            TextView lo = findViewById(R.id.longitude);
            lo.setText("Longitude: " + DeviceInfoBroadcaster.getLocation(this).getLongitude());
        } else {
            TextView la = findViewById(R.id.latitude);
            la.setText("Latitude: Not Available");
            TextView lo = findViewById(R.id.longitude);
            lo.setText("Longitude: Not Available");
        }
    }

    void showDialog(String masterNodeID) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    NearbySingleton.getInstance(getApplicationContext()).acceptConnection(masterId);
                    startWorkerComputation();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    NearbySingleton.getInstance(getApplicationContext()).rejectConnection(masterId);
                    break;
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(WorkerActivity.this);
        builder.setMessage("Do you want to pair with master node" + masterNodeID +"?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }



    @Override
    protected void onResume() {
        setState("Initializing...");
        super.onResume();
        NearbySingleton.getInstance(getApplicationContext()).advertise(workerId, advertisingOptions).addOnSuccessListener(command -> {
            Log.d("WORKER", "Discoverable by all devices");
            setState("Discoverable by all devices");
        }).addOnFailureListener(c -> {
            if (((ApiException) c).getStatusCode() == 8001) {
                Log.d("WORKER", "Discoverable by all devices");
                setState("Discoverable by all devices");
            } else {
                setState("Failed to host device");
            }
        });
        NearbySingleton.getInstance(getApplicationContext()).registerClientConnectionListener(connectionListener);
        Log.d("WORKER", "Starting Device Stats");
        deviceInfoBroadcaster.start();
        handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbySingleton.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        Log.d("WORKER", "Stopping Device Stats");
        deviceInfoBroadcaster.stop();
        handler.removeCallbacks(runnable);
    }

    private void startWorkerComputation() {
        Intent intent = new Intent(getApplicationContext(), Worker_Computation.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.MASTER_ENDPOINT_ID, masterId);
        intent.putExtras(bundle);
        startActivity(intent);
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
        Log.d("WORKER", "Device is not discoverable");
        finish();
    }

    @Override
    public void onBackPressed() {
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
        Log.d("WORKER", "Device is not discoverable");
        if (!masterId.equals("")) {
            NearbySingleton.getInstance(getApplicationContext()).disconnectFromEndpoint(masterId);
            NearbySingleton.getInstance(getApplicationContext()).rejectConnection(masterId);
        }
        finish();
        super.onBackPressed();
    }
}