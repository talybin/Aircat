package com.talybin.aircat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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

    public interface StateListener {
        void onStateChange(Job job);
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
    private StateListener stateListener = null;

    @Ignore
    private HashCat.Listener progressListener = null;

    @Ignore
    private HashCat hashCat = null;

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

    @NonNull
    String getPmkId() {
        return pmkId;
    }

    @Nullable
    String getSsid() {
        return ssid;
    }

    @NonNull
    String getApMac() {
        return apMac;
    }

    @NonNull
    String getClientMac() {
        return clientMac;
    }

    void setWordList(@Nullable Uri uri) {
        wordList = uri;
    }

    @Nullable
    Uri getWordList() {
        return wordList;
    }

    void setPassword(@Nullable String password) {
        this.password = password;
        writeChanges();
    }

    @Nullable
    String getPassword() {
        return password;
    }

    void setState(State state) {
        this.state = state;
        if (state == State.NOT_RUNNING)
            hashCat = null;
        if (stateListener != null)
            stateListener.onStateChange(this);
    }
    State getState() {
        return state;
    }

    @Nullable
    HashCat.Progress getProgress() {
        return hashCat != null ? hashCat.getProgress() : null;
    }

    void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    void setProgressListener(HashCat.Listener listener) {
        this.progressListener = listener;
    }

    boolean start(Context context) {
        if (state != Job.State.NOT_RUNNING)
            return false;

        stop();
        // Reset password
        password = null;

        hashCat = new HashCat(context, this, (progress, ex) -> {
            if (progressListener != null)
                progressListener.onProgress(progress, ex);
        });
        hashCat.start();

        return true;
    }

    // Stop hashcat job if running
    void stop() {
        if (hashCat != null)
            hashCat.abort();
        // hashCat will be set to null on NOT_RUNNING state
    }

    void writeChanges() {
        App.repo().update(this);
    }
}
