package com.talybin.aircat;

import java.util.LinkedList;
import java.util.List;

public class ListenerBase<T> {

    protected List<T> listeners = new LinkedList<>();

    protected ListenerBase() {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        listeners.clear();
    }

    public void addListener(T listener) {
        listeners.add(listener);
    }

    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    /*
    protected <M, Args> void fire(M method, Args... args) {
        for (T listener : listeners)
            method(args);
    }*/
}
