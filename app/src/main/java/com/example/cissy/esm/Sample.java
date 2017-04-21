package com.example.cissy.esm;

import android.app.Service;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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

    public Sample(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

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

    public View getPopup(Service service) {
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout popup;

        switch(this.getType()) {
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

        return popup;
    }

    public View getFollowUp(Service service) {
        LayoutInflater li=(LayoutInflater) service.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout popup = (LinearLayout) li.inflate(R.layout.affect_text, null);
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

    public void closePopup() {

    }
}
