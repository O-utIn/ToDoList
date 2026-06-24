package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.example.todolist.MainActivity;
import com.example.todolist.R;

/**
 * Home screen widget that displays pending todo count and top items.
 * Data is fetched from TodoProvider via ContentResolver on a background thread.
 */
public class TodoWidgetProvider extends AppWidgetProvider {

    private static final Uri PENDING_URI =
        Uri.parse("content://com.example.todolist.provider/todos/pending");
    private static final Uri PENDING_COUNT_URI =
        Uri.parse("content://com.example.todolist.provider/todos/pending/count");

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            // System widget update — run on background thread via goAsync()
            final BroadcastReceiver.PendingResult pendingResult = goAsync();
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (ids != null && ids.length > 0) {
                new Thread(() -> {
                    for (int id : ids) {
                        updateWidget(context, mgr, id);
                    }
                    pendingResult.finish();
                }).start();
            } else {
                pendingResult.finish();
            }
        } else if ("com.example.todolist.APPWIDGET_MANUAL_UPDATE".equals(action)) {
            // Manual refresh from app
            final BroadcastReceiver.PendingResult pendingResult = goAsync();
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final int[] ids = mgr.getAppWidgetIds(
                new ComponentName(context, TodoWidgetProvider.class));
            new Thread(() -> {
                if (ids != null && ids.length > 0) {
                    for (int id : ids) {
                        updateWidget(context, mgr, id);
                    }
                }
                pendingResult.finish();
            }).start();
        } else {
            super.onReceive(context, intent);
        }
    }

    private void updateWidget(Context context, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo);

        // Click anywhere on widget → open MainActivity
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        Cursor countCursor = null;
        Cursor todoCursor = null;
        try {
            countCursor = context.getContentResolver()
                .query(PENDING_COUNT_URI, null, null, null, null);
            todoCursor = context.getContentResolver()
                .query(PENDING_URI, null, null, null, null);

            int count = 0;
            if (countCursor != null && countCursor.moveToFirst()) {
                count = countCursor.getInt(0);
            }
            views.setTextViewText(R.id.widget_count_badge, String.valueOf(count));

            if (count == 0) {
                views.setTextViewText(R.id.widget_title_1,
                    context.getString(R.string.widget_empty));
                views.setViewVisibility(R.id.widget_item_1, View.VISIBLE);
                views.setViewVisibility(R.id.widget_item_2, View.GONE);
                views.setViewVisibility(R.id.widget_item_3, View.GONE);
            } else {
                int idxTitle = todoCursor != null ? todoCursor.getColumnIndex("title") : -1;
                int idxPriority = todoCursor != null ? todoCursor.getColumnIndex("priority") : -1;

                int[] itemIds  = {R.id.widget_item_1,  R.id.widget_item_2,  R.id.widget_item_3};
                int[] titleIds = {R.id.widget_title_1, R.id.widget_title_2, R.id.widget_title_3};
                int[] dotIds   = {R.id.widget_dot_1,   R.id.widget_dot_2,   R.id.widget_dot_3};

                if (todoCursor != null && idxTitle >= 0 && todoCursor.moveToFirst()) {
                    int i = 0;
                    do {
                        String title = todoCursor.getString(idxTitle);
                        int priority = idxPriority >= 0 ? todoCursor.getInt(idxPriority) : 1;

                        views.setTextViewText(titleIds[i], title);
                        views.setImageViewResource(dotIds[i], getPriorityDot(priority));
                        views.setViewVisibility(itemIds[i], View.VISIBLE);
                        i++;
                    } while (todoCursor.moveToNext() && i < 3);

                    for (int j = i; j < 3; j++) {
                        views.setViewVisibility(itemIds[j], View.GONE);
                    }
                }
            }
        } finally {
            if (countCursor != null) countCursor.close();
            if (todoCursor != null) todoCursor.close();
        }

        mgr.updateAppWidget(widgetId, views);
    }

    private int getPriorityDot(int priority) {
        switch (priority) {
            case 2: return R.drawable.widget_dot_high;
            case 1: return R.drawable.widget_dot_medium;
            case 0:
            default: return R.drawable.widget_dot_low;
        }
    }

    /**
     * Request widget refresh from within the app (e.g. after todo changes).
     */
    public static void requestUpdate(Context context) {
        Intent intent = new Intent(context, TodoWidgetProvider.class);
        intent.setAction("com.example.todolist.APPWIDGET_MANUAL_UPDATE");
        context.sendBroadcast(intent);
    }
}
