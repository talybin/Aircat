package com.talybin.aircat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.file.Paths;

public class Job extends ListenerBase<Job.Listener> {

    public enum State {
        NOT_RUNNING,
        STARTING,
        RUNNING,
        STOPPING,
    }

    // HashCat progress
    public static class Progress {
        // State of the job:
        //  3 (running)
        //  5 (exhausted)
        //  6 (cracked)
        //  7 (aborted)
        //  8 (quit)
        public int state = 0;

        // Speed in hashes per second
        public int speed = 0;

        // Number of hashes completed so far
        public long nr_complete = 0;

        // Total number of hashes
        public long total = 0;

        // For debug purpose
        @NonNull
        @SuppressLint("DefaultLocale")
        public String toString() {
            return String.format("state: %d, speed: %d H/s, complete: %d/%d",
                    state, speed, nr_complete, total);
        }
    }

    public interface Listener {
        void onJobStateChange(Job job);
        void onJobProgressChange(Job job);
    }

    private String apMac = null;
    private String clientMac = null;
    private String ssid = null;
    private String pmkId = null;

    private State state = State.NOT_RUNNING;
    private Progress progress = null;
    private String wordListPath = null;

    public Job(ApInfo apInfo) {
        super();

        apMac = apInfo.bssid;
        clientMac = apInfo.clientMac;
        ssid = apInfo.ssid;
        pmkId = apInfo.pmkId;
    }

    // Constructor for testing
    public Job() {
        super();
        apMac = "c4:72:95:64:51:26";
        clientMac = "6c:c7:ec:95:3d:63";
        pmkId = "5265b2887ac349c4096eb7c2e4aaba61";
        ssid = "IterationRentalsWifi";
    }

    public String getApMac() {
        return apMac;
    }

    public String getClientMac() {
        return clientMac;
    }

    public String getSSID() {
        return ssid;
    }

    public Progress getProgress() {
        return progress;
    }

    // This hash used to store and find in the list
    public String getHash() {
        return pmkId;
    }

    public String getWordListPath() {
        return wordListPath != null ? wordListPath : WordLists.getBuiltInPath();
    }

    public String getWordListName() {
        return Paths.get(getWordListPath()).getFileName().toString();
    }

    public String getStateAsStr(Context context) {
        switch (state) {
            case NOT_RUNNING:
                return context.getString(R.string.not_running);
            case STARTING:
                return context.getString(R.string.starting);
            case RUNNING:
                return context.getString(R.string.running);
            case STOPPING:
                return context.getString(R.string.stopping);
        }
        // Should never reach here
        return context.getString(android.R.string.unknownName);
    }

    public State getState() {
        return state;
    }

    public void setState(State newState) {
        Log.d("Job", "Setting new state: " + newState);
        if (state != newState) {
            state = newState;
            for (Listener listener : listeners)
                listener.onJobStateChange(this);
        }
    }

    public void setProgress(Progress newProgress) {
        progress = newProgress;
        for (Listener listener : listeners)
            listener.onJobProgressChange(this);
    }
}
