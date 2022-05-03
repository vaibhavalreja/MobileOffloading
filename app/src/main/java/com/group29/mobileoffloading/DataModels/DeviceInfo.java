package com.group29.mobileoffloading.DataModels;

import android.location.Location;

import java.io.Serializable;

public class DeviceInfo implements Serializable {

    private final int batteryPercentage;
    private final boolean chargingStatus;
    private final double latitude;
    private final double longitude;

    public DeviceInfo(Integer batteryPecentage, Boolean chargingStatus, Double latitude, Double longitude){
        this.batteryPercentage = batteryPecentage;
        this.chargingStatus = chargingStatus;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public boolean isCharging() {
        return chargingStatus;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
