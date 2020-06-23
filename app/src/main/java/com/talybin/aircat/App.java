package com.talybin.aircat;

import android.app.Application;
import android.content.Context;

// This class instantiates from manifest file
// <application android:name=".App" ... />
public class App extends Application {

    private static Context context;
    private static AircatRepository repository = null;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }

    public static AircatRepository repo() {
        if (repository == null)
            repository = new AircatRepository((Application)context);
        return repository;
    }
}
