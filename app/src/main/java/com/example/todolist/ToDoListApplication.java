package com.example.todolist;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Application subclass. Database is initialized lazily by AppDatabase.getInstance().
 *
 * Pre-warms heavy UI components (DatePicker, TimePicker) on a deferred main-thread
 * runnable so the first user-triggered dialog opens instantly instead of janking.
 */
public class ToDoListApplication extends Application {

    private static final String TAG = "ToDoListApp";
    private static ToDoListApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prewarmPickers();
    }

    public static ToDoListApplication getInstance() {
        return instance;
    }

    /**
     * Deferred pre-initialization of DatePickerDialog and TimePickerDialog.
     * Creating + dismissing them once forces Android to load and cache the
     * layout resources and View classes, so the first real open is instant.
     */
    private void prewarmPickers() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        // Short delay so we don't compete with the initial layout pass
        mainHandler.postDelayed(() -> {
            try {
                // Pre-warm DatePickerDialog (calendar mode)
                android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(
                        this, null, 2026, 6, 27);
                dpd.create();
                dpd.dismiss();
            } catch (Exception e) {
                Log.w(TAG, "DatePicker prewarm failed: " + e.getMessage());
            }

            try {
                // Pre-warm TimePickerDialog (spinner mode — is24HourView=true
                // matches the app's usage)
                android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(
                        this, null, 12, 0, true);
                tpd.create();
                tpd.dismiss();
            } catch (Exception e) {
                Log.w(TAG, "TimePicker prewarm failed: " + e.getMessage());
            }

            Log.d(TAG, "Picker pre-warm complete");
        }, 800); // 800ms after onCreate — UI is on screen by then
    }
}
