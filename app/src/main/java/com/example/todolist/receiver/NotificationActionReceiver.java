package com.example.todolist.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.todolist.service.PomodoroService;

/**
 * Handles notification action button clicks (pause/resume/stop)
 * from the Pomodoro foreground service notification.
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Intent serviceIntent = new Intent(context, PomodoroService.class);

        if ("com.example.todolist.action.NOTIFICATION_PAUSE".equals(action)) {
            serviceIntent.setAction(PomodoroService.ACTION_PAUSE);
            context.startService(serviceIntent);
        } else if ("com.example.todolist.action.NOTIFICATION_RESUME".equals(action)) {
            serviceIntent.setAction(PomodoroService.ACTION_RESUME);
            context.startService(serviceIntent);
        } else if ("com.example.todolist.action.NOTIFICATION_STOP".equals(action)) {
            serviceIntent.setAction(PomodoroService.ACTION_STOP);
            context.startService(serviceIntent);
        }
    }
}
