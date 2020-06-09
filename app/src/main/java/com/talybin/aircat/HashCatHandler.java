package com.talybin.aircat;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class HashCatHandler extends ListenerBase<HashCatHandler.Listener> {

    private Handler handler;
    private HashCat hashCat;

    public interface Listener {
        void onJobState(Job.State state);
        void onStatus(HashCat.Status status);
        void onError(Exception ex);
    }

    public HashCatHandler(Job job) {
        super();

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
                for (Listener l : listeners)
                    l.onJobState((Job.State)msg.obj);
                break;

            case HashCat.MSG_STATUS:
                for (Listener l : listeners)
                    l.onStatus((HashCat.Status)msg.obj);
                break;

            case HashCat.MSG_ERROR:
                for (Listener l : listeners)
                    l.onError((Exception)msg.obj);
                break;
        }
        return true;
    }
}
