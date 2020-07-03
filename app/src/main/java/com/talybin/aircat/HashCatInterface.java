package com.talybin.aircat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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

    public interface ErrorListener {
        void onError(String err);
    }

    private HashCatService hashCatService = null;

    // Waiting for service connection queue
    private List<Consumer<HashCatService>> sendQueue = new ArrayList<>();

    private ErrorListener errorListener = null;

    // Singleton constructor
    private HashCatInterface() {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        hashCatService = ((HashCatService.LocalBinder) service).getService();

        // Invoke runnable queue
        sendQueue.forEach(fn -> fn.accept(hashCatService));
        sendQueue.clear();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        hashCatService = null;
    }

    // Start service
    void start(Context context) {
        Intent intent = new Intent(context, HashCatService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else
            context.startService(intent);
    }

    // Stop service
    void stop(Context context) {
        context.stopService(new Intent(context, HashCatService.class));
    }

    // Bind to service
    void bind(Context context) {
        //Intent intent = new Intent(context, HashCatService.class);
        //context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    // Unbind from service
    void unbind(Context context) {
        //context.unbindService(this);
    }

    /*
    // Event from service
    private void onServiceEvent(int eventCode, Bundle eventData) {
        switch (eventCode) {
            case HashCatServiceOld2.EVENT_CODE_ERROR:
                setError(eventData.getString(HashCatServiceOld2.EVENT_ARG_ERROR));
                break;
        }
    }*/

    // Submit message to service
    private void submit(Message msg) {
        /*
        Consumer<HashCatService> fn = m -> {
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                setError(e.getMessage());
            }
        };
        if (messenger != null)
            fn.accept(messenger);
        else
            sendQueue.add(fn);
         */
    }

    // Notify listener about error
    private void setError(String err) {
        if (errorListener != null)
            errorListener.onError(err);
        else
            Log.e(HashCatInterface.class.getName(), err);
    }

    void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    void start(Job... jobs) {
        start(Arrays.asList(jobs));
    }

    // Start or queue processing with specified jobs
    void start(List<Job> jobs) {
        Message msg = Message.obtain(null, HashCatServiceOld2.MSG_START_JOBS);
        Bundle args = new Bundle();

        args.putParcelableArrayList(HashCatServiceOld2.ARG_JOB_LIST, new ArrayList<>(jobs));

        msg.setData(args);
        submit(msg);
    }

    void stop(Job... jobs) {
        stop(Arrays.asList(jobs));
    }

    void stop(List<Job> jobs) {
        Message msg = Message.obtain(null, HashCatServiceOld2.MSG_STOP_JOBS);
        Bundle args = new Bundle();

        args.putParcelableArrayList(HashCatServiceOld2.ARG_JOB_LIST, new ArrayList<>(jobs));

        msg.setData(args);
        submit(msg);
    }

    // Should be called whenever settings has been altered
    void updateSettings() {
        Message msg = Message.obtain(null, HashCatServiceOld2.MSG_SET_SETTINGS);
        Bundle settings = new Bundle();

        String[] keys = {
                HashCatServiceOld2.SETTING_POWER_USAGE,
                HashCatServiceOld2.SETTING_REFRESH_INTERVAL,
        };

        for (String key : keys)
            settings.putString(key, App.settings().getString(key, null));

        msg.setData(settings);
        submit(msg);
    }

    /*
    boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager)
                App.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }*/
}
