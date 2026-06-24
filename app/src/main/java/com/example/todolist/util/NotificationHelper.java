package com.example.todolist.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Helper for creating notification channels and building notifications.
 */
public class NotificationHelper {

    // Channel IDs
    public static final String CHANNEL_POMODORO = "pomodoro_channel";
    public static final String CHANNEL_REMINDER = "reminder_channel";
    public static final String CHANNEL_GENERAL = "general_channel";
    public static final String CHANNEL_BADGE = "badge_channel";

    /**
     * Create all app notification channels. Safe to call multiple times.
     */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Pomodoro timer channel — low importance to avoid sound on every tick
        NotificationChannel pomodoroChannel = new NotificationChannel(
            CHANNEL_POMODORO,
            "番茄钟",
            NotificationManager.IMPORTANCE_LOW
        );
        pomodoroChannel.setDescription("番茄钟计时通知");
        nm.createNotificationChannel(pomodoroChannel);

        // Daily reminder channel
        NotificationChannel reminderChannel = new NotificationChannel(
            CHANNEL_REMINDER,
            "每日提醒",
            NotificationManager.IMPORTANCE_HIGH
        );
        reminderChannel.setDescription("每日习惯和待办提醒");
        nm.createNotificationChannel(reminderChannel);

        // General channel
        NotificationChannel generalChannel = new NotificationChannel(
            CHANNEL_GENERAL,
            "通用通知",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        nm.createNotificationChannel(generalChannel);

        // Badge channel — silent, no vibration, for launcher icon badge
        NotificationChannel badgeChannel = new NotificationChannel(
            CHANNEL_BADGE,
            "角标通知",
            NotificationManager.IMPORTANCE_MIN
        );
        badgeChannel.setDescription("应用图标角标数字");
        nm.createNotificationChannel(badgeChannel);
    }

    /**
     * Build a simple notification with optional pending intent.
     */
    public static Notification buildSimple(Context context, String channelId, String title, String content,
                                            int iconRes, Intent clickIntent) {
        PendingIntent pendingIntent = null;
        if (clickIntent != null) {
            pendingIntent = PendingIntent.getActivity(context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        return new NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build();
    }

    /**
     * Post a notification.
     */
    public static void post(Context context, int id, Notification notification) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(id, notification);
    }

    /**
     * Cancel a notification by ID.
     */
    public static void cancel(Context context, int id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(id);
    }
}
