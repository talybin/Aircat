package com.talybin.aircat;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    private List<WordList> wordLists = new ArrayList<>();

    private WordListManager() {
        wordListDao = AppDatabase.getDatabase(App.getContext()).wordListDao();

        //AppDatabase.databaseExecutor.execute(wordListDao::deleteAll);

        AppDatabase.databaseExecutor.execute(() -> {
            wordLists = wordListDao.getWordLists();
        });
    }

    List<WordList> getAll() {
        return wordLists;
    }

    @Nullable
    WordList get(Uri uri) {
        for (WordList wl : wordLists) {
            if (wl.getUri().equals(uri))
                return wl;
        }
        return null;
    }

    @NonNull
    WordList getOrCreate(Uri uri) {
        WordList wordList = get(uri);
        if (wordList == null)
            noCheckAdd(wordList = new WordList(uri));
        return wordList;
    }

    void update(WordList wordList) {
        AppDatabase.databaseExecutor.execute(() -> wordListDao.update(wordList));
    }

    boolean add(WordList wordList) {
        if (get(wordList.getUri()) != null)
            return false;

        noCheckAdd(wordList);
        return true;
    }

    private void noCheckAdd(WordList wordList) {
        wordLists.add(wordList);
        AppDatabase.databaseExecutor.execute(() -> wordListDao.insert(wordList));
    }
}
