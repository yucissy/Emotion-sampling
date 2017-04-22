package com.example.cissy.esm;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.rvalerio.fgchecker.AppChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static java.security.AccessController.getContext;

/**
 * Created by Cissy on 4/17/2017.
 */

public class DetectAppsService extends Service {

    private static final List<String> targetApps = Arrays.asList("com.facebook.katana","com.whatsapp",
            "com.snapchat.android","com.facebook.orca","com.instagram.android","com.twitter.android",
            "com.netflix.mediaclient","com.google.android.youtube","com.king.candycrushsaga", "com.zynga.words",
            "com.nintendo.zara", "com.google.android.gm","com.microsoft.office.outlook", "me.bluemail.mail",
            "com.google.android.apps.inbox", "com.android.email");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private String sampledApp;
    private static Tracker mTracker;

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onCreate() {
        super.onCreate();

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        final Handler h = new Handler();
        final int delay = 1000; //milliseconds

        h.postDelayed(new Runnable(){
            String lastApp = null;
            Sample lastSample = null;
            long lastSampledTime = 0;
            int secondsPassed = -1;
            int secondToSample = 0;

            public void run(){
                AppChecker appChecker = new AppChecker();
                String packageName = appChecker.getForegroundApp(getApplicationContext());

                //If user just opened a target app
                if (targetApps.contains(packageName) && (!packageName.equals(lastApp) &&
                        (System.currentTimeMillis() - lastSampledTime > 0))) {
                    //should be 30000
                    final Sample sample = Sample.getRandomSample(lastSample);
                    Log.d("c", "sampled: "+ sample.getType());
                    lastSample = sample;
                    sampledApp = packageName;
                    sample.setPackageName(packageName);

                    if (sample.getType() == Sample.BEFORE) {
                        lastSampledTime = System.currentTimeMillis();
                        popupSample(sample);
                    }
                    else {
                        boolean continuousUse = true;
                        int waitPeriod;

                        if (sample.getType() == Sample.DURING) {
                            waitPeriod = getRandom15and120();
                        }
                        else {
                            waitPeriod = 15;
                        }

                        int waitedSoFar = 0;
                        while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName) && waitedSoFar < waitPeriod) {
                            sleep(1000);
                            waitedSoFar++;
                        }
                        continuousUse = (waitedSoFar == waitPeriod);

                        String currentApp = appChecker.getForegroundApp(getApplicationContext());
                        if (currentApp.equals(packageName) && sample.getType() == Sample.DURING && continuousUse){
                            lastSampledTime = System.currentTimeMillis();
                            sample.setSampleTime(waitPeriod);
                            popupSample(sample);
                        }
                        else if (sample.getType() == Sample.AFTER && continuousUse) {
                            int elapsed = 0;
                            while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName)) {
                                sleep(1000);
                                elapsed++;
                            }
                            lastSampledTime = System.currentTimeMillis();
                            sample.setSampleTime(elapsed+15);
                            popupSample(sample);
                        }
                    }
                }
                lastApp = packageName;

                h.postDelayed(this, delay);
            }
        }, delay);
    }

    public String getSampledApp() {
        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(sampledApp,0);
        } catch (Exception e) {
            ai = null;
        }
        final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
        Log.d("c", applicationName);
        return applicationName;
    }

    private void sleep(int length) {
        try {
            Thread.sleep(length);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void popupSample(final Sample sample) {
        wm = getWindowManager();
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                0,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.setTitle("Load Average");

        final View popup = sample.getPopup(this);

        wm.addView(popup, params);
    }

    private int getRandom15and120() {
        Random rand = new Random();
        return rand.nextInt(106) + 15;
    }

    public void removeView(View v) {
        wm.removeView(v);
    }

    public void addView(View v) {
        wm.addView(v, params);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class RecentUseComparator implements Comparator<UsageStats> {
        @Override
        public int compare(UsageStats lhs, UsageStats rhs) {
            return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed()) ? -1 : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
        }
    }

    private WindowManager getWindowManager() {
        return (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
    }

    private Service getService() {
        return this;
    }

    public static void sendEvent(Service service, String category, String action, String label) {
        if (mTracker == null) {
            return;
        }

        if (label != null && !label.equals("") && !label.startsWith(";")) {
            label = ";" + label;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service.getBaseContext());
        String code = prefs.getString(service.getString(R.string.invite_code), "noInviteCode");

        String taggedLabel = "PID=" + code;
        taggedLabel += ";timestamp=" + System.currentTimeMillis();
        taggedLabel += label;

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(taggedLabel)
                .build());
    }
}
