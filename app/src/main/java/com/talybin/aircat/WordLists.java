package com.talybin.aircat;

import android.net.Uri;

import androidx.annotation.NonNull;

public class WordLists {

    private static WordLists instance = null;

    static WordLists getInstance() {
        if (instance == null) {
            synchronized (WordLists.class) {
                if (instance == null)
                    instance = new WordLists();
            }
        }
        return instance;
    }

    private WordListDao wordListDao;

    private WordLists() {
        wordListDao = AppDatabase.getDatabase(App.getContext()).wordListDao();
    }

    // Get word list synchronously.
    // On failure will create and add new word list asynchronously.
    @NonNull
    static WordList getOrCreate(Uri uri) {
        WordList wordList = getInstance().wordListDao.getSync(uri);
        if (wordList == null)
            add(wordList = new WordList(uri));
        return wordList;
    }

    static void update(WordList wordList) {
        AppDatabase.databaseExecutor.execute(
                () -> getInstance().wordListDao.update(wordList));
    }

    static void add(WordList wordList) {
        AppDatabase.databaseExecutor.execute(
                () -> getInstance().wordListDao.insert(wordList));
    }
}
