package com.group29.mobileoffloading.listeners;

import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.DataModels.WorkInfo;

public interface WorkerStatusListener {

    void onWorkStatusReceived(String nodeIdString, WorkInfo workInfo);

    void onDeviceStatsReceived(String nodeIdString, DeviceInfo deviceStats);

}
