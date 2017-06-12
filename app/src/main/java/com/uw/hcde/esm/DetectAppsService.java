package com.uw.hcde.esm;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.rvalerio.fgchecker.AppChecker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Cissy on 4/17/2017.
 */

public class DetectAppsService extends Service {

    private static final List<String> launcherApps = new ArrayList();
    private static DetectAppsService instance;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private String sampledApp;
    private static Tracker mTracker;
    // Add this empty layout:
    private MyLayout myLayout;
    private HomeWatcher mHomeWatcher;
    private Sample currentSample;
    private static int FOREGROUND_ID=16;
    private int lastSampleType;
    private boolean cancelPrompt;
    private Handler cancelPromptHandler;
    private cancelPromptCommand cancelPromptRunnable;
    private List<String> installedApps;
    private List<String> pastSamples;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onCreate() {
        super.onCreate();

        instance = this;

        final DetectAppsService thisService = this;
        mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                thisService.hide(true);  //means: windowManager.removeView(view);
            }
            @Override
            public void onHomeLongPressed() {
            }
        });

        setAlarmManager();

        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
        for (ResolveInfo resolveInfo : lst) {
            launcherApps.add(resolveInfo.activityInfo.packageName.toString());
        }

        pastSamples = new LinkedList<>();
        installedApps = new LinkedList<String>();
        populateInstalledApps();

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        final Handler h = new Handler();
        final int delay = 1000; //milliseconds

        lastSampleType = PreferenceManager.getDefaultSharedPreferences(thisService).getInt("lastSampleType", -1);
        String pastSample1 = PreferenceManager.getDefaultSharedPreferences(thisService).getString("pastSample1", "");
        String pastSample2 = PreferenceManager.getDefaultSharedPreferences(thisService).getString("pastSample2", "");
        String pastSample3 = PreferenceManager.getDefaultSharedPreferences(thisService).getString("pastSample3", "");
        pastSamples.add(pastSample1);
        pastSamples.add(pastSample2);
        pastSamples.add(pastSample3);
        Log.d("c", "past samples");
        System.out.println(pastSamples);

        final AppChecker appChecker = new AppChecker();
        appChecker.other(new AppChecker.Listener() {
                             String lastApp = null;
                             long lastSampledTime = 0;
                             long launchTime = System.currentTimeMillis();
                             boolean sampled = false;
                             Sample sample;

                             @Override
                             public void onForeground(String packageName) {

                    //If user just opened a target app
                    if (packageName != null && !packageName.equals(lastApp)) {
                        if (!pastSamples.contains(packageName) && installedApps.contains(packageName) && System.currentTimeMillis() - lastSampledTime > 1800000) {

                            sample = Sample.getRandomSample(lastSampleType, packageName);
                            //sample = new Sample(Sample.BEFORE, packageName);
                            Log.d("c", "sampled: "+sample.getType());
                            System.out.println(pastSamples);
                            currentSample = sample;
                            sampledApp = packageName;
                            sample.setPackageName(packageName);
                            cancelPrompt = true;

                            if (sample.getType() == Sample.BEFORE) {
                                sampled = true;
                                popupSample(sample);
                                beginTimer();
                                pastSamples.add(packageName);
                                lastSampledTime = System.currentTimeMillis();
                                lastSampleType = sample.getType();
                            }
                            else {
                                boolean continuousUse = true;
                                int waitPeriod;

                                if (sample.getType() == Sample.DURING) {
                                    waitPeriod = getRandom15and120();
                                }
                                else {
                                    waitPeriod = 15000;
                                }

                                int waitedSoFar = 0;
                                while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName) && waitedSoFar < waitPeriod) {
                                    sleep(1000);
                                    waitedSoFar+=1000;
                                }
                                continuousUse = (waitedSoFar == waitPeriod);

                                String currentApp = appChecker.getForegroundApp(getApplicationContext());
                                if (currentApp.equals(packageName) && sample.getType() == Sample.DURING && continuousUse){
                                    sample.setSampleTime(waitPeriod);
                                    sampled = true;
                                    popupSample(sample);
                                    pastSamples.add(packageName);
                                    beginTimer();
                                    lastSampledTime = System.currentTimeMillis();
                                    lastSampleType = sample.getType();
                                }
                                else if (sample.getType() == Sample.AFTER && continuousUse) {
                                    int elapsed = 0;
                                    sampled = true;
                                    while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName)) {
                                        sleep(1000);
                                        elapsed+=1000;
                                    }
                                    sample.setSampleTime(elapsed+15000);
                                    sample.setAppEnd(elapsed);
                                    popupSample(sample);
                                    pastSamples.add(packageName);
                                    beginTimer();
                                    lastSampledTime = System.currentTimeMillis();
                                    lastSampleType = sample.getType();
                                }
                            }
                        }

                        if (pastSamples.size() > 3) {
                            pastSamples.remove(0);
                        }

                        long duration = System.currentTimeMillis() - launchTime;
                        launchTime = System.currentTimeMillis();
                        if (!launcherApps.contains(lastApp)) {
                            if (sampled && sample.getType() != Sample.AFTER) {
                                Event event = sample.getEvent();
                                event.setDurationAfter(System.currentTimeMillis() - sample.getEndTime());
                                sendEvent(thisService, sample.getCategory(), sample.getPromptType(), event);
                                sampled = false;
                                sample = null;
                            }
                            else if (!sampled && installedApps.contains(lastApp)) {
                                Event event = new Event.EventBuilder(0, launchTime, DetectAppsService.getLocalDateTime(), lastApp)
                                        .durationTotal(duration)
                                        .build();
                                sendEvent(getService(), "non-sample", "non-sample", event);
                                sampled = false;
                            }
                        }
                    }
                    lastApp = packageName;
                                 // do something
                             }
                         }
                    ).timeout(500).start(this);

//        h.postDelayed(new Runnable(){
//
//
//
//            public void run(){
//
//                try {
//                    AppChecker appChecker = new AppChecker();
//                    String packageName = appChecker.getForegroundApp(getApplicationContext());
//
//                    //TODO: Change these back after testing
//                    //If user just opened a target app
//                    if (!packageName.equals(lastApp)) {
//                        if (!pastSamples.contains(packageName) && installedApps.contains(packageName) && System.currentTimeMillis() - lastSampledTime > 1800000) {
//                            //1800000
//
//                            sample = Sample.getRandomSample(lastSampleType, packageName);
//                            //sample = new Sample(Sample.BEFORE, packageName);
//                            Log.d("c", "sampled: "+sample.getType());
//                            System.out.println(pastSamples);
//                            currentSample = sample;
//                            sampledApp = packageName;
//                            sample.setPackageName(packageName);
//                            cancelPrompt = true;
//
//                            if (sample.getType() == Sample.BEFORE) {
//                                sampled = true;
//                                popupSample(sample);
//                                beginTimer();
//                                pastSamples.add(packageName);
//                                lastSampledTime = System.currentTimeMillis();
//                                lastSampleType = sample.getType();
//                            }
//                            else {
//                                boolean continuousUse = true;
//                                int waitPeriod;
//
//                                if (sample.getType() == Sample.DURING) {
//                                    waitPeriod = getRandom15and120();
//                                }
//                                else {
//                                    waitPeriod = 15000;
//                                }
//
//                                int waitedSoFar = 0;
//                                while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName) && waitedSoFar < waitPeriod) {
//                                    sleep(1000);
//                                    waitedSoFar+=1000;
//                                }
//                                continuousUse = (waitedSoFar == waitPeriod);
//
//                                String currentApp = appChecker.getForegroundApp(getApplicationContext());
//                                if (currentApp.equals(packageName) && sample.getType() == Sample.DURING && continuousUse){
//                                    sample.setSampleTime(waitPeriod);
//                                    sampled = true;
//                                    popupSample(sample);
//                                    pastSamples.add(packageName);
//                                    beginTimer();
//                                    lastSampledTime = System.currentTimeMillis();
//                                    lastSampleType = sample.getType();
//                                }
//                                else if (sample.getType() == Sample.AFTER && continuousUse) {
//                                    int elapsed = 0;
//                                    sampled = true;
//                                    while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName)) {
//                                        sleep(1000);
//                                        elapsed+=1000;
//                                    }
//                                    sample.setSampleTime(elapsed+15000);
//                                    sample.setAppEnd(elapsed);
//                                    popupSample(sample);
//                                    pastSamples.add(packageName);
//                                    beginTimer();
//                                    lastSampledTime = System.currentTimeMillis();
//                                    lastSampleType = sample.getType();
//                                }
//                            }
//                        }
//
//                        if (pastSamples.size() > 3) {
//                            pastSamples.remove(0);
//                        }
//
//                        long duration = System.currentTimeMillis() - launchTime;
//                        launchTime = System.currentTimeMillis();
//                        if (!launcherApps.contains(lastApp)) {
//                            if (sampled && sample.getType() != Sample.AFTER) {
//                                Event event = sample.getEvent();
//                                event.setDurationAfter(System.currentTimeMillis() - sample.getEndTime());
//                                sendEvent(thisService, sample.getCategory(), sample.getPromptType(), event);
//                                sampled = false;
//                                sample = null;
//                            }
//                            else if (!sampled && installedApps.contains(lastApp)) {
//                                Event event = new Event.EventBuilder(0, launchTime, DetectAppsService.getLocalDateTime(), lastApp)
//                                        .durationTotal(duration)
//                                        .build();
//                                sendEvent(getService(), "non-sample", "non-sample", event);
//                                sampled = false;
//                            }
//                        }
//                    }
//                    lastApp = packageName;
//                    h.postDelayed(this, delay);
//                }
//                catch (Exception e) {
//                    Log.d("c", "Exception Caught");
//                }
//            }
//        }, delay);

    }

    private void populateInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for(ApplicationInfo packageInfo:packages){
            if( pm.getLaunchIntentForPackage(packageInfo.packageName) != null ){
                String currAppName = packageInfo.packageName;
                installedApps.add(currAppName);
            }
        }
        installedApps.remove("com.uw.hcde.esm");
        installedApps.remove("com.android.settings");
    }

    private void setAlarmManager() {
        Intent intentAlarm = new Intent(this, SensorRestarterBroadcastReceiver.class);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 1800000, pi);
    }

    private void beginTimer() {
        Log.d("c", "timer begun");
        cancelPromptHandler = new Handler();
        cancelPromptRunnable = new cancelPromptCommand(this);
        cancelPromptHandler.postDelayed(cancelPromptRunnable, 30000);
    }

    private class cancelPromptCommand implements Runnable {
        DetectAppsService mService;
        public cancelPromptCommand(DetectAppsService service) {
            mService = service;
        }
        @Override
        public void run() {
            if (cancelPrompt && myLayout.getWindowToken() != null) {
                mService.hide(true);
            }
        }
    };

    public void restartTimer() {
        cancelPromptHandler.removeCallbacks(cancelPromptRunnable);
        cancelPromptRunnable = new cancelPromptCommand(this);
        cancelPromptHandler.postDelayed(cancelPromptRunnable, 30000);
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
        params.gravity = Gravity.TOP;
        params.verticalMargin = 0.07F;
        params.setTitle("Load Average");

        final View popup = sample.getPopup(this);
        addView(popup);
    }

    public void hide(boolean cancelled) {
        if (cancelled) {
            currentSample.cancel();
            Sample.showFinalToast(this);
            if (currentSample.getType() == Sample.AFTER) {
                sendEvent(this, currentSample.getCategory(), currentSample.getPromptType(), currentSample.getEvent());
            }
        }
        if (myLayout != null) {
            wm.removeView(myLayout);
        }
        mHomeWatcher.stopWatch();
    }

    private int getRandom15and120() {
        Random rand = new Random();
        return (rand.nextInt(106) + 15) * 1000;
    }

    public void removeView(View v) {
        this.hide(false);
    }

    public void addView(View v) {
        // Add your original view to the new empty layout:
        myLayout = new MyLayout(this);
        myLayout.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        wm.addView(myLayout, params);
        mHomeWatcher.startWatch();
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

    public static String getLocalDateTime() {
        DateFormat df = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z");
        String date = df.format(Calendar.getInstance().getTime());
        return date;
    }

    public void sendEvent(Service service, String category, String action, Event event) {

        if (mTracker == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service.getBaseContext());
        String code = prefs.getString(service.getString(R.string.invite_code), "NA");
        UUID deviceID = UUID.fromString(prefs.getString(getString(R.string.device_id), new UUID(0L, 0L).toString()));

        PackageInfo pInfo = null;
        try {
            pInfo = service.getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("c", "error: couldn't get package info");
        }
        int verCode = pInfo.versionCode;
        event.setVersionCode(verCode);

        event.setPID(code);
        event.setDeviceId(deviceID);

        if (!category.equals("non-sample")) {
            if (category.equals("after")) {
                event.setDurationTotal(event.getDurationBefore());
            }
            else {
                long durationBefore = (event.getDurationBefore() >= 0) ? event.getDurationBefore() : 0;
                long durationAfter = (event.getDurationAfter() >= 0) ? event.getDurationAfter() : 0;
                long sum = durationAfter + durationBefore;
                event.setDurationTotal(sum);
            }
        }
        Log.d("c", event.toString());
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(event.toString())
                .build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("C", "STARTING SERVICE");
        startForeground(FOREGROUND_ID, buildForegroundNotification());
        return START_STICKY;
    }

    private Notification buildForegroundNotification() {
        NotificationCompat.Builder b=new NotificationCompat.Builder(this);

        b.setOngoing(true)
                .setContentTitle("Study is running")
                .setContentText("RightNow - UW study")
                .setSmallIcon(R.drawable.ic_action_tag_faces)
                .setTicker("active");

        return(b.build());
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("lastSampleType", lastSampleType);
        edit.apply();
        edit.putString("pastSample1", pastSamples.get(0));
        edit.apply();
        edit.putString("pastSample2", pastSamples.get(1));
        edit.apply();
        edit.putString("pastSample3", pastSamples.get(2));
        edit.apply();

        Log.d("c", "service destroyed");
        super.onDestroy();
        instance = null;
        stopForeground(true);

        Intent broadcastIntent = new Intent("RestartSensor");
        sendBroadcast(broadcastIntent);
    }
}
