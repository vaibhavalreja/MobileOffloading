package com.group29.mobileoffloading.backgroundservices;

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
import com.group29.mobileoffloading.listeners.PayloadListener;

import java.util.HashSet;

public class NearbySingleton {

    private static NearbySingleton nearbySingleton;
    private Context context;

    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private HashSet<ClientConnectionListener> clientConnectionListenerSet = new HashSet<>();

    private HashSet<PayloadListener> payloadListenersSet = new HashSet<>();

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
                    Log.d("NEARBYCONNCTNMGR", "CONNECTION REQUESTED");
                })
                .addOnFailureListener((Exception e) -> {
                    Log.d("NEARBYCONNCTNMGR", "CONNECTION FAILED");
                    e.printStackTrace();
                });
    }

    public void acceptConnection(String nodeIdString) {
        Nearby.getConnectionsClient(context).acceptConnection(nodeIdString, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String nodeIdString, @NonNull Payload payload) {
                for (PayloadListener payloadListener : payloadListenersSet) {
                    try {
                        payloadListener.onPayloadReceived(nodeIdString, payload);
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
                .addOnFailureListener((Exception e) -> {
                    e.printStackTrace();
                });
    }

    public boolean registerPayloadListener(PayloadListener payloadListener) {
        if (payloadListener != null) {
            return payloadListenersSet.add(payloadListener);
        }
        return false;
    }

    public boolean registerClientConnectionListener(ClientConnectionListener clientConnectionListener) {
        if (clientConnectionListener != null) {
            return clientConnectionListenerSet.add(clientConnectionListener);
        }
        return false;
    }

    public boolean unregisterPayloadListener(PayloadListener payloadListener) {
        if (payloadListener != null) {
            return payloadListenersSet.remove(payloadListener);
        }
        return false;
    }


    public boolean unregisterClientConnectionListener(ClientConnectionListener clientConnectionListener) {
        if (clientConnectionListener != null) {
            return clientConnectionListenerSet.remove(clientConnectionListener);
        }
        return false;
    }
}

