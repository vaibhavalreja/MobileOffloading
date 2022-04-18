package com.group29.mobileoffloading.backgroundservices;

import android.content.Context;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.group29.mobileoffloading.listeners.PayloadListener;
import com.group29.mobileoffloading.listeners.WorkerStatusListener;
import com.group29.mobileoffloading.models.ClientPayLoad;
import com.group29.mobileoffloading.models.DeviceStatistics;
import com.group29.mobileoffloading.models.WorkInfo;
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;

public class WorkerStatusSubscriber {

    private Context context;
    private String endpointId;
    private PayloadListener payloadListener;
    private WorkerStatusListener workerStatusListener;

    public WorkerStatusSubscriber(Context context, String endpointId, WorkerStatusListener workerStatusListener) {
        this.context = context;
        this.endpointId = endpointId;
        this.workerStatusListener = workerStatusListener;
    }

    public void start() {
        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                try {
                    ClientPayLoad tPayload = (ClientPayLoad) PayloadConverter.fromPayload(payload);
                    String payloadTag = tPayload.getTag();

                    if (payloadTag.equals(Constants.PayloadTags.WORK_STATUS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onWorkStatusReceived(endpointId, (WorkInfo) tPayload.getData());
                        }
                    } else if (payloadTag.equals(Constants.PayloadTags.DEVICE_STATS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onDeviceStatsReceived(endpointId, (DeviceStatistics) tPayload.getData());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {

            }
        };

        NearbyConnectionsManager.getInstance(context).registerPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(context).acceptConnection(endpointId);
    }

    public void stop() {
        NearbyConnectionsManager.getInstance(context).unregisterPayloadListener(payloadListener);
    }

}