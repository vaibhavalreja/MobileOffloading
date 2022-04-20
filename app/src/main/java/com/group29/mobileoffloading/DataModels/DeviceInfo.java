package com.group29.mobileoffloading.DataModels;

import android.location.Location;

import java.io.Serializable;

public class DeviceInfo implements Serializable {

    private int batteryLevel;
    private boolean charging;

    private double latitude;
    private double longitude;

    private boolean locationValid;

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isLocationValid() {
        return locationValid;
    }

    public void setLocation(Location loc) {
        if(loc != null) {
            this.latitude = loc.getLatitude();
            this.longitude = loc.getLongitude();
            this.locationValid = true;
        } else {
            this.locationValid = false;
        }
    }
}
