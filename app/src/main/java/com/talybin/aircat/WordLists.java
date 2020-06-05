package com.talybin.aircat;

public class WordLists {

    private static String builtInPath = null;

    public static void setBuiltInPath(String path) {
        builtInPath = path;
    }

    public static String getBuiltInPath() {
        return builtInPath;
    }
}
