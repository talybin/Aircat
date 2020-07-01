package com.talybin.aircat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

class HashCatService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("HashCatService", "---> onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("HashCatService", "---> onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("HashCatService", "---> onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("HashCatService", "---> onBind");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("HashCatService", "---> onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d("HashCatService", "---> onRebind");
    }
}
