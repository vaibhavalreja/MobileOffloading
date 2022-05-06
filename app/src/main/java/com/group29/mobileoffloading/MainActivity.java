package com.group29.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final int REQUEST_CODE_FOR_PERMISSION = 12345;
    String selectedRole = "Master";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = (Spinner) findViewById(R.id.app_role_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.app_roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        (findViewById(R.id.app_role_submit)).setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), MasterSearchActivity.class);
            if(!selectedRole.equals("Master")){
                intent = new Intent(getApplicationContext(), WorkerBroadcastingActivity.class);
            }
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
        String[] permissions = {};//Manifest.permission.READ_EXTERNAL_STORAGE,
                              //  Manifest.permission.ACCESS_FINE_LOCATION,
                            //    Manifest.permission.INTERNET,
                               // Manifest.permission.BLUETOOTH_ADMIN,
                          //      Manifest.permission.ACCESS_WIFI_STATE,
                               // Manifest.permission.BLUETOOTH,
                            //    Manifest.permission.CHANGE_WIFI_STATE,
                           //     Manifest.permission.ACCESS_COARSE_LOCATION,
                           //     Manifest.permission.ACCESS_BACKGROUND_LOCATION};
        if(Build.VERSION.SDK_INT >= 30){
            //permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            //permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            //permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        selectedRole  = (String) adapterView.getItemAtPosition(i);

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}