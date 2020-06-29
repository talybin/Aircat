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

import java.util.function.Predicate;

@Entity(tableName = "job_table")
public class Job {

    public enum State {
        NOT_RUNNING,
        QUEUED,
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
                case QUEUED:
                    return context.getString(R.string.queued);
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

    public interface ProgressListener {
        void onProgressChange(Job job, HashCat.Progress progress);
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
    private Uri uri;

    @ColumnInfo(name = "password")
    @Nullable
    private String password;

    @Ignore
    private State state = State.NOT_RUNNING;

    @Ignore
    private StateListener stateListener = null;

    @Ignore
    private HashCat.Progress progress = null;

    @Ignore
    private ProgressListener progressListener = null;

    public Job(
            @NonNull String pmkId,
            @Nullable String ssid,
            @NonNull String apMac,
            @NonNull String clientMac,
            @Nullable Uri uri,
            @Nullable String password)
    {
        this.pmkId = pmkId;
        this.ssid = ssid;
        this.apMac = apMac;
        this.clientMac = clientMac;
        this.uri = uri;
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

    void setUri(@Nullable Uri uri) {
        this.uri = uri;
        writeChanges();
    }

    @NonNull
    Uri getUri() {
        if (uri == null)
            uri = WordList.getDefault();
        return uri;
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
        if (state != this.state) {
            this.state = state;

            // Remove progress on stopped job
            if (state == State.NOT_RUNNING)
                setProgress(null);

            if (stateListener != null)
                stateListener.onStateChange(this);
        }
    }

    State getState() {
        return state;
    }

    // Return true if job is running
    boolean isRunning() {
        return state == State.STARTING || state == State.RUNNING;
    }

    // Return true if job has been added to queue or running
    boolean isProcessing() {
        return isRunning() || state == State.QUEUED;
    }

    // Return hash in hashcat format: <pmkid>*<ap mac>*<client mac>*<ssid as hex>
    @NonNull
    String getHash() {
        return String.format("%s*%s*%s*%s",
                pmkId,
                apMac.replace(":", ""),
                clientMac.replace(":", ""),
                Utils.toHexSequence(ssid != null ? ssid : ""));
    }

    void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    @Nullable
    HashCat.Progress getProgress() {
        return progress;
    }

    void setProgress(HashCat.Progress progress) {
        this.progress = progress;
        if (progressListener != null)
            progressListener.onProgressChange(this, progress);
    }

    private void writeChanges() {
        JobManager.getInstance().update(this);
    }
}
