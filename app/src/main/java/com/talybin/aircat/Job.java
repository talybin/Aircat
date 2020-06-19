package com.talybin.aircat;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "job_table")
public class Job {

    @PrimaryKey
    @ColumnInfo(name = "pmkid")
    @NonNull
    private String pmkId;

    @ColumnInfo(name = "ssid")
    @Nullable
    private String ssid;

    @ColumnInfo(name = "ap_mac")
    @NonNull
    private String apMac;

    @ColumnInfo(name = "client_mac")
    @NonNull
    private String clientMac;
    
    @ForeignKey(
            entity = WordList.class,
            parentColumns = "uri",
            childColumns = "word_list",
            onDelete = ForeignKey.SET_NULL
    )
    @ColumnInfo(name = "word_list")
    @Nullable
    @TypeConverters(UriConverter.class)
    private Uri wordList;

    @ColumnInfo(name = "password")
    @Nullable
    private String password;

    public Job(
            @NonNull String pmkId,
            @Nullable String ssid,
            @NonNull String apMac,
            @NonNull String clientMac,
            @Nullable Uri wordList,
            @Nullable String password)
    {
        this.pmkId = pmkId;
        this.ssid = ssid;
        this.apMac = apMac;
        this.clientMac = clientMac;
        this.wordList = wordList;
        this.password = password;
    }

    String getPmkId() {
        return pmkId;
    }

    String getSsid() {
        return ssid;
    }

    String getApMac() {
        return apMac;
    }

    String getClientMac() {
        return clientMac;
    }

    Uri getWordList() {
        return wordList;
    }

    String getPassword() {
        return password;
    }
}
