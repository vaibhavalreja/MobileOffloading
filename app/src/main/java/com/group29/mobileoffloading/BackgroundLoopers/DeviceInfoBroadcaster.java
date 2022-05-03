package com.group29.mobileoffloading.BackgroundLoopers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.group29.mobileoffloading.DataModels.ClientPayLoad;
import com.group29.mobileoffloading.DataModels.DeviceInfo;
import com.group29.mobileoffloading.Helpers.FusedLocationHelper;
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.DataTransfer;

public class DeviceInfoBroadcaster {

    private final Context context;
    private final String nodeIdString;
    private final Handler handler;
    private Runnable runnable;
    private final int interval;

    public DeviceInfoBroadcaster(Context context, String nodeIdString, int updateInterval) {
        this.context = context;
        this.nodeIdString = nodeIdString;
        this.interval = updateInterval;
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            publish();
            handler.postDelayed(runnable, interval);
        };
    }

    public static void publish(Context context, String nodeIdString) {
        DeviceInfo deviceInfo = new DeviceInfo(getBatteryLevel(context),
                isPluggedIn(context),
                getLocation(context).getLatitude(),
                getLocation(context).getLongitude()
        );
        if (nodeIdString != null) {
            ClientPayLoad payload = new ClientPayLoad().setTag(Constants.PayloadTags.DEVICE_STATS).setData(deviceInfo);
            DataTransfer.sendPayload(context, nodeIdString, payload);
        }
        Log.d("DEVICE_STATS", "DEVICE STATUS B: " + deviceInfo.getBatteryPercentage() + " P: " + deviceInfo.isCharging() + " L: " + deviceInfo.getLatitude() + " " + deviceInfo.getLongitude());
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

    public void start() {
        handler.postDelayed(runnable, interval);
        FusedLocationHelper.getInstance(context).start(interval);
    }

    public void stop() {
        handler.removeCallbacks(runnable);
        FusedLocationHelper.getInstance(context).stop();
    }

    private void publish() {
        DeviceInfoBroadcaster.publish(this.context, this.nodeIdString);
    }

}
