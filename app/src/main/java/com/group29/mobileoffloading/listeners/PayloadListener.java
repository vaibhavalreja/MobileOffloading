package com.group29.mobileoffloading.listeners;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public interface PayloadListener {
    void onPayloadReceived(String nodeIdString, Payload payload);
    void onPayloadTransferUpdate( String nodeIdString,  PayloadTransferUpdate payloadTransferUpdate);
}