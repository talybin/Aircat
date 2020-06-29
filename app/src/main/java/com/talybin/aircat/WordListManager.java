package com.talybin.aircat;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.function.Consumer;

class WordListManager {

    private static WordListManager instance = null;

    static WordListManager getInstance() {
        if (instance == null) {
            synchronized (WordListManager.class) {
                if (instance == null)
                    instance = new WordListManager();
            }
        }
        return instance;
    }

    private WordListDao wordListDao;
    private Handler uiHandler;

    private WordListManager() {
        wordListDao = AppDatabase.getDatabase(App.getContext()).wordListDao();
        uiHandler = new Handler();
    }

    // Get word list synchronously.
    // On failure will create and add new word list asynchronously.
    @NonNull
    WordList getOrCreate(Uri uri) {
        WordList wordList = wordListDao.get(uri);
        if (wordList == null)
            add(wordList = new WordList(uri));
        return wordList;
    }

    // Get word list asynchronously
    void getOrCreate(Uri uri, Consumer<WordList> callback) {
        AppDatabase.databaseExecutor.execute(() -> {
            WordList wordList = getOrCreate(uri);
            uiHandler.post(() -> callback.accept(wordList));
        });
    }

    void update(WordList wordList) {
        AppDatabase.databaseExecutor.execute(() -> wordListDao.update(wordList));
    }

    void add(WordList wordList) {
        AppDatabase.databaseExecutor.execute(() -> wordListDao.insert(wordList));
    }
}
