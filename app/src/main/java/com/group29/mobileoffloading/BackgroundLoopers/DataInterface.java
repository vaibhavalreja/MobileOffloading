package com.group29.mobileoffloading.BackgroundLoopers;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.NodeDataPayload;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;

public class DataInterface {
    public static void sendToDevice(Context context, String nodeIdString, NodeDataPayload tPayload) {
        try {
            Payload payload = PayloadConverter.toPayload(tPayload);
            DataInterface.sendToDevice(context, nodeIdString, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToDevice(Context context, String nodeIdString, Payload payload) {
        Nearby.getConnectionsClient(context).sendPayload(nodeIdString, payload);
    }
}