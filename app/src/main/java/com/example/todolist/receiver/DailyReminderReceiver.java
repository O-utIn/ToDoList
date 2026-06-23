package com.example.todolist.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.util.NotificationHelper;

/**
 * Broadcast receiver triggered by AlarmManager for daily habit/todo reminders.
 */
public class DailyReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Build notification
        Intent clickIntent = new Intent(context, MainActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationHelper.createChannels(context);
        androidx.core.app.NotificationCompat.Builder builder =
            new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDER)
                .setContentTitle("⏰ 每日提醒")
                .setContentText("别忘了查看今天的习惯和待办事项哦！")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationHelper.post(context, 100, builder.build());
    }
}
