package com.talybin.aircat;

import android.app.Application;
import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This class instantiates from manifest file
// <application android:name=".App" ... />
public class App extends Application {

    private static App instance = null;

    private ExecutorService poolExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Creates a thread pool that creates new threads as needed, but will
        // reuse previously constructed threads when they are available
        poolExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void onTerminate() {
        poolExecutor.shutdownNow();
        instance = null;

        super.onTerminate();
    }

    public static App getInstance() {
        return instance;
    }
    public static Context getContext() {
        return instance;
    }

    public static ExecutorService getThreadPool() {
        return instance.poolExecutor;
    }
}
