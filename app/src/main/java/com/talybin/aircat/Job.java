package com.talybin.aircat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "job_table")
public class Job {

    @PrimaryKey
    @ColumnInfo(name = "pmkid")
    @NonNull
    private String pmkid;

    @ColumnInfo(name = "ssid")
    @Nullable
    private String ssid;

    @ColumnInfo(name = "ap_mac")
    @NonNull
    private String apMac;

    @ColumnInfo(name = "client_mac")
    @NonNull
    private String clientMac;
    
    @ColumnInfo(name = "password")
    @Nullable
    private String password;
    
    @ForeignKey(
            entity = WordList.class,
            parentColumns = "uri",
            childColumns = "word_list",
            onDelete = ForeignKey.SET_NULL
    )
    @ColumnInfo(name = "word_list")
    @Nullable
    private WordList wordList;

    public Job(
            @NonNull String pmkid,
            @Nullable String ssid,
            @NonNull String apMac,
            @NonNull String clientMac,
            @Nullable String password,
            @Nullable WordList wordList)
    {
        this.pmkid = pmkid;
        this.ssid = ssid;
        this.apMac = apMac;
        this.clientMac = clientMac;
        this.password = password;
        this.wordList = wordList;
    }
}
