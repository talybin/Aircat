package com.talybin.aircat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;

public class Job extends ListenerBase<Job.Listener> {

    public enum State {
        NOT_RUNNING,
        STARTING,
        RUNNING,
        STOPPING,
    }

    public interface Listener {
        void onJobStateChange(Job job);
        void onHashCatProgressChange(Job job, HashCat.Progress progress);
    }

    private String apMac = null;
    private String clientMac = null;
    private String ssid = null;
    private String pmkId = null;
    private String password = null;

    private State state = State.NOT_RUNNING;
    private Uri wordList = null;

    private HashCat hashCat = null;

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
        pmkId = "5265b2887ac349c4096eb7c2e4aaba60";
        //pmkId = "5265b2887ac349c4096eb7c2e4aaba61";
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

    // This hash used to store and find in the list
    public String getHash() {
        return pmkId;
    }

    public Uri getWordList() {
        if (wordList == null)
            wordList = Uri.fromFile(new File(WordLists.getBuiltInPath()));
        return wordList;
    }

    public String getWordListAsString() {
        return Uri.decode(getWordList().toString());
    }

    public String getWordListFileName() {
        String ret = getWordList().getLastPathSegment();
        return ret.substring(ret.lastIndexOf(':') + 1);
    }

    public void setWordList(Uri wordList) {
        this.wordList = wordList;
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

            if (state == State.NOT_RUNNING) {
                setProgress(null);
                hashCat = null;
            }
        }
    }

    public HashCat.Progress getProgress() {
        return hashCat != null ? hashCat.getProgress() : null;
    }

    public void setProgress(HashCat.Progress newProgress) {
        for (Listener listener : listeners)
            listener.onHashCatProgressChange(this, newProgress);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String newPassword) {
        password = newPassword;
    }

    public boolean start(Context context) {
        return start(context, null);
    }

    public boolean start(Context context, HashCat.Listener listener) {
        if (state != State.NOT_RUNNING)
            return false;

        stop();

        // Reset password
        password = null;

        hashCat = new HashCat(context, this, listener);
        hashCat.start();

        return true;
    }

    // Stop hashcat job if running
    public void stop() {
        if (hashCat != null)
            hashCat.abort();
        // hashCat will be set to null on NOT_RUNNING state
    }
}
