package com.group29.mobileoffloading.BackgroundLoopers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

import com.group29.mobileoffloading.DataModels.NodeDataPayload;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.Helpers.FusedLocationHelper;
import com.group29.mobileoffloading.utilities.DataPacketStringKeys;
import com.group29.mobileoffloading.utilities.DataTransfer;

public class DeviceInfoBroadcaster {

    private final Context context;
    private final String nodeIdString;
    private final Handler handler;
    private Runnable runnable;

    public DeviceInfoBroadcaster(Context context, String nodeIdString) {
        this.context = context;
        this.nodeIdString = nodeIdString;
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            Broadcast();
            handler.postDelayed(runnable, 7000);
        };
    }

    public static void Broadcast(Context context, String nodeIdString) {
        DeviceInfo deviceInfo = new DeviceInfo(getBatteryLevel(context),
                isPluggedIn(context),
                getLocation(context).getLatitude(),
                getLocation(context).getLongitude()
        );
        if (nodeIdString != null) {
            NodeDataPayload payload = new NodeDataPayload().setTag(DataPacketStringKeys.DEVICE_STATS).setData(deviceInfo);
            DataTransfer.sendPayload(context, nodeIdString, payload);
        }
        
    }

    public static Location getLocation(Context context) {
        Location location = FusedLocationHelper.getInstance(context).getLastAvailableLocation();
        return location;
    }

    public static int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static boolean isPluggedIn(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        final boolean b;
        b = isCharging == BatteryManager.BATTERY_PLUGGED_AC
                || isCharging == BatteryManager.BATTERY_PLUGGED_USB
                || isCharging == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        return b;
    }

    public void begin() {
        FusedLocationHelper.getInstance(context).start(7000);
        handler.postDelayed(runnable, 7000);

    }

    public void destroy() {
        FusedLocationHelper.getInstance(context).stop();
        handler.removeCallbacks(runnable);
    }

    private void Broadcast() {
        DeviceInfoBroadcaster.Broadcast(this.context, this.nodeIdString);
    }

}
