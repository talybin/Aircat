package com.talybin.aircat;

import android.content.Context;

import java.nio.file.Paths;

public class Job {

    public enum State {
        NOT_RUNNING,
        RUNNING,
    }

    public String apMac = null;
    public String clientMac = null;
    public String ssid = null;
    public String pmkId = null;
    private String wordListPath = null;
    public State state = State.NOT_RUNNING;

    public Job(ApInfo apInfo) {
        apMac = apInfo.bssid;
        clientMac = apInfo.clientMac;
        ssid = apInfo.ssid;
        pmkId = apInfo.pmkId;
    }

    // Constructor for testing
    public Job() { }

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

    public String getState() {
        Context ctx = MainActivity.getContext();
        switch (state) {
            case NOT_RUNNING:
                return ctx.getString(R.string.not_running);
            case RUNNING:
                return ctx.getString(R.string.running);
        }
        // Should never reach here
        return ctx.getString(android.R.string.unknownName);
    }
}
