package com.group29.mobileoffloading.listeners;

import com.group29.mobileoffloading.DataModels.DeviceStatistics;
import com.group29.mobileoffloading.DataModels.WorkInfo;

public interface WorkerStatusListener {

    void onWorkStatusReceived(String endpointId, WorkInfo workInfo);

    void onDeviceStatsReceived(String endpointId, DeviceStatistics deviceStats);

}
