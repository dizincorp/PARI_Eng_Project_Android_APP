package com.example.project.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.project.R;

/**
 * Utility helper class for building and displaying local system notifications.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "parkshare_notifications";
    private static final String CHANNEL_NAME = "Powiadomienia ParkShare";

    /**
     * Shows a local system notification to the user.
     *
     * @param context Application context
     * @param title   Notification title
     * @param message Notification body message
     */
    public static void showNotification(Context context, String title, String message) {
        if (context == null) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception ignored) {
        }
    }
}