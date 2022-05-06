package com.group29.mobileoffloading.listeners;

import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.DataModels.WorkDataforWorker;

public interface WorkerStatusListener {

    void onWorkStatusReceived(String nodeIdString, WorkDataforWorker workDataforWorker);

    void onDeviceStatsReceived(String nodeIdString, DeviceInfo deviceStats);

}
