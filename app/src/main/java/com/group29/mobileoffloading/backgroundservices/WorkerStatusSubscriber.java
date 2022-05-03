package com.group29.mobileoffloading.backgroundservices;

import android.content.Context;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.group29.mobileoffloading.listeners.PayloadListener;
import com.group29.mobileoffloading.listeners.WorkerStatusListener;
import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.DataModels.WorkInfo;
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;

public class WorkerStatusSubscriber {

    private final Context context;
    private final String nodeIdString;
    private PayloadListener payloadListener;
    private final WorkerStatusListener workerStatusListener;

    public WorkerStatusSubscriber(Context context, String nodeIdString, WorkerStatusListener workerStatusListener) {
        this.context = context;
        this.nodeIdString = nodeIdString;
        this.workerStatusListener = workerStatusListener;
    }

    public void start() {
        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String nodeIdString, Payload payload) {
                try {
                    ClientPayLoad tPayload = (ClientPayLoad) PayloadConverter.fromPayload(payload);
                    String payloadTag = tPayload.getTag();

                    if (payloadTag.equals(Constants.PayloadTags.WORK_STATUS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onWorkStatusReceived(nodeIdString, (WorkInfo) tPayload.getData());
                        }
                    } else if (payloadTag.equals(Constants.PayloadTags.DEVICE_STATS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onDeviceStatsReceived(nodeIdString, (DeviceInfo) tPayload.getData());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String nodeIdString, PayloadTransferUpdate payloadTransferUpdate) {

            }
        };

        NearbySingleton.getInstance(context).registerPayloadListener(payloadListener);
        NearbySingleton.getInstance(context).acceptConnection(nodeIdString);
    }

    public void stop() {
        NearbySingleton.getInstance(context).unregisterPayloadListener(payloadListener);
    }

}
