package com.talybin.aircat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
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

    // Messenger for communicating with the service
    private Messenger messenger = null;

    // Waiting for service connection queue
    private List<Consumer<Messenger>> sendQueue = new ArrayList<>();

    private ErrorListener errorListener = null;

    // Singleton constructor
    private HashCatInterface() {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        messenger = new Messenger(service);

        // Send current settings
        updateSettings();

        // Invoke runnable queue
        sendQueue.forEach(fn -> fn.accept(messenger));
        sendQueue.clear();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        messenger = null;
    }

    // Start service
    void start(Context context) {
        Intent intent = new Intent(context, HashCatService.class);
        intent.putExtra(HashCatService.ARG_PATH,
                context.getFilesDir().toString() + "/hashcat");
        context.startService(intent);
    }

    // Stop service
    void stop(Context context) {
        context.stopService(new Intent(context, HashCatService.class));
    }

    // Bind to service
    void bind(Context context) {
        Intent intent = new Intent(context, HashCatService.class);

        // Setup event listener
        intent.putExtra(HashCatService.ARG_RECEIVER,
                new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        onServiceEvent(resultCode, resultData);
                    }
                });

        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    // Unbind from service
    void unbind(Context context) {
        context.unbindService(this);
    }

    // Event from service
    private void onServiceEvent(int eventCode, Bundle eventData) {
        switch (eventCode) {
            case HashCatService.EVENT_CODE_ERROR:
                setError(eventData.getString(HashCatService.EVENT_ARG_ERROR));
                break;
        }
    }

    // Submit message to service
    private void submit(Message msg) {
        Consumer<Messenger> fn = m -> {
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
        Message msg = Message.obtain(null, HashCatService.MSG_START_JOBS);
        Bundle args = new Bundle();

        args.putParcelableArrayList(HashCatService.ARG_JOB_LIST, new ArrayList<>(jobs));

        msg.setData(args);
        submit(msg);
    }

    void stop(Job... jobs) {
        stop(Arrays.asList(jobs));
    }

    void stop(List<Job> jobs) {
        Message msg = Message.obtain(null, HashCatService.MSG_STOP_JOBS);
        Bundle args = new Bundle();

        args.putParcelableArrayList(HashCatService.ARG_JOB_LIST, new ArrayList<>(jobs));

        msg.setData(args);
        submit(msg);
    }

    // Should be called whenever settings has been altered
    void updateSettings() {
        Message msg = Message.obtain(null, HashCatService.MSG_SET_SETTINGS);
        Bundle settings = new Bundle();

        String[] keys = {
                HashCatService.SETTING_POWER_USAGE,
                HashCatService.SETTING_REFRESH_INTERVAL,
        };

        for (String key : keys)
            settings.putString(key, App.settings().getString(key, null));

        msg.setData(settings);
        submit(msg);
    }
}
