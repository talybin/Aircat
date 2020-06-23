package com.talybin.aircat;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class WordListManager {

    private static WordListManager instance = null;

    private WordListDao wordListDao;

    private Handler uiHandler = new Handler();
    private Map<Uri, WordList> wordLists = new HashMap<>();

    static WordListManager getInstance() {
        if (instance == null)
            instance = new WordListManager();
        return instance;
    }

    private WordListManager() {
        wordListDao = AppDatabase.getDatabase(App.getContext()).wordListDao();

        Future<List<WordList>> future =
                AppDatabase.databaseExecutor.submit(wordListDao::getWordLists);
        // Wait for list
        try {
            future.get().forEach(w -> wordLists.put(w.getUri(), w));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<Uri, WordList> getAll() {
        return wordLists;
    }

    @Nullable
    public WordList get(Uri id) {
        return wordLists.getOrDefault(id, null);
    }
}
