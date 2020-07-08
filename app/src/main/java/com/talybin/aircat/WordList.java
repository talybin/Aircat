package com.talybin.aircat;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.File;
import java.util.Date;

@Entity(tableName = "wordlist_table")
public class WordList {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uri")
    @TypeConverters(UriConverter.class)
    private Uri uri;

    @ColumnInfo(name = "nr_words")
    @Nullable
    private Long nrWords;

    @ColumnInfo(name = "last_used")
    @NonNull
    @TypeConverters(DateConverter.class)
    private Date lastUsed;

    @Ignore
    WordList(@NonNull Uri uri) {
        this(uri, null, new Date());
    }

    WordList(@NonNull Uri uri, @Nullable Long nrWords, @NonNull Date lastUsed) {
        this.uri = uri;
        this.nrWords = nrWords;
        this.lastUsed = lastUsed;

        //Log.d("WordList", "---> created: " + WordList.getFileName(uri) + ", words: " + nrWords + ", last used: " + lastUsed);
    }

    @NonNull
    Uri getUri() {
        return uri;
    }

    @Nullable
    Long getNrWords() {
        return nrWords;
    }

    @NonNull
    Date getLastUsed() {
        return lastUsed;
    }

    void setNrWords(@Nullable Long nrWords) {
        this.nrWords = nrWords;
        writeChanges();
    }

    void setLastUsed() {
        this.lastUsed = new Date();
        writeChanges();
    }

    static String getFilePath(Uri uri) {
        String ret = Uri.decode(uri.toString());
        return ret.substring(ret.lastIndexOf("//") + 1);
    }

    static String getFileName(Uri uri) {
        return new File(getFilePath(uri)).getName();
    }

    static Uri getDefault() {
        String defaultPath = App.getContext().getFilesDir() + "/rockyou.txt.gz";
        return Uri.fromFile(new File(defaultPath));
    }

    private void writeChanges() {
        this.lastUsed = new Date();
        WordListManager.getInstance().update(this);
    }
}
