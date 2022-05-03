package com.group29.mobileoffloading.BackgroundLoopers;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;

public class Connector {
    public static void sendToDevice(Context context, String nodeIdString, ClientPayLoad tPayload) {
        try {
            Payload payload = PayloadConverter.toPayload(tPayload);
            Connector.sendToDevice(context, nodeIdString, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToDevice(Context context, String nodeIdString, Payload payload) {
        Nearby.getConnectionsClient(context).sendPayload(nodeIdString, payload);
    }
}