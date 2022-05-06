package com.group29.mobileoffloading.listeners;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public interface NodeDataListener {
    void onDataReceived(String nodeIdString, Payload payload);
}