package com.example.project.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.project.R;
import com.example.project.ui.ReservationsActivity;

/**
 * Utility helper class for building and displaying local system notifications.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "parkshare_notifications";
    private static final String CHANNEL_NAME = "Powiadomienia ParkShare";
    private static final String URGENT_CHANNEL_ID = "parkshare_urgent_notifications";
    private static final String URGENT_CHANNEL_NAME = "Pilne alerty rezerwacji";

    /**
     * Shows a local system notification to the user.
     *
     * @param context Application context
     * @param title   Notification title
     * @param message Notification body message
     */
    public static void showNotification(Context context, String title, String message) {
        showNotification(context, title, message, false, null);
    }

    /**
     * Shows a local system notification with optional high-priority urgent alerting
     * and deep-link opening to {@link ReservationsActivity}.
     *
     * @param context        Application context
     * @param title          Notification title
     * @param message        Notification body message
     * @param urgent         True for high-importance heads-up alerts (e.g. overdue check-out)
     * @param notificationKey Optional stable key so updates replace cleanly without spam
     */
    public static void showNotification(Context context,
                                        String title,
                                        String message,
                                        boolean urgent,
                                        @Nullable String notificationKey) {
        if (context == null) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        String channelId = urgent ? URGENT_CHANNEL_ID : CHANNEL_ID;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);

            NotificationChannel urgentChannel = new NotificationChannel(
                    URGENT_CHANNEL_ID,
                    URGENT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            urgentChannel.enableVibration(true);
            manager.createNotificationChannel(urgentChannel);
        }

        Intent intent = new Intent(context, ReservationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationKey != null ? notificationKey.hashCode() : 1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(urgent ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (urgent) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        }

        int notifyId = notificationKey != null ? notificationKey.hashCode() : (int) (System.currentTimeMillis() & 0x7fffffff);
        try {
            manager.notify(notifyId, builder.build());
        } catch (Exception ignored) {
        }
    }
}