package com.group29.mobileoffloading.utilities;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.models.ClientPayLoad;

import java.io.IOException;

public class DataTransfer {
    public static void sendPayload(Context context, String endpointId, ClientPayLoad tPayload) {
        try {
            Payload payload = PayloadConverter.toPayload(tPayload);
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}