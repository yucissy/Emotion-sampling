package com.uw.hcde.esm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.util.Log;

public class SensorRestarterBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(SensorRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
        if (!HomeScreenActivity.isMyServiceRunning(DetectAppsService.class, context)) {
            context.startService(new Intent(context, DetectAppsService.class));
        }
    }
}