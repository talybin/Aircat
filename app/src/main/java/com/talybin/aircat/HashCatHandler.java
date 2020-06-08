package com.talybin.aircat;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

public class HashCatHandler {

    private Handler handler;
    private HashCat hashCat;
    private Job job;

    public HashCatHandler(Job job) {
        super();

        this.job = job;
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                return HashCatHandler.this.handleMessage(msg);
            }
        });
        hashCat = new HashCat(job, handler);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }

    public void start() {
        hashCat.start();
    }

    public void stop() {
        hashCat.abort();
    }

    private boolean handleMessage(Message msg) {
        Log.d("HashCatHandler", "---> Got message: " + msg.what);
        switch (msg.what) {
            case HashCat.MSG_SET_STATE:
                job.setState((Job.State)msg.obj);
                break;
            case HashCat.MSG_ERROR:
                Log.e("HashCatHandler", ((Exception)msg.obj).getMessage());
                break;
        }
        return true;
    }

    /*
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (thread.isAlive())
    }*/
}
