package com.talybin.aircat;

import android.content.Context;
import android.util.Log;

import java.nio.file.Paths;

public class Job extends ListenerBase<Job.Listener> {

    public enum State {
        NOT_RUNNING,
        STARTING,
        RUNNING,
        STOPPING,
    }

    public interface Listener {
        void onStateChange();
        //void onProgress();
    }

    public String apMac = null;
    public String clientMac = null;
    public String ssid = null;
    public String pmkId = null;

    private State state = State.NOT_RUNNING;
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
        //Context ctx = MainActivity.getContext();
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
                listener.onStateChange();
        }
    }
}
