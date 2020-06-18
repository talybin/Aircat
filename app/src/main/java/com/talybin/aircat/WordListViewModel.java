package com.talybin.aircat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

/*
  A ViewModel holds your app's UI data in a lifecycle-conscious way that survives
  configuration changes. Separating your app's UI data from your Activity and
  Fragment classes lets you better follow the single responsibility principle:
  Your activities and fragments are responsible for drawing data to the screen,
  while your ViewModel can take care of holding and processing all the data
  needed for the UI.
  In the ViewModel, use LiveData for changeable data that the UI will use or
  display. Using LiveData has several benefits:

    * You can put an observer on the data (instead of polling for changes) and
      only update the the UI when the data actually changes.
    * The Repository and the UI are completely separated by the ViewModel.
    * There are no database calls from the ViewModel (this is all handled in the
      Repository), making the code more testable.

  http://shorturl.at/nosHU
 */
public class WordListViewModel extends AndroidViewModel {

    private AircatRepository repository;
    private LiveData<List<WordList>> allWordLists;

    public WordListViewModel(@NonNull Application application) {
        super(application);

        repository = new AircatRepository(application);
        allWordLists = repository.getAllWordLists();
    }

    LiveData<List<WordList>> getAllWordLists() {
        return allWordLists;
    }

    void insert(WordList wordList) {
        repository.insert(wordList);
    }
}
