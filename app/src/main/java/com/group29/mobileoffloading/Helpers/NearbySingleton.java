package com.group29.mobileoffloading.Helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.Task;
import com.group29.mobileoffloading.listeners.ClientConnectionListener;
import com.group29.mobileoffloading.listeners.NodeDataListener;

import java.util.HashSet;

public class NearbySingleton {

    @SuppressLint("StaticFieldLeak")
    private static NearbySingleton nearbySingleton;
    private final Context context;

    private final ConnectionLifecycleCallback connectionLifecycleCallback;
    private final HashSet<ClientConnectionListener> clientConnectionListenerSet = new HashSet<>();

    private final HashSet<NodeDataListener> nodeDataListenersSet = new HashSet<>();

    public NearbySingleton(Context context) {
        this.context = context;
        this.connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String nodeIdString, @NonNull ConnectionInfo connectionInfo) {
                for (ClientConnectionListener clientConnectionListener : clientConnectionListenerSet) {
                    try {
                        clientConnectionListener.onConnectionInitiated(nodeIdString, connectionInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onConnectionResult(@NonNull String nodeIdString, @NonNull ConnectionResolution connectionResolution) {
                for (ClientConnectionListener clientConnectionListener : clientConnectionListenerSet) {
                    try {
                        clientConnectionListener.onConnectionResult(nodeIdString, connectionResolution);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDisconnected(@NonNull String nodeIdString) {
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
                for (ClientConnectionListener clientConnectionListener : clientConnectionListenerSet) {
                    try {
                        clientConnectionListener.onDisconnected(nodeIdString);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public static NearbySingleton getInstance(Context context) {
        if (nearbySingleton == null) {
            nearbySingleton = new NearbySingleton(context);
        }

        return nearbySingleton;
    }

    public void requestConnection(String nodeIdString, String clientId) {
        Nearby.getConnectionsClient(context)
                .requestConnection(clientId, nodeIdString, connectionLifecycleCallback)
                .addOnSuccessListener(unused -> {
                    
                })
                .addOnFailureListener((Exception e) -> {
                    
                    e.printStackTrace();
                });
    }

    public void acceptConnection(String nodeIdString) {
        Nearby.getConnectionsClient(context).acceptConnection(nodeIdString, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String nodeIdString, @NonNull Payload payload) {
                for (NodeDataListener nodeDataListener : nodeDataListenersSet) {
                    try {
                        nodeDataListener.onDataReceived(nodeIdString, payload);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String nodeIdString, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        });
    }

    public void rejectConnection(String nodeIdString) {
        Nearby.getConnectionsClient(context).rejectConnection(nodeIdString);
    }

    public void disconnectFromEndpoint(String nodeIdString) {
        Nearby.getConnectionsClient(context).disconnectFromEndpoint(nodeIdString);
    }

    public Task<Void> advertise(String clientId, AdvertisingOptions advertisingOptions) {
        return Nearby.getConnectionsClient(context)
                .startAdvertising(clientId, context.getPackageName(), connectionLifecycleCallback, advertisingOptions)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void registerPayloadListener(NodeDataListener nodeDataListener) {
        if (nodeDataListener != null) {
            nodeDataListenersSet.add(nodeDataListener);
        }
    }

    public void registerClientConnectionListener(ClientConnectionListener clientConnectionListener) {
        if (clientConnectionListener != null) {
            clientConnectionListenerSet.add(clientConnectionListener);
        }
    }

    public void unregisterPayloadListener(NodeDataListener nodeDataListener) {
        if (nodeDataListener != null) {
            nodeDataListenersSet.remove(nodeDataListener);
        }
    }


    public void unregisterClientConnectionListener(ClientConnectionListener clientConnectionListener) {
        if (clientConnectionListener != null) {
            clientConnectionListenerSet.remove(clientConnectionListener);
        }
    }
}

