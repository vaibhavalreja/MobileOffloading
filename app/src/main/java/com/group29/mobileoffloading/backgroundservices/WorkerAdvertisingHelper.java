package com.group29.mobileoffloading.backgroundservices;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.Task;

public class WorkerAdvertisingHelper {

    private Context context;
    private AdvertisingOptions advtOptions;

    public WorkerAdvertisingHelper(Context context) {
        this.context = context;
        this.advtOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
    }

    public Task<Void> advertise(String workerId) {
        return NearbyConnectionsManager.getInstance(context).advertise(workerId, advtOptions);
    }

    public void stopAdvertising() {
        Nearby.getConnectionsClient(context).stopAdvertising();
    }

    public void acceptConnection(String masterId){
        NearbyConnectionsManager.getInstance(context).acceptConnection(masterId);
    }

    public void rejectConnection(String masterId){
        NearbyConnectionsManager.getInstance(context).rejectConnection(masterId);
    }

}
