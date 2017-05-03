package com.uw.hcde.esm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Cissy on 4/17/2017.
 */

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent pushIntent = new Intent(context, DetectAppsService.class);
        context.startService(pushIntent);
    }
}
