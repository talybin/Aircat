package com.talybin.aircat;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WordListDao {

    // Allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(WordList wordList);

    @Query("DELETE FROM wordlist_table")
    void deleteAll();

    @Query("SELECT * FROM wordlist_table")
    LiveData<List<WordList>> getWordLists();

    //@Query("SELECT * FROM wordlist_table WHERE uri = ':uri'")
    //WordList getWordList(Uri uri);
}
