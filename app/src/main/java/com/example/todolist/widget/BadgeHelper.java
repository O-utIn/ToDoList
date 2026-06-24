package com.example.todolist.widget;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.util.NotificationHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages a launcher icon badge showing the pending todo count.
 * Uses a silent ongoing notification with setNumber() so the launcher
 * displays a badge on the app icon (Android 8.0+).
 */
public class BadgeHelper {

    private static final int NOTIFICATION_ID = 200;
    private static final Uri PENDING_COUNT_URI =
        Uri.parse("content://com.example.todolist.provider/todos/pending/count");

    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    /**
     * Refresh the badge count. Safe to call from any thread.
     */
    public static void refresh(Context context) {
        final Context appContext = context.getApplicationContext();
        exec.execute(() -> {
            int count = queryPendingCount(appContext);
            if (count > 0) {
                postBadge(appContext, count);
            } else {
                cancelBadge(appContext);
            }
        });
    }

    private static int queryPendingCount(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver()
                .query(PENDING_COUNT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            // Provider might not be ready
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    private static void postBadge(Context context, int count) {
        NotificationHelper.createChannels(context);

        Intent clickIntent = new Intent(context, MainActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_BADGE)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.badge_pending_format, count))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setNumber(count)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build();

        NotificationHelper.post(context, NOTIFICATION_ID, notification);
    }

    private static void cancelBadge(Context context) {
        NotificationHelper.cancel(context, NOTIFICATION_ID);
    }
}
