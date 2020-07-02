package com.talybin.aircat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class HashCatInterface implements ServiceConnection {

    private static HashCatInterface instance = null;

    private HashCatService hashCatService = null;
    private List<Consumer<HashCatService>> runQueue = new ArrayList<>();

    static HashCatInterface getInstance() {
        if (instance == null) {
            synchronized (HashCatInterface.class) {
                if (instance == null)
                    instance = new HashCatInterface();
            }
        }
        return instance;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        HashCatService.ServiceBinder binder = (HashCatService.ServiceBinder) service;
        hashCatService = binder.getService();

        // Invoke runnable queue
        runQueue.forEach(fn -> fn.accept(hashCatService));
        runQueue.clear();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        hashCatService = null;
    }

    void start(Context context) {
        context.startService(new Intent(context, HashCatService.class));
    }

    void stop(Context context) {
        context.stopService(new Intent(context, HashCatService.class));
    }

    void bind(Context context) {
        Intent intent = new Intent(context, HashCatService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    void unbind(Context context) {
        context.unbindService(this);
    }

    void submit(Consumer<HashCatService> fn) {
        if (hashCatService != null)
            fn.accept(hashCatService);
        else
            runQueue.add(fn);
    }

    void setErrorListener(HashCatService.ErrorListener listener) {
        submit(service -> service.setErrorListener(listener));
    }

    void start(Job... jobs) {
        start(Arrays.asList(jobs));
    }

    // Start or queue processing with specified jobs
    void start(List<Job> jobs) {
        submit(service -> service.start(jobs));
    }

    void stop(Job... jobs) {
        stop(Arrays.asList(jobs));
    }

    void stop(List<Job> jobs) {
        submit(service -> service.stop(jobs));
    }
}
