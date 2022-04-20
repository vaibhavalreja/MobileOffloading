package com.group29.mobileoffloading;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.group29.mobileoffloading.backgroundservices.DeviceStatisticsPublisher;
import com.group29.mobileoffloading.backgroundservices.NearbyConnectionsManager;
import com.group29.mobileoffloading.backgroundservices.WorkerAdvertisingHelper;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.utilities.Constants;

public class WorkerActivity extends AppCompatActivity {
    private WorkerAdvertisingHelper workerAdvertisingHelper;
    private String workerId;
    private String masterId = "";
    private ClientConnectionListener connectionListener;
    private Dialog confirmationDialog;
    private DeviceStatisticsPublisher deviceStatsPublisher;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);
        workerId = Build.MANUFACTURER + " " + Build.MODEL;
        initialiseDialog();
        //Start Advertisement
        workerAdvertisingHelper = new WorkerAdvertisingHelper(getApplicationContext());
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), null, Constants.UPDATE_INTERVAL_UI);
        setDeviceId("Device ID: " + workerId);
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            refreshCardData();
            handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
        };
        setConnectionCallback();
    }

    void setConnectionCallback() {
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
    }

    void setState(String text) {
        ((TextView) findViewById(R.id.statusText)).setText(text);
    }

    void setDeviceId(String text) {

    }

    void refreshCardData() {
        TextView st = findViewById(R.id.percentage);
        st.setText("Percentage: " + DeviceStatisticsPublisher.getBatteryLevel(this) + "%");
        TextView st2 = findViewById(R.id.plugged);
        st2.setText(String.format("Charging Status: %s", DeviceStatisticsPublisher.isPluggedIn(this) ? "Plugged In" : "Not Charging"));
        if (DeviceStatisticsPublisher.getLocation(this) != null) {
            TextView la = findViewById(R.id.latitude);
            la.setText(String.format("Latitude: %s", DeviceStatisticsPublisher.getLocation(this).getLatitude()));
            TextView lo = findViewById(R.id.longitude);
            lo.setText("Longitude: " + DeviceStatisticsPublisher.getLocation(this).getLongitude());
        } else {
            TextView la = findViewById(R.id.latitude);
            la.setText("Latitude: Not Available");
            TextView lo = findViewById(R.id.longitude);
            lo.setText("Longitude: Not Available");
        }
    }


    void initialiseDialog() {
        confirmationDialog = new BottomSheetDialog(this);
        confirmationDialog.setContentView(R.layout.confirmation_dialog);
        confirmationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        confirmationDialog.findViewById(R.id.accept).setOnClickListener(v -> acceptConnection());
        confirmationDialog.findViewById(R.id.reject).setOnClickListener(v -> rejectConnection());
    }

    void showDialog(String masterInfo) {
        TextView title = confirmationDialog.findViewById(R.id.dialogText);
        title.setText(String.format("%s is trying to connect. Do you accept the connection ?", masterInfo));
        confirmationDialog.show();
    }

    void acceptConnection() {
        workerAdvertisingHelper.acceptConnection(masterId);
        confirmationDialog.dismiss();
        startWorkerComputation();
    }

    void rejectConnection() {
        workerAdvertisingHelper.rejectConnection(masterId);
        confirmationDialog.dismiss();
    }

    @Override
    protected void onResume() {
        setState("Initializing...");
        super.onResume();
        workerAdvertisingHelper.advertise(workerId).addOnSuccessListener(command -> {
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
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(connectionListener);
        Log.d("WORKER", "Starting Device Stats");
        deviceStatsPublisher.start();
        handler.postDelayed(runnable, Constants.UPDATE_INTERVAL_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        Log.d("WORKER", "Stopping Device Stats");
        deviceStatsPublisher.stop();
        handler.removeCallbacks(runnable);
    }

    private void startWorkerComputation() {
        Intent intent = new Intent(getApplicationContext(), Worker_Computation.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.MASTER_ENDPOINT_ID, masterId);
        intent.putExtras(bundle);
        startActivity(intent);
        workerAdvertisingHelper.stopAdvertising();
        Log.d("WORKER", "Device is not discoverable");
        finish();
    }

    @Override
    public void onBackPressed() {
        workerAdvertisingHelper.stopAdvertising();
        Log.d("WORKER", "Device is not discoverable");
        if (!masterId.equals("")) {
            NearbyConnectionsManager.getInstance(getApplicationContext()).disconnectFromEndpoint(masterId);
            NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(masterId);
        }
        finish();
        super.onBackPressed();
    }
}