package com.talybin.aircat;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "job_table")
public class Job {

    public enum State {
        NOT_RUNNING,
        STARTING,
        RUNNING,
        STOPPING;

        @NonNull
        @Override
        public String toString() {
            Context context = App.getContext();
            switch (this) {
                case NOT_RUNNING:
                    return context.getString(R.string.not_running);
                case STARTING:
                    return context.getString(R.string.starting);
                case RUNNING:
                    return context.getString(R.string.running);
                case STOPPING:
                    return context.getString(R.string.stopping);
                default:
                    return super.toString();
            }
        }
    }

    public interface Listener {
        void onStateChange();
    }

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

    @Ignore
    private State state = State.NOT_RUNNING;

    @Ignore
    private Listener listener = null;

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

    void setWordList(@Nullable Uri uri) {
        wordList = uri;
    }

    State getState() {
        return state;
    }

    void setState(State state) {
        this.state = state;
        if (listener != null)
            listener.onStateChange();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }
}
