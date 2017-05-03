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
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static java.security.AccessController.getContext;

/**
 * Created by Cissy on 4/17/2017.
 */

public class DetectAppsService extends Service {

    private static final List<String> targetApps = Arrays.asList("com.facebook.katana", "com.whatsapp",
            "com.snapchat.android", "com.facebook.orca", "com.instagram.android", "com.google.android.gm",
            "com.microsoft.office.outlook", "me.bluemail.mail", "com.google.android.apps.inbox", "com.android.email",
            "com.tinder", "com.linkedin.android", "com.ftw_and_co.happn","com.yik.yak", "com.okcupid.okcupid", "com.bumble.app", "com.android.chrome",
            "com.google.android.googlequicksearchbox", "com.cnn.mobile.android.phone", "com.twitter.android",
            "com.google.android.apps.genie.geniewidget","com.google.android.apps.magazines",
            "com.foxnews.android", "com.netflix.mediaclient","com.google.android.youtube",
            "com.king.candycrushsaga", "com.zynga.words", "com.nintendo.zara", "com.reddit.frontpage", "com.amazon.avod.thirdpartyclient",
            "com.directv.dvrscheduler", "tv.twitch.android.app", "com.hulu.plus", "com.imgur.mobile");
    private static final List<String> familyFriendsApps = Arrays.asList("com.facebook.katana", "com.whatsapp",
            "com.snapchat.android", "com.facebook.orca", "com.instagram.android", "com.google.android.gm",
            "com.microsoft.office.outlook", "me.bluemail.mail", "com.google.android.apps.inbox", "com.android.email");
    private static final List<String> meetingPeopleApps = Arrays.asList("com.tinder", "com.linkedin.android",
            "com.ftw_and_co.happn","com.yik.yak", "com.okcupid.okcupid", "com.bumble.app");
    private static final List<String> informationSeekingApps = Arrays.asList("com.android.chrome",
            "com.google.android.googlequicksearchbox", "com.cnn.mobile.android.phone", "com.twitter.android",
            "com.google.android.apps.genie.geniewidget","com.google.android.apps.magazines",
            "com.foxnews.android");
    private static final List<String> entertainmentApps = Arrays.asList("com.netflix.mediaclient","com.google.android.youtube",
            "com.king.candycrushsaga", "com.zynga.words", "com.nintendo.zara", "com.reddit.frontpage", "com.amazon.avod.thirdpartyclient",
            "com.directv.dvrscheduler", "tv.twitch.android.app", "com.hulu.plus", "com.imgur.mobile");
    private static final List<String> launcherApps = new ArrayList();


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

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onCreate() {
        super.onCreate();

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

        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
        for (ResolveInfo resolveInfo : lst) {
            launcherApps.add(resolveInfo.activityInfo.packageName.toString());
        }

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        final Handler h = new Handler();
        final int delay = 1000; //milliseconds

        h.postDelayed(new Runnable(){
            String lastApp = null;
            Sample lastSample = null;
            long lastSampledTime = 0;
            String lastAppCategory;
            long launchTime = System.currentTimeMillis();
            boolean sampled = false;
            Sample sample;

            public void run(){
                AppChecker appChecker = new AppChecker();
                String packageName = appChecker.getForegroundApp(getApplicationContext());

                //TODO: Change these back after testing
                //If user just opened a target app
                if (!packageName.equals(lastApp)) {
                    if (targetApps.contains(packageName)) {
                        //System.currentTimeMillis() - lastSampledTime > 1800000
                        //(getAppCategory(packageName) != lastAppCategory)

                        lastAppCategory = getAppCategory(packageName);

                        sample = Sample.getRandomSample(lastSample, packageName);
                        currentSample = sample;
                        lastSample = sample;
                        sampledApp = packageName;
                        sample.setPackageName(packageName);
                        Log.d("c", "sample "+currentSample.getType());

                        if (sample.getType() == Sample.BEFORE) {
                            lastSampledTime = System.currentTimeMillis();
                            sampled = true;
                            popupSample(sample);
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
                                lastSampledTime = System.currentTimeMillis();
                                sample.setSampleTime(waitPeriod);
                                sampled = true;
                                popupSample(sample);
                            }
                            else if (sample.getType() == Sample.AFTER && continuousUse) {
                                int elapsed = 0;
                                sampled = true;
                                while (appChecker.getForegroundApp(getApplicationContext()).equals(packageName)) {
                                    sleep(1000);
                                    elapsed+=1000;
                                }
                                lastSampledTime = System.currentTimeMillis();
                                sample.setSampleTime(elapsed+15000);
                                sample.setAppEnd(elapsed);
                                popupSample(sample);
                            }
                        }
                    }


                    long duration = System.currentTimeMillis() - launchTime;
                    launchTime = System.currentTimeMillis();
                    if (!launcherApps.contains(lastApp)) {
                        if (sampled && sample.getType() != Sample.AFTER) {
                            Log.d("c", "a sample app just closed");
                            Event event = sample.getEvent();
                            event.setDurationAfter(System.currentTimeMillis() - sample.getEndTime());
                            sendEvent(thisService, sample.getCategory(), sample.getPromptType(), event);
                            sampled = false;
                            sample = null;
                        }
                        else if (!sampled) {
                            Event event = new Event.EventBuilder(0, launchTime, DetectAppsService.getLocalDateTime(), lastApp)
                                    .durationTotal(duration)
                                    .build();
                            sendEvent(getService(), "non-sample", "non-sample", event);
                            Log.d("c", "a normal app just closed");
                            sampled = false;
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
            if (currentSample.getType() == Sample.AFTER) {
                sendEvent(this, currentSample.getCategory(), currentSample.getPromptType(), currentSample.getEvent());
            }
            //sendEvent(this, currentSample.getCategory(), currentSample.getPromptType(), currentSample.getEvent());
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
        UUID deviceID = UUID.fromString(prefs.getString(getString(R.string.device_id), "NA"));

        event.setPID(code);
        event.setDeviceId(deviceID);

        if (!category.equals("non-sample")) {
            if (category.equals("after")) {
                event.setDurationTotal(event.getDurationBefore());
            }
            else {
                System.out.println("duration before: "+event.getDurationBefore());
                System.out.println("duration after: "+event.getDurationAfter());
                long sum = event.getDurationAfter() + event.getDurationBefore();
                event.setDurationTotal(sum);
            }
        }
        System.out.println(event.toString());
//        mTracker.send(new HitBuilders.EventBuilder()
//                .setCategory(category)
//                .setAction(action)
//                .setLabel(event.toString())
//                .build());
    }

    public static String getAppCategory(String packageName) {
        if (familyFriendsApps.contains(packageName)) {
            return "family-friends";
        }
        else if (meetingPeopleApps.contains(packageName)) {
            return "meeting-people";
        }
        else if (informationSeekingApps.contains(packageName)) {
            return "info";
        }
        else if (entertainmentApps.contains(packageName)){
            return "entertainment";
        }
        else {
            return "unknown";
        }
    }
}
