package com.group29.mobileoffloading.listeners;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;

public interface ClientConnectionListener {
    void onConnectionInitiated(String nodeIdString, ConnectionInfo connectionInfo);

    void onConnectionResult(String nodeIdString, ConnectionResolution connectionResolution);

    void onDisconnected(String nodeIdString);
}
