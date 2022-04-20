package com.group29.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_FOR_PERMISSION = 12345;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button)findViewById(R.id.role_master)).setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Master_Discovery.class);
            startActivity(intent);
        });

        ((Button)findViewById(R.id.role_worker)).setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Worker.class);
            startActivity(intent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_FOR_PERMISSION) {
            for (int isGranted : grantResults) {
                if (isGranted == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(getApplicationContext(), "Some Permissions are missing.\nPlease go to settings and allow all the requested permissions", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkAndAskPermissions() {
        ArrayList<String> permissionsToRequest = new ArrayList<String>();
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.INTERNET,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.CHANGE_WIFI_STATE,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION};

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if(permissionsToRequest.size() > 0){
            String[] requestPermissions = new String[permissionsToRequest.size()];
            requestPermissions = permissionsToRequest.toArray(requestPermissions);
            ActivityCompat.requestPermissions(this, requestPermissions, REQUEST_CODE_FOR_PERMISSION);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onResume() {
        super.onResume();
        checkAndAskPermissions();
    }
}