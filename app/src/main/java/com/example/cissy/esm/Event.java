package com.example.cissy.esm;


import java.util.UUID;

/**
 * Created by Cissy on 4/30/2017.
 */

public class Event {
    private int type;
    private String PID;
    private UUID deviceId;
    private long timeStamp;
    private String app;
    private String category;
    private String timing;
    private long durationBefore;
    private long sampleDuration;
    private long durationAfter;
    private long durationTotal;
    private String valence;
    private String arousal;
    private String affectText;
    private String purpose;
    private String closeness;
    private String meaningfulness;
    private String meaningfulnessText;

    private Event(EventBuilder builder) {
        this.type = builder.type;
        this.PID = builder.PID;
        this.deviceId = builder.deviceId;
        this.timeStamp = builder.timeStamp;
        this.app = builder.app;
        this.category = builder.category;
        this.timing = builder.timing;
        this.durationBefore = builder.durationBefore;
        this.sampleDuration = builder.sampleDuration;
        this.durationAfter = builder.durationAfter;
        this.durationTotal = builder.durationTotal;
        this.valence = builder.valence;
        this.arousal = builder.arousal;
        this.affectText = builder.affectText;
        this.purpose = builder.purpose;
        this.closeness = builder.closeness;
        this.meaningfulness = builder.meaningfulness;
        this.meaningfulnessText = builder.meaningfulnessText;
    }

    public void setPID(String PID) {
        this.PID = PID;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        String result = "";

        result += "type=" + ((this.type == -1) ? "NA" : this.type);
        result += ";PID=" + ((this.PID == null) ? "NA" : this.PID);
        result += ";device_ID=" + ((this.deviceId == null) ? "NA" : this.deviceId);
        result += ";timestamp=" + ((this.timeStamp == -1) ? "NA" : this.timeStamp);
        result += ";app=" + ((this.app == null) ? "NA" : this.app);
        result += ";category=" + ((this.category == null) ? "NA" : this.category);
        result += ";timing=" + ((this.timing == null) ? "NA" : this.timing);
        result += ";duration_before=" + ((this.durationBefore == -1) ? "NA" : this.durationBefore);
        result += ";sample_duration=" + ((this.sampleDuration == -1) ? "NA" : this.sampleDuration);
        result += ";duration_after=" + ((this.durationAfter == -1) ? "NA" : this.durationAfter);
        result += ";duration_total=" + ((this.durationTotal == -1) ? "NA" : this.durationTotal);
        result += ";valence=" + ((this.valence == null) ? "NA" : this.valence);
        result += ";arousal=" + ((this.arousal == null) ? "NA" : this.arousal);
        result += ";affect_text=" + ((this.affectText == null) ? "NA" : this.affectText);
        result += ";purpose=" + ((this.purpose == null) ? "NA" : this.purpose);
        result += ";closness=" + ((this.closeness == null) ? "NA" : this.closeness);
        result += ";meaningfulness=" + ((this.meaningfulness == null) ? "NA" : this.meaningfulness);
        result += ";meaningfulness_text=" + ((this.meaningfulnessText == null) ? "NA" : this.meaningfulnessText);

        result += ";";

        return result;
    }

    public static class EventBuilder {
        protected int type;
        protected String PID;
        protected UUID deviceId;
        protected long timeStamp;
        protected String app;
        protected String category;
        protected String timing;
        protected long durationBefore = -1;
        protected long sampleDuration = -1;
        protected long durationAfter = -1;
        protected long durationTotal = -1;
        protected String valence;
        protected String arousal;
        protected String affectText;
        protected String purpose;
        protected String closeness;
        protected String meaningfulness;
        protected String meaningfulnessText;

        public EventBuilder(int type, long timeStamp, String app) {
            this.type = type;
            this.timeStamp = timeStamp;
            this.app = app;
        }

        public EventBuilder category(String category) {
            this.category = category;
            return this;
        }

        public EventBuilder timing(String timing) {
            this.timing = timing;
            return this;
        }

        public EventBuilder durationBefore(long durationBefore) {
            this.durationBefore = durationBefore;
            return this;
        }

        public EventBuilder sampleDuration(long sampleDuration) {
            this.sampleDuration = sampleDuration;
            return this;
        }

        public EventBuilder durationAfter(long durationAfter) {
            this.durationAfter = durationAfter;
            return this;
        }

        public EventBuilder durationTotal(long durationTotal) {
            this.durationTotal = durationTotal;
            return this;
        }

        public EventBuilder valence(String valence) {
            this.valence = valence;
            return this;
        }

        public EventBuilder arousal(String arousal) {
            this.arousal = arousal;
            return this;
        }

        public EventBuilder affectText(String affectText) {
            this.affectText = affectText;
            return this;
        }

        public EventBuilder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        public EventBuilder closeness(String closeness) {
            this.closeness = closeness;
            return this;
        }

        public EventBuilder meaningfulness(String meaningfulness) {
            this.meaningfulness = meaningfulness;
            return this;
        }

        public EventBuilder meaningfulnessText(String meaningfulnessText) {
            this.meaningfulnessText = meaningfulnessText;
            return this;
        }

        public Event build() {
            return new Event(this);
        }
    }
}
