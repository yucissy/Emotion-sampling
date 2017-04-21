package com.example.cissy.esm;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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

    private View mPopup;
    private RecentUseComparator mRecentComp;
    private static final List<String> targetApps = Arrays.asList("com.facebook.katana","com.whatsapp",
            "com.snapchat.android","com.facebook.orca","com.instagram.android","com.twitter.android",
            "com.netflix.mediaclient","com.google.android.youtube","com.king.candycrushsaga", "com.zynga.words",
            "com.nintendo.zara", "com.google.android.gm","com.microsoft.office.outlook", "me.bluemail.mail",
            "com.google.android.apps.inbox", "com.android.email");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onCreate() {
        super.onCreate();
        mRecentComp = new RecentUseComparator();

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
                        (System.currentTimeMillis() - lastSampledTime > 30000))) {
                    final Sample sample = Sample.getRandomSample(lastSample);
                    Log.d("c", "sampled: "+ sample.getType());
                    lastSample = sample;

                    if (sample.getType() == Sample.BEFORE) {
                        lastSampledTime = System.currentTimeMillis();
                        popupSample(sample);
                    }
                    else if (sample.getType() == Sample.DURING) {
                        lastSampledTime = System.currentTimeMillis();
                        popupSample(sample);
                    }
                    else {
                        sleep(5000);
                        lastSampledTime = System.currentTimeMillis();
                        popupSample(sample);
                    }
                }
                lastApp = packageName;

                h.postDelayed(this, delay);
            }
        }, delay);


    }

    private void sleep(int length) {
        try {
            Thread.sleep(length);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void popupSample(final Sample sample) {
        final WindowManager wm = getWindowManager();
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                0,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.setTitle("Load Average");

        final View popup = sample.getPopup(getService());
        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup vGroup = (RadioGroup) popup.findViewById(R.id.valence_group);
                int checkedV = vGroup.getCheckedRadioButtonId();
                View radioButton = vGroup.findViewById(checkedV);
                int idx = vGroup.indexOfChild(radioButton);

                RadioGroup eGroup = (RadioGroup) popup.findViewById(R.id.energy_group);
                int checkedE = eGroup.getCheckedRadioButtonId();
                View radioButton2 = eGroup.findViewById(checkedE);
                int idx2 = eGroup.indexOfChild(radioButton2);

                System.out.println("e group is "+idx2+" and v group is "+idx);

                if (idx >= 0 && idx2 >= 0) {
                    wm.removeView(popup);

                    Random rand = new Random();
                    int n = rand.nextInt(4);
                    if (n == 0) {
                        final View followUp = sample.getFollowUp(getService());
                        final EditText answer = (EditText) followUp.findViewById(R.id.editText);

                        wm.addView(followUp, params);
                        Button submit2 = (Button) followUp.findViewById(R.id.btnSubmit);

                        submit2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                System.out.println(answer.getText());
                                wm.removeView(followUp);
                            }
                        });
                    }
                }
            }
        });

        wm.addView(popup, params);
    }

    private int getRandom1and120() {
        Random rand = new Random();
        return rand.nextInt(120) + 1;
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
}
