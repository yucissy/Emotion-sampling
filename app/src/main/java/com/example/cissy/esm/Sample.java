package com.example.cissy.esm;

import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
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

    public Sample(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

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

    public static Sample getRandomSample(Sample lastSample) {

        List<Integer> sampleTypes = Arrays.asList(Sample.BEFORE, Sample.DURING, Sample.AFTER);
        int lastSampleType = 3;
        if (lastSample != null) {
            lastSampleType = lastSample.getType();
        }

        int selectedType = lastSampleType;
        while (selectedType == lastSampleType) {
            //Choose a sample type randomly
            Random randomizer = new Random();
            selectedType = sampleTypes.get(randomizer.nextInt(sampleTypes.size()));
        }

        return new Sample(selectedType);
    }

    public View getPopup(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li = (LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.affect_before, null);
        TextView prompt = (TextView) popup.findViewById(R.id.textView);
        final SpannableStringBuilder str;

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
                    Event event = new Event.EventBuilder(1, System.currentTimeMillis(), thisSample.getPackageName())
                            .valence(Integer.toString(idx))
                            .arousal(Integer.toString(idx2))
                            .durationBefore(thisSample.getSampleTime())
                            .build();
                    //DetectAppsService.sendEvent(service, thisSample.getCategory(), "affect_scale", event);
                    service.removeView(popup);

                    Random rand = new Random();
                    int n = rand.nextInt(4);
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
                                if (response.length() >= 75) {
                                    //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";response="+answer.getText()+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                                    service.removeView(followUp);
                                    if (thisSample.getType() == Sample.AFTER) {
                                        final View followUp = thisSample.getMeaningfulnessLikert(service);
                                        service.addView(followUp);
                                        thisSample.setPromptType("meaningfulness_scale");
                                    }
                                    else {
                                        final View followUp = thisSample.getPurpose(service);
                                        service.addView(followUp);
                                        thisSample.setPromptType("purpose_mc");
                                    }
                                }
                            }
                        });
                    }
                    else if (thisSample.getType() == Sample.AFTER) {
                        final View followUp = thisSample.getMeaningfulnessLikert(service);
                        service.addView(followUp);
                        thisSample.setPromptType("meaningfulness_scale");
                    }
                    else {
                        final View followUp = thisSample.getPurpose(service);
                        service.addView(followUp);
                        thisSample.setPromptType("purpose_mc");
                    }
                }
            }
        });
        return popup;
    }



    public View getFollowUp(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.affect_text, null);
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
        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.purpose_group);
                int checkedM = mGroup.getCheckedRadioButtonId();
                RadioButton radioButton = (RadioButton) mGroup.findViewById(checkedM);
                String purpose = radioButton.getText().toString();

                //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";purpose="+purpose+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                service.removeView(popup);

                if (purpose.equals("Communicating / interacting")) {
                    final View closenessFollowUp = thisSample.getCloseness(service);
                    service.addView(closenessFollowUp);
                    thisSample.setPromptType("closeness_scale");
                }
            }
        });
        return popup;
    }

    public View getCloseness(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.closeness_scale, null);
        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.closeness_group);
                int checkedM = mGroup.getCheckedRadioButtonId();
                RadioButton radioButton = (RadioButton) mGroup.findViewById(checkedM);
                int idx = mGroup.indexOfChild(radioButton);
                //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";closeness="+idx+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                service.removeView(popup);
            }
        });
        return popup;
    }

    public View getMeaningfulnessLikert(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.meaningfulness_scale, null);
        TextView prompt = (TextView) popup.findViewById(R.id.textView);

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

                service.removeView(popup);
                Random rand = new Random();
                int n = rand.nextInt(2);
                if (n == 0) {
                    final View followUp = thisSample.getMeaningfulnessText(service);
                    final EditText answer = (EditText) followUp.findViewById(R.id.editText);

                    service.addView(followUp);
                    thisSample.setPromptType("meaningfulness_text");
                    Button submit2 = (Button) followUp.findViewById(R.id.btnSubmit);

                    submit2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String response = answer.getText().toString();
                            if (response.length() >= 75) {
                                //DetectAppsService.sendEvent(service, thisSample.getCategory(), thisSample.getPromptType(), ";response="+answer.getText()+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                                service.removeView(followUp);
                                final View followUp = thisSample.getPurpose(service);
                                service.addView(followUp);
                                thisSample.setPromptType("purpose_mc");
                            }
                        }
                    });
                }
            }
        });
        return popup;
    }

    public View getMeaningfulnessText(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.meaningfulness_text, null);
        return popup;
    }

    public void closePopup() {

    }
}
