package com.talybin.aircat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class HashCatService extends Service {

    // Service actions
    static final String ACTION_START = "start";
    static final String ACTION_PROGRESS = "progress";
    static final String ACTION_PASSWORD = "password";
    static final String ACTION_CANCEL = "cancel";

    // Notification related
    private static final int PROGRESS_NOTIFICATION_ID = 1;
    private static int dynamicId = PROGRESS_NOTIFICATION_ID + 1;
    private NotificationCompat.Builder progressBuilder = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d("HashCatService", "---> onCreate: " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        //Log.d("HashCatService", "---> onStartCommand: " + action + ", this: " + this);
        if (action != null) {
            switch (action) {
                case ACTION_START:
                    createProgressBuilder();
                    break;

                case ACTION_PROGRESS:
                    processProgress(intent);
                    break;

                case ACTION_PASSWORD:
                    processPassword(intent);
                    break;

                case ACTION_CANCEL:
                    stopForeground(true);
                    HashCat.getInstance().post(() -> HashCat.getInstance().stopAll());
                    break;
            }
        }
        // Do not restart service if it gets killed
        return Service.START_NOT_STICKY;
    }

    public static void start() {
        Context context = App.getContext();
        Intent intent = new Intent(context, HashCatService.class);
        intent.setAction(ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else
            context.startService(intent);
    }

    public static void stop() {
        Context context = App.getContext();
        Intent intent = new Intent(context, HashCatService.class);

        context.stopService(intent);
    }

    public static void showProgress(List<Job> jobs, HashCat.Progress progress) {
        Context context = App.getContext();
        Intent intent = new Intent(context, HashCatService.class);
        intent.setAction(ACTION_PROGRESS);

        float percentComplete = 0;
        if (progress.total > 0)
            percentComplete = progress.nr_complete * 100.f / progress.total;

        intent.putExtra("nr_jobs", jobs.size());
        intent.putExtra("percent", percentComplete);

        context.startService(intent);
    }

    private void processProgress(Intent intent) {
        int nr_jobs = intent.getIntExtra("nr_jobs", 0);
        float percentComplete = intent.getFloatExtra("percent", 0);

        progressBuilder
                .setContentTitle(getString(R.string.running_jobs, nr_jobs))
                .setProgress(100, Math.round(percentComplete), false)
                .setContentText(getString(R.string.complete_percent, percentComplete));

        NotificationManagerCompat.from(this)
                .notify(PROGRESS_NOTIFICATION_ID, progressBuilder.build());
    }

    public static void showPassword(Job job, String password) {
        Context context = App.getContext();
        Intent intent = new Intent(context, HashCatService.class);
        intent.setAction(ACTION_PASSWORD);

        intent.putExtra("ssid", job.getSafeSSID());
        intent.putExtra("password", password);

        context.startService(intent);
    }

    private void processPassword(Intent intent) {
        String ssid = intent.getStringExtra("ssid");
        String password = intent.getStringExtra("password");

        Notification notification = getNotificationBuilder(getCategory(R.string.category_password))
                .setContentTitle(ssid)
                .setContentText(password)
                .setOngoing(false)
                .build();

        NotificationManagerCompat.from(this)
                .notify(dynamicId++, notification);
    }

    private void createProgressBuilder() {
        Intent cancelIntent = new Intent(this, HashCatService.class);
        cancelIntent.setAction(ACTION_CANCEL);

        PendingIntent cancelPendingIntent =
                PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                //PendingIntent.getForegroundService(this, 0, cancelIntent, 0);

        progressBuilder = getNotificationBuilder(getCategory(R.string.category_progress))
                .setContentTitle(getString(R.string.starting))
                .setContentText(getString(R.string.complete_percent, 0.f))
                // Add a progress bar
                .setProgress(100, 0, true)
                // Disable notification sound
                .setNotificationSilent()
                // Add actions
                .addAction(R.drawable.ic_stop, getString(android.R.string.cancel), cancelPendingIntent)
                // Ongoing notifications do not have an 'X' close button,
                // and are not affected by the "Clear all" button
                .setOngoing(true);

        startForeground(PROGRESS_NOTIFICATION_ID, progressBuilder.build());
    }

    /*
    private void finalReport() {
        NotificationCompat.Builder builder = getNotificationBuilder(getCategory(R.string.category_final_report))
                .setContentTitle(getString(R.string.finished_jobs))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setOngoing(false);

        NotificationManagerCompat.from(this)
                .notify(dynamicId++, builder.build());
    }*/

    private NotificationCompat.Builder getNotificationBuilder(String channelId) {
        NotificationCompat.Builder builder;

        // Note, MainActivity has android:launchMode="singleTask" attribute
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder = new NotificationCompat.Builder(this, channelId);
        else
            builder = new NotificationCompat.Builder(this);

        return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
    }

    private String getCategory(int resourceName) {
        return getCategory(getString(resourceName), null, NotificationManager.IMPORTANCE_DEFAULT);
    }

    private String getCategory(String name, String description, int importance) {

        final NotificationManager nm = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
        final String channelId = HashCatService.class.getName() + "." + name;

        if (nm != null) {
            NotificationChannel channel = nm.getNotificationChannel(channelId);

            if (channel == null) {
                channel = new NotificationChannel(channelId, name, importance);
                if (description != null)
                    channel.setDescription(description);
                nm.createNotificationChannel(channel);
            }
        }

        return channelId;
    }
}
