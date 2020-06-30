package com.talybin.aircat;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.File;

@Entity(tableName = "wordlist_table")
public class WordList {

    /*
    public enum Type {
        UNKNOWN,
        PLAIN,
        ZIP,
        GZ,
    }*/

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uri")
    @TypeConverters(UriConverter.class)
    private Uri uri;

    @ColumnInfo(name = "nr_words")
    @Nullable
    private Long nrWords;

    @Ignore
    WordList(@NonNull Uri uri) {
        this(uri, null);
    }

    WordList(@NonNull Uri uri, @Nullable Long nrWords) {
        this.uri = uri;
        this.nrWords = nrWords;
    }

    @NonNull
    Uri getUri() {
        return uri;
    }

    @Nullable
    Long getNrWords() {
        return nrWords;
    }

    void setNrWords(@Nullable Long nrWords) {
        this.nrWords = nrWords;
        writeChanges();
    }

    static String getFileName(Uri uri) {
        String ret = Uri.decode(uri.toString());
        return ret.substring(ret.lastIndexOf("//") + 1);
    }

    static Uri getDefault() {
        String defaultPath = App.getContext().getFilesDir() + "/wordlists/english.gz";
        return Uri.fromFile(new File(defaultPath));
    }

    private void writeChanges() {
        WordListManager.getInstance().update(this);
    }
}
