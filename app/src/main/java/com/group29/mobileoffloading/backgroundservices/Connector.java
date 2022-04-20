package com.group29.mobileoffloading.backgroundservices;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;

public class Connector {
    public static void sendToDevice(Context context, String endpointId, ClientPayLoad tPayload) {
        try {
            Payload payload = PayloadConverter.toPayload(tPayload);
            Connector.sendToDevice(context, endpointId, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToDevice(Context context, String endpointId, byte[] data) {
        Payload payload = Payload.fromBytes(data);
        Connector.sendToDevice(context, endpointId, payload);
    }

    public static void sendToDevice(Context context, String endpointId, Payload payload) {
        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
    }
}