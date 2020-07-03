package com.talybin.aircat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HashCatService extends Service {

    class LocalBinder extends Binder {
        HashCatService getService() {
            return HashCatService.this;
        }
    }

    private LocalBinder localBinder = new LocalBinder();

    /*
    private final Map<String, Consumer<Intent>> actions =
            new HashMap<String, Consumer<Intent>>()
            {{
                put("start", HashCatService.this::start);
                put("stop", HashCatService.this::stop);
            }};
     */

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        /*
        Consumer<Intent> action = actions.get(intent.getAction());
        if (action != null)
            action.accept(intent);
        else
            Log.e(HashCatService.class.getName(), "unknown command");
         */

        showNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    /*
    private void startJobs(Intent intent) {
    }

    private void stopJobs(Intent intent) {
    }
     */

    static final int ONGOING_NOTIFICATION_ID = 1;
    static final String CHANNEL_ID = "com.talybin.aircat.notification.CHANNEL_ID_FOREGROUND";

    private void showNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                // Flag indicating that if the described PendingIntent already exists,
                // the current one should be canceled before generating a new one
                this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification =
                getNotificationBuilder(this, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
                        .setContentTitle("Service sticker")
                        .setContentText("My content")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent)
                        .setTicker("My ticker")
                        // Disable notification sound
                        .setNotificationSilent()
                        // Ongoing notifications do not have an 'X' close button,
                        // and are not affected by the "Clear all" button
                        .setOngoing(true)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        Toast.makeText(this, "onStartCommand", Toast.LENGTH_LONG).show();
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context, String channelId, int importance) {
        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, channelId, importance);
            builder = new NotificationCompat.Builder(context, channelId);
        }
        else
            builder = new NotificationCompat.Builder(context);

        return builder;
    }

    @TargetApi(26)
    private static void prepareChannel(Context context, String id, int importance) {
        final String appName = context.getString(R.string.app_name);
        String description = "whatever"; //context.getString(R.string.notifications_channel_description);
        final NotificationManager nm = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);

        if (nm != null) {
            NotificationChannel channel = nm.getNotificationChannel(id);

            if (channel == null) {
                channel = new NotificationChannel(id, appName, importance);
                channel.setDescription(description);
                nm.createNotificationChannel(channel);
            }
        }
    }

    /*
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "hashcat_channel_01";//getString(R.string.channel_name);
            String description = "whatever";//getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }*/
}
