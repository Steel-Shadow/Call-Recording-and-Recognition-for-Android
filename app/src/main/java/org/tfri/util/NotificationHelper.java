package org.tfri.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import org.tfri.CallRecordingService;
import org.tfri.R;

public class NotificationHelper {

    private static final String CHANNEL_ID = "ChannelID";
    private static final String CHANNEL_NAME = CallRecordingService.class.getSimpleName() + " Channel";
    private static final int NOTIFICATION_ID = 1;

    public static void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT));

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.warning)
                .setContentTitle(title)
                .setContentText(message);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}