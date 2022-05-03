package com.group29.mobileoffloading.Helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class FusedLocationHelper {
    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static FusedLocationHelper fusedLocationHelper;
    private Location lastestLocation;
    private LocationCallback locationCallback;


    public FusedLocationHelper(Context context) {
        this.context = context;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                lastestLocation = locationResult.getLastLocation();
            }
        };
    }

    public static FusedLocationHelper getInstance(Context context) {
        if (fusedLocationHelper == null) {
            fusedLocationHelper = new FusedLocationHelper(context);
        }
        return fusedLocationHelper;
    }

    public Location getLastAvailableLocation() {
        return this.lastestLocation;
    }

    public void start(int interval) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setFastestInterval(interval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(interval);
        locationRequest.setSmallestDisplacement(1);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location Permission is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, context.getMainLooper());
    }

    public void stop() {
        if (this.locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
        }
    }
}