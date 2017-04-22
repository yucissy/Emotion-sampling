package com.example.cissy.esm;

import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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

    public void setPackageName(String name) { this.packageName = name; }

    public String getPackageName() { return this.packageName; }

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
        final LinearLayout popup;

        switch (this.getType()) {
            case BEFORE:
                popup = (LinearLayout) li.inflate(R.layout.affect_before, null);
                break;
            case DURING:
                popup = (LinearLayout) li.inflate(R.layout.affect_during, null);
                break;
            case AFTER:
                popup = (LinearLayout) li.inflate(R.layout.affect_during, null);
                break;
            default:
                popup = null;
        }
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
                    DetectAppsService.sendEvent(service, thisSample.getCategory(), "affect_scale", ";happiness="+idx+";energy="+idx2+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                    service.removeView(popup);

                    Random rand = new Random();
                    int n = rand.nextInt(4);
                    if (n == 0) {
                        final View followUp = thisSample.getFollowUp(service);
                        final EditText answer = (EditText) followUp.findViewById(R.id.editText);

                        service.addView(followUp);
                        Button submit2 = (Button) followUp.findViewById(R.id.btnSubmit);

                        submit2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DetectAppsService.sendEvent(service, thisSample.getCategory(), "affect_text", ";response="+answer.getText()+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                                service.removeView(followUp);
                                if (thisSample.getType() == Sample.AFTER) {
                                    final View followUp = thisSample.getMeaningfulnessLikert(service);
                                    service.addView(followUp);
                                }
                            }
                        });
                    }
                    else if (thisSample.getType() == Sample.AFTER) {
                        final View followUp = thisSample.getMeaningfulnessLikert(service);
                        service.addView(followUp);
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

    public View getMeaningfulnessLikert(final DetectAppsService service) {
        final Sample thisSample = this;
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout popup = (LinearLayout) li.inflate(R.layout.meaningfulness_scale, null);
        TextView prompt = (TextView) popup.findViewById(R.id.textView);

        prompt.setText("How meaningful was using "+service.getSampledApp()+" just now?");

        Button submit = (Button) popup.findViewById(R.id.btnSubmit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup mGroup = (RadioGroup) popup.findViewById(R.id.meaning_group);
                int checkedM = mGroup.getCheckedRadioButtonId();
                View radioButton = mGroup.findViewById(checkedM);
                int idx = mGroup.indexOfChild(radioButton);
                DetectAppsService.sendEvent(service, thisSample.getCategory(), "meaningfulness_scale", ";meaningfulness="+idx+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());

                service.removeView(popup);
                Random rand = new Random();
                int n = rand.nextInt(2);
                if (n == 0) {
                    final View followUp = thisSample.getMeaningfulnessText(service);
                    final EditText answer = (EditText) followUp.findViewById(R.id.editText);

                    service.addView(followUp);
                    Button submit2 = (Button) followUp.findViewById(R.id.btnSubmit);

                    submit2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DetectAppsService.sendEvent(service, thisSample.getCategory(), "meaningfulness_text", ";response="+answer.getText()+";app="+thisSample.getPackageName()+";duration="+thisSample.getSampleTime());
                            service.removeView(followUp);
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
