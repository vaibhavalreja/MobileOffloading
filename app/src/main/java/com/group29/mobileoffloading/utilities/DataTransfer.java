package com.group29.mobileoffloading.utilities;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;

import java.io.IOException;

public class DataTransfer {
    public static void sendPayload(Context context, String nodeIdString, ClientPayLoad tPayload) {
        try {
            Payload payload = PayloadConverter.toPayload(tPayload);
            Nearby.getConnectionsClient(context).sendPayload(nodeIdString, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}