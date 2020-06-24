package com.talybin.aircat;

import android.app.Application;
import android.content.Context;

// This class instantiates from manifest file
// <application android:name=".App" ... />
public class App extends Application {

    private static App instance = null;
    private static AircatRepository repository = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    public static App getInstance() {
        return instance;
    }

    public static AircatRepository repo() {
        if (repository == null)
            repository = new AircatRepository(instance);
        return repository;
    }
}
