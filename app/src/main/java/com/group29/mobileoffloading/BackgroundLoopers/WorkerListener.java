package com.group29.mobileoffloading.BackgroundLoopers;

import android.content.Context;

import com.google.android.gms.nearby.connection.Payload;
import com.group29.mobileoffloading.DataModels.NodeDataPayload;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.DataModels.WorkDataforWorker;
import com.group29.mobileoffloading.Helpers.NearbySingleton;
import com.group29.mobileoffloading.listeners.NodeDataListener;
import com.group29.mobileoffloading.listeners.WorkerStatusListener;
import com.group29.mobileoffloading.utilities.DataPacketStringKeys;
import com.group29.mobileoffloading.utilities.PayloadConverter;

import java.io.IOException;

public class WorkerListener {

    private final Context context;
    private final String nodeIdString;
    private final WorkerStatusListener workerStatusListener;
    private NodeDataListener nodeDataListener;

    public WorkerListener(Context context, String nodeIdString, WorkerStatusListener workerStatusListener) {
        this.context = context;
        this.nodeIdString = nodeIdString;
        this.workerStatusListener = workerStatusListener;
    }

    public void start() {
        nodeDataListener = new NodeDataListener() {
            @Override
            public void onDataReceived(String nodeIdString, Payload payload) {
                try {
                    NodeDataPayload tPayload = PayloadConverter.fromPayload(payload);
                    String payloadTag = tPayload.getTag();

                    if (payloadTag.equals(DataPacketStringKeys.WORK_STATUS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onWorkStatusReceived(nodeIdString, (WorkDataforWorker) tPayload.getData());
                        }
                    } else if (payloadTag.equals(DataPacketStringKeys.DEVICE_STATS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onDeviceStatsReceived(nodeIdString, (DeviceInfo) tPayload.getData());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        NearbySingleton.getInstance(context).registerPayloadListener(nodeDataListener);
        NearbySingleton.getInstance(context).acceptConnection(nodeIdString);
    }

    public void stop() {
        NearbySingleton.getInstance(context).unregisterPayloadListener(nodeDataListener);
    }

}
