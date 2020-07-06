package com.talybin.aircat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class HashCatInterface implements ServiceConnection {

    private static HashCatInterface instance = null;

    // This is a singleton
    static HashCatInterface getInstance() {
        if (instance == null) {
            synchronized (HashCatInterface.class) {
                if (instance == null)
                    instance = new HashCatInterface();
            }
        }
        return instance;
    }

    private HashCatService hashCatService = null;
    private boolean serviceStarting = false;

    // Waiting for service connection queue
    private List<Consumer<HashCatService>> sendQueue = new ArrayList<>();

    // Singleton constructor
    private HashCatInterface() {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        hashCatService = ((HashCatService.LocalBinder) service).getService();
        serviceStarting = false;

        // Invoke runnable queue
        sendQueue.forEach(fn -> fn.accept(hashCatService));
        sendQueue.clear();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        hashCatService = null;
    }

    // Start service
    private void start(Context context) {
        Intent intent = new Intent(context, HashCatService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else
            context.startService(intent);
    }

    /*
    // Stop service
    void stop(Context context) {
        context.stopService(new Intent(context, HashCatService.class));
    }*/

    // Bind to service
    public void bind(Context context) {
        Intent intent = new Intent(context, HashCatService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    // Unbind from service
    public void unbind(Context context) {
        context.unbindService(this);
    }

    // Submit message to service
    private void submit(Consumer<HashCatService> fn) {
        if (hashCatService != null)
            fn.accept(hashCatService);
        else
            sendQueue.add(fn);
    }

    void setErrorListener(HashCatService.ErrorListener listener) {
        submit(service -> service.setErrorListener(listener));
    }

    void start(Job... jobs) {
        start(Arrays.asList(jobs));
    }

    // Start or queue processing with specified jobs
    void start(List<Job> jobs) {
        // Start service if not already
        if (hashCatService == null && !serviceStarting) {
            Context context = App.getContext();

            serviceStarting = true;
            start(context);
        }

        submit(service -> service.start(jobs));
    }

    void stop(Job... jobs) {
        stop(Arrays.asList(jobs));
    }

    void stop(List<Job> jobs) {
        submit(service -> service.stop(jobs));
    }

    void getRunningJobs(Consumer<List<Job>> runningJobs) {
        submit(service -> runningJobs.accept(service.getJobQueue()));
    }
}
