package com.uw.hcde.esm;

import android.app.Service;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Cissy on 4/18/2017.
 */

public class Sample {

    public static final int BEFORE = 0, DURING = 1, AFTER = 2;
    private int type;
    private int sampleTime = 0;
    private String packageName;
    private String promptType;
    private long sampleStartTime;
    private long sampleEndTime;
    private final Event event;

    public Sample(int type, String appName) {
        this.type = type;
        this.event = new Event.EventBuilder(1, System.currentTimeMillis(), DetectAppsService.getLocalDateTime(), appName)
                .timing(this.getCategory())
                .build();
    }

    public int getType() {
        return this.type;
    }

    public Event getEvent() { return this.event; }

    public long getEndTime() { return this.sampleEndTime; }

    public String getCategory() {
        int type = this.getType();
        switch (type) {
            case BEFORE:
                return "before";
            case DURING:
                return "during";
            case AFTER:
                return "after";
            default:
                return null;
        }
    }

    public void setPromptType(String type) { this.promptType = type; }

    public void setPackageName(String name) { this.packageName = name; }

    public String getPackageName() { return this.packageName; }

    public String getPromptType() { return this.promptType; }

    public void setSampleTime(int time) { this.sampleTime = time; }

    public int getSampleTime() { return this.sampleTime; }

    public static String getAppNameFromPackageName(Service service, String packageName) {
        final PackageManager pm = service.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo( packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }

    public static Sample getRandomSample(int lastSample, String appName) {

        List<Integer> sampleTypes = Arrays.asList(Sample.BEFORE, Sample.DURING, Sample.AFTER);
        int lastSampleType = 3;
        if (lastSample != -1) {
            lastSampleType = lastSample;
        }

        int selectedType = lastSampleType;
        while (selectedType == lastSampleType) {
            //Choose a sample type randomly
            Random randomizer = new Random();
            selectedType = sampleTypes.get(randomizer.nextInt(sampleTypes.size()));
        }

        return new Sample(selectedType, appName);
    }

    public View getPopup(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.affect_before, null);

        TextView appNameBox = (TextView) popup.findViewById(R.id.appNameBox);
        appNameBox.setText("App Name: " + getAppNameFromPackageName(service, this.getPackageName()));

        TextView prompt = (TextView) popup.findViewById(R.id.textView);
        final SpannableStringBuilder str;
        this.sampleStartTime = System.currentTimeMillis();

        switch (this.getType()) {
            case BEFORE:
                str = new SpannableStringBuilder("How were you feeling right before launching this app?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 20, 33, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            case DURING:
                str = new SpannableStringBuilder("How are you feeling right now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 20, 28, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            case AFTER:
                str = new SpannableStringBuilder("How are you feeling right now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 20, 28, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            default:
                prompt.setText(null);
        }
        this.setPromptType("affect_scale");

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

                if (idx >= 0 && idx2 >= 0) {
//                    event = new Event.EventBuilder(1, System.currentTimeMillis(), thisSample.getPackageName())
//                            .valence(Integer.toString(idx))
//                            .arousal(Integer.toString(idx2))
//                            .category(DetectAppsService.getAppCategory(thisSample.getPackageName()))
//                            .timing(thisSample.getCategory())
//                            .durationBefore(thisSample.getSampleTime())
//                            .build();
                    event.setValence(Integer.toString(idx));
                    event.setArousal(Integer.toString(idx2));
                    event.setDurationBefore(thisSample.getSampleTime());
                    //DetectAppsService.sendEvent(service, thisSample.getCategory(), "affect_scale", event);
                    service.removeView(popup);

                    Random rand = new Random();
                    int n = rand.nextInt(10);
                    if (n == 0) {
                        final View followUp = thisSample.getFollowUp(service);
                        final EditText answer = (EditText) followUp.findViewById(R.id.editText);

                        service.addView(followUp);
                        thisSample.setPromptType("affect_text");
                        Button submit2 = (Button) followUp.findViewById(R.id.btnSubmit);

                        submit2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String response = answer.getText().toString();
                                event.setAffectText(response);
                                if (response.length() >= 25) {
                                    //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";response="+answer.getText()+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                                    service.removeView(followUp);
                                    final View followUp = thisSample.getUGPrompt(service);
                                    service.addView(followUp);
                                    thisSample.setPromptType("ug_mc");
                                }
                            }
                        });

                        answer.addTextChangedListener ( new TextWatcher() {

                            public void afterTextChanged ( Editable s ) {

                            }

                            public void beforeTextChanged ( CharSequence s, int start, int count, int after ) {

                            }

                            public void onTextChanged ( CharSequence s, int start, int before, int count ) {
                                Log.d("c", "touch");
                                service.restartTimer();
                            }
                        });
                    }
                    else {
                        final View followUp = thisSample.getUGPrompt(service);
                        service.addView(followUp);
                        thisSample.setPromptType("ug_mc");
                    }
                }
            }
        });
        return popup;
    }

    public View getUGPrompt(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.ug_mc, null);

        TextView appNameBox = (TextView) popup.findViewById(R.id.appNameBox);
        appNameBox.setText("App Name: " + getAppNameFromPackageName(service, this.getPackageName()));

        TextView prompt = (TextView) popup.findViewById(R.id.textView);
        final SpannableStringBuilder str;

        switch (this.getType()) {
            case BEFORE:
                str = new SpannableStringBuilder("Which of the following best describes the way you are using your phone right now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 71, 79, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            case DURING:
                str = new SpannableStringBuilder("Which of the following best describes the way you are using your phone right now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 71, 79, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            case AFTER:
                str = new SpannableStringBuilder("Which of the following best describes the way you used your phone just now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 66, 73, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            default:
                prompt.setText(null);
        }


        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.purpose_group);
                int checkedM = mGroup.getCheckedRadioButtonId();

                if (checkedM >= 0) {
                    RadioButton radioButton = (RadioButton) mGroup.findViewById(checkedM);
                    String purpose = radioButton.getText().toString();

                    service.removeView(popup);
                    event.setUgPurpose(purpose);

                    final View followUp = thisSample.getPurpose(service);
                    service.addView(followUp);
                    thisSample.setPromptType("purpose_mc");
                }
            }
        });
        return popup;
    }

    public View getFollowUp(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.affect_text, null);

        TextView appNameBox = (TextView) popup.findViewById(R.id.appNameBox);
        appNameBox.setText("App Name: " + getAppNameFromPackageName(service, this.getPackageName()));

        TextView prompt = (TextView) popup.findViewById(R.id.textView);

        switch(this.getType()) {
            case BEFORE:
                prompt.setText("Why do you think you were feeling this way?");
                break;
            case DURING:
                prompt.setText("Why do you think you are feeling this way?");
                break;
            case AFTER:
                prompt.setText("Why do you think you are feeling this way?");
                break;
            default:
                break;
        }
        return popup;
    }

    public View getPurpose(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.purpose_mc, null);

        TextView appNameBox = (TextView) popup.findViewById(R.id.appNameBox);
        appNameBox.setText("App Name: " + getAppNameFromPackageName(service, this.getPackageName()));

        TextView prompt = (TextView) popup.findViewById(R.id.textView);
        final SpannableStringBuilder str;

        switch (this.getType()) {
            case BEFORE:
                str = new SpannableStringBuilder("What best describes how you're using this app right now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 45, 54, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            case DURING:
                str = new SpannableStringBuilder("What best describes how you're using this app right now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 45, 54, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            case AFTER:
                str = new SpannableStringBuilder("What best describes how you used this app just now?");
                str.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 41, 49, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                prompt.setText(str);
                break;
            default:
                prompt.setText(null);
        }

        SpannableStringBuilder mc;
        RadioButton communicatingBox = (RadioButton) popup.findViewById(R.id.radio0);
        mc = new SpannableStringBuilder("Communicating/interacting with other people");
        mc.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 26, 30, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        communicatingBox.setText(mc);

        RadioButton browsingBox = (RadioButton) popup.findViewById(R.id.radio1);
        mc = new SpannableStringBuilder("Browsing social media without interacting with other people");
        mc.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 22, 29, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        browsingBox.setText(mc);




        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.purpose_group);
                int checkedM = mGroup.getCheckedRadioButtonId();

                if (checkedM >= 0) {
                    RadioButton radioButton = (RadioButton) mGroup.findViewById(checkedM);
                    String purpose = radioButton.getText().toString();

                    //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";purpose="+purpose+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                    service.removeView(popup);
                    event.setPurpose(purpose);

                    if (thisSample.getType() == Sample.AFTER) {
                        final View meaningfulnessFollowUp = thisSample.getMeaningfulnessLikert(service);
                        service.addView(meaningfulnessFollowUp);
                        thisSample.setPromptType("meaningfulness_scale");
                    }
                    else {
                        sampleEndTime = System.currentTimeMillis();
                        event.setSampleDuration(sampleEndTime - sampleStartTime);
                        endPrompts(service);
                    }
                }
            }
        });
        return popup;
    }
//
//    public View getCloseness(final DetectAppsService service) {
//        final Sample thisSample = this;
//        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.closeness_scale, null);
//        Button submit = (Button) popup.findViewById(R.id.btnSubmit);
//
//        submit.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.closeness_group);
//                int checkedM = mGroup.getCheckedRadioButtonId();
//
//                if (checkedM >= 0) {
//                    RadioButton radioButton = (RadioButton) mGroup.findViewById(checkedM);
//                    int idx = mGroup.indexOfChild(radioButton);
//                    //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";closeness="+idx+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
//                    event.setCloseness(Integer.toString(idx));
//                    service.removeView(popup);
//                    if (thisSample.getType() == Sample.AFTER) {
//                        final View meaningfulnessFollowUp = thisSample.getMeaningfulnessLikert(service);
//                        service.addView(meaningfulnessFollowUp);
//                        thisSample.setPromptType("meaningfulness_scale");
//                    }
//                    else {
//                        sampleEndTime = System.currentTimeMillis();
//                        event.setSampleDuration(sampleEndTime - sampleStartTime);
//                        endPrompts(service);
//                    }
//                }
//            }
//        });
//        return popup;
//    }

    public View getMeaningfulnessLikert(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.meaningfulness_scale, null);
        TextView prompt = (TextView) popup.findViewById(R.id.textView);

        TextView appNameBox = (TextView) popup.findViewById(R.id.appNameBox);
        appNameBox.setText("App Name: " + getAppNameFromPackageName(service, this.getPackageName()));

        prompt.setText("You just used "+service.getSampledApp()+".\n\nHow much do you feel like you have spent your time on something meaningful?");

        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.meaning_group);
                int checkedM = mGroup.getCheckedRadioButtonId();
                View radioButton = mGroup.findViewById(checkedM);
                int idx = mGroup.indexOfChild(radioButton);
                //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";meaningfulness="+idx+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                event.setMeaningfulness(Integer.toString(idx));

                service.removeView(popup);
                Random rand = new Random();
                int n = rand.nextInt(100);
                if (n <= 35) {
                    final View followUp = thisSample.getMeaningfulnessText(service);
                    final EditText answer = (EditText) followUp.findViewById(R.id.editText);

                    service.addView(followUp);
                    thisSample.setPromptType("meaningfulness_text");
                    Button submit2 = (Button) followUp.findViewById(R.id.btnSubmit);

                    submit2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String response = answer.getText().toString();
                            if (response.length() >= 25) {
                                //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";response="+answer.getText()+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                                service.removeView(followUp);
                                event.setMeaningfulnessText(response);
                                sampleEndTime = System.currentTimeMillis();
                                event.setSampleDuration(sampleEndTime - sampleStartTime);
                                endPrompts(service);
                            }
                        }
                    });

                    answer.addTextChangedListener ( new TextWatcher() {

                        public void afterTextChanged ( Editable s ) {

                        }

                        public void beforeTextChanged ( CharSequence s, int start, int count, int after ) {

                        }

                        public void onTextChanged ( CharSequence s, int start, int before, int count ) {
                            Log.d("c", "touch");
                            service.restartTimer();
                        }
                    });
                }
                else {
                    sampleEndTime = System.currentTimeMillis();
                    event.setSampleDuration(sampleEndTime - sampleStartTime);
                    endPrompts(service);
                }
            }
        });
        return popup;
    }

    public View getMeaningfulnessText(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.meaningfulness_text, null);

        TextView appNameBox = (TextView) popup.findViewById(R.id.appNameBox);
        appNameBox.setText("App Name: " + getAppNameFromPackageName(service, this.getPackageName()));

        return popup;
    }

    private void endPrompts(DetectAppsService service) {
        this.showFinalToast(service);
        //if (this.getType() == AFTER) {
        service.sendEvent(service, this.getCategory(), this.getPromptType(), this.event);
        //}
    }

    public void setAppEnd(long duration) {
        if (this.getType() == Sample.AFTER) {
            event.setDurationTotal(duration);
        }
    }

    public static void showFinalToast(Service service) {
        CharSequence text = "Your response was recorded";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(service, text, duration);
        toast.show();
    }

    public void cancel() {
        String none = "NO_RESPONSE";
        sampleEndTime = System.currentTimeMillis();
        event.setSampleDuration(sampleEndTime - sampleStartTime);
        switch (this.getPromptType()) {
            case "affect_scale":
                event.setValence(none);
                event.setArousal(none);
                break;
            case "affect_text":
                event.setAffectText(none);
                break;
            case "ug_mc":
                event.setUgPurpose(none);
                break;
            case "purpose_mc":
                event.setPurpose(none);
                break;
            case "meaningfulness_scale":
                event.setMeaningfulness(none);
                break;
            case "meaningfulness_text":
                event.setMeaningfulnessText(none);
                break;
            default:
                break;
        }
    }
}
