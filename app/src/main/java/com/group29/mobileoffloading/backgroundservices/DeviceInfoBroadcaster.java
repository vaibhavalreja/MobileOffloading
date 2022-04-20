package com.group29.mobileoffloading.backgroundservices;

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
import com.group29.mobileoffloading.utilities.Constants;
import com.group29.mobileoffloading.utilities.DataTransfer;

public class DeviceInfoBroadcaster {

    private Context context;
    private String endpointId;
    private Handler handler;
    private Runnable runnable;
    private int interval;

    public DeviceInfoBroadcaster(Context context, String endpointId, int updateInterval) {
        this.context = context;
        this.endpointId = endpointId;
        this.interval = updateInterval;
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            publish();
            handler.postDelayed(runnable, interval);
        };
    }

    public void start() {
        handler.postDelayed(runnable,  interval);
        LocationService.getInstance(context).start(interval);
    }

    public void stop() {
        handler.removeCallbacks(runnable);
        LocationService.getInstance(context).stop();
    }

    private void publish() {
        DeviceInfoBroadcaster.publish(this.context, this.endpointId);
    }

    public static void publish(Context context, String endpointId) {
        // Get Device Statistics
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setBatteryLevel(getBatteryLevel(context));
        deviceInfo.setCharging(isPluggedIn(context));
        deviceInfo.setLocation(getLocation(context));
        if(endpointId != null) {
            ClientPayLoad payload = new ClientPayLoad().setTag(Constants.PayloadTags.DEVICE_STATS).setData(deviceInfo);
            DataTransfer.sendPayload(context, endpointId, payload);
        }
        Log.d("DEVICE_STATS", "DEVICE STATUS B: " + deviceInfo.getBatteryLevel() + " P: " + deviceInfo.isCharging() +  " L: " + deviceInfo.getLatitude() + " " + deviceInfo.getLongitude());
    }

    public static Location getLocation(Context context) {
        Location location = LocationService.getInstance(context).getLastAvailableLocation();
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
        if (isCharging == BatteryManager.BATTERY_PLUGGED_AC
                || isCharging == BatteryManager.BATTERY_PLUGGED_USB
                || isCharging == BatteryManager.BATTERY_PLUGGED_WIRELESS) b = true;
        else b = false;
        return b;
    }

}
