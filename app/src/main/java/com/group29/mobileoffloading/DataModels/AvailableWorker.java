package com.group29.mobileoffloading.DataModels;

import java.io.Serializable;

public class AvailableWorker implements Serializable {
    private String endpointId;
    private String endpointName;
    private DeviceInfo deviceInfo;
    private String requestStatus;


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

    public DeviceInfo getDeviceStats() {
        return deviceInfo;
    }

    public void setDeviceStats(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}