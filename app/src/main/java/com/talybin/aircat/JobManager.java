package com.talybin.aircat;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class JobManager {

    public interface Listener {
        void onNewJob();
    }

    private static JobManager instance = new JobManager();

    private Set<Listener> listeners;

    public static JobManager getInstance() {
        return instance;
    }

    private JobManager() {
        listeners = new HashSet<>();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void add(ApInfo apInfo) {
        Log.d("JobManager::add", "---> ap mac [" + apInfo.bssid + "]");
        Log.d("JobManager::add", "---> client mac [" + apInfo.clientMac + "]");
        Log.d("JobManager::add", "---> pmkid [" + apInfo.pmkId + "]");

        // Notify others
        for (Listener listener : listeners)
            listener.onNewJob();
    }
}
