package com.example.todolist.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.ai.recommendation.RecommendationEngine;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.util.NotificationHelper;
import java.util.List;

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
        final NotificationCompat.Builder builder =
            new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDER)
                .setContentTitle("⏰ 每日提醒")
                .setContentText("别忘了查看今天的习惯和待办事项哦！")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Post generic notification immediately
        NotificationHelper.post(context, 100, builder.build());

        // Async: compute personalized recommendation and update notification
        final BroadcastReceiver.PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                RecommendationEngine engine = new RecommendationEngine(context);
                List<HabitItem> recs = engine.recommend(1);
                if (recs != null && !recs.isEmpty()) {
                    HabitItem top = recs.get(0);
                    String habitName = top.name != null ? top.name : "习惯";
                    String personalized = context.getString(
                        R.string.daily_reminder_personalized, habitName);
                    builder.setContentText(personalized);
                    NotificationHelper.post(context, 100, builder.build());
                }
            } catch (Exception ignored) {
                // Fall back to generic notification already posted
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
