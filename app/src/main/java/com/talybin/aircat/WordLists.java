package com.talybin.aircat;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class WordLists {

    private static WordLists instance = null;

    public static WordLists getInstance() {
        return instance;
    }

    public static void create(Context context) {
        instance = new WordLists(context);
    }

    //private Map<Uri, Long> wordLists;
    private SharedPreferences prefs;
    private Uri builtInPath;
    //private File storeFile;

    private WordLists(Context context) {
        prefs = context.getSharedPreferences("wordlists.db", MODE_PRIVATE);
        builtInPath = Uri.fromFile(Paths.get(
                context.getFilesDir().toString(), "wordlists", "english.txt").toFile());
        //storeFile = Paths.get(
                //context.getCacheDir().toString(), "wordlists.db").toFile();
    }

    public Uri getBuiltIn() {
        return builtInPath;
    }

    public long getNrWords(Uri uri) {
        return prefs.getLong(uri.toString(), 0);
    }

    public void setNrWords(Uri uri, long count) {
        prefs.edit().putLong(uri.toString(), count).apply();
    }
}
