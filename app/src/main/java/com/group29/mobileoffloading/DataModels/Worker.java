package com.group29.mobileoffloading.DataModels;

import java.io.Serializable;

public class Worker implements Serializable {

    private String nodeIdString, endpointName;
    private DeviceInfo deviceInfo;
    private WorkDataforWorker workDataforWorker;

    private int workQuantity;
    private float distanceFromMaster;

    public String getEndpointId() {
        return nodeIdString;
    }

    public void setEndpointId(String nodeIdString) {
        this.nodeIdString = nodeIdString;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public WorkDataforWorker getWorkStatus() {
        return workDataforWorker;
    }

    public void setWorkStatus(WorkDataforWorker workStatus) {
        this.workDataforWorker = workStatus;
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
