package com.talybin.aircat;

import android.app.Application;
import android.content.Context;

// This class instantiates from manifest file
// <application android:name=".App" ... />
public class App extends Application {

    private static App instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static App getInstance() {
        return instance;
    }
    public static Context getContext() {
        return instance;
    }
}
