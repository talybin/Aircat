package com.talybin.aircat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class WordListViewModel extends AndroidViewModel {

    private AircatRepository repository;
    //private List<WordList> allWordLists;

    public WordListViewModel(@NonNull Application application) {
        super(application);

        repository = new AircatRepository(application);
        //allWordLists = repository.getAllWordLists();
    }

    /*
    List<WordList> getAllWordLists() {
        return allWordLists;
    }*/

    void insert(WordList wordList) {
        repository.insert(wordList);
    }
}
