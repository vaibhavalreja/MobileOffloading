package com.group29.mobileoffloading.DataModels;

import java.io.Serializable;

public class Worker implements Serializable {

    private String endpointId, endpointName;
    private DeviceInfo deviceInfo;
    private WorkInfo workInfo;

    private int workQuantity;
    private float distanceFromMaster;

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public WorkInfo getWorkStatus() {
        return workInfo;
    }

    public void setWorkStatus(WorkInfo workStatus) {
        this.workInfo = workStatus;
    }

    public DeviceInfo getDeviceStats() {
        return deviceInfo;
    }

    public void setDeviceStats(DeviceInfo deviceStats) {
        this.deviceInfo = deviceStats;
    }

    public int getWorkAmount() {
        return workQuantity;
    }

    public void setWorkAmount(int workAmount) {
        this.workQuantity = workAmount;
    }

    public float getDistanceFromMaster() {
        return distanceFromMaster;
    }

    public void setDistanceFromMaster(float distanceFromMaster) {
        this.distanceFromMaster = distanceFromMaster;
    }

}
