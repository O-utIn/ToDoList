package com.example.todolist.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service for Pomodoro countdown with phase cycling.
 *
 * State is persisted to SharedPreferences so the UI (Fragment/Activity)
 * can recover display state without needing a direct binding.
 *
 * Phase cycle: WORK → SHORT_BREAK → WORK → ... → WORK → LONG_BREAK → WORK → ...
 */
public class PomodoroService extends Service {

    // --- Actions (intents sent TO the service) ---
    public static final String ACTION_START  = "com.example.todolist.action.START";
    public static final String ACTION_PAUSE  = "com.example.todolist.action.PAUSE";
    public static final String ACTION_RESUME = "com.example.todolist.action.RESUME";
    public static final String ACTION_STOP   = "com.example.todolist.action.STOP";

    // --- Broadcasts (sent BY the service to UI) ---
    public static final String BROADCAST_TICK          = "com.example.todolist.ACTION_POMODORO_TICK";
    public static final String BROADCAST_FINISHED      = "com.example.todolist.ACTION_POMODORO_FINISHED";
    public static final String BROADCAST_PAUSED        = "com.example.todolist.ACTION_POMODORO_PAUSED";
    public static final String BROADCAST_RESUMED       = "com.example.todolist.ACTION_POMODORO_RESUMED";
    public static final String BROADCAST_STOPPED       = "com.example.todolist.ACTION_POMODORO_STOPPED";
    public static final String BROADCAST_PHASE_CHANGED = "com.example.todolist.ACTION_POMODORO_PHASE_CHANGED";

    // --- Phase constants ---
    public static final int PHASE_WORK  = 0;
    public static final int PHASE_SHORT = 1;
    public static final int PHASE_LONG  = 2;

    // --- Prefs keys ---
    private static final String PREFS = "app_prefs";
    private static final String KEY_RUNNING       = "pomodoro_running";
    private static final String KEY_PAUSED        = "pomodoro_paused";
    private static final String KEY_REMAINING     = "pomodoro_remaining";
    private static final String KEY_TOTAL         = "pomodoro_total";
    private static final String KEY_END_TIME      = "pomodoro_end_time";
    private static final String KEY_PHASE         = "pomodoro_phase";
    private static final String KEY_WORK_COUNT    = "pomodoro_work_count";

    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final int NOTIFY_ID = 1;

    // --- Runtime state ---
    private CountDownTimer timer;
    private long remainingMillis;
    private long totalMillis;
    private boolean isPaused;
    private int phase;           // PHASE_WORK / PHASE_SHORT / PHASE_LONG
    private int workCount;       // completed work sessions in this cycle

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. Recover persisted state (survives process death)
        restoreState();

        if (intent == null || intent.getAction() == null) {
            // Service restarted by system — resume timer if it was running
            if (!isPaused && remainingMillis > 0) {
                startForegroundWithNotification();
                startCountdown(remainingMillis);
            }
            return START_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            handleStart(intent);

        } else if (ACTION_PAUSE.equals(action)) {
            handlePause();

        } else if (ACTION_RESUME.equals(action)) {
            handleResume();

        } else if (ACTION_STOP.equals(action)) {
            handleStop();
        }

        return START_STICKY;
    }

    // ──────────────────────────────────────────────
    //  Action handlers
    // ──────────────────────────────────────────────

    private void handleStart(Intent intent) {
        // Stop any existing timer
        cancelTimer();

        long specified = intent.getLongExtra("duration_millis", -1L);
        if (specified > 0) {
            totalMillis = specified;
        } else {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            totalMillis = p.getInt("pomodoro_length", 25) * 60L * 1000L;
        }
        remainingMillis = totalMillis;
        isPaused = false;
        phase = PHASE_WORK;
        workCount = 0;

        persistState();
        startForegroundWithNotification();
        startCountdown(remainingMillis);
    }

    private void handlePause() {
        if (!isTimerActive()) return;
        cancelTimer();
        isPaused = true;
        persistState();
        updateNotification(getPhaseLabel() + " · " + getString(com.example.todolist.R.string.paused));
        sendLocalBroadcast(new Intent(BROADCAST_PAUSED));
    }

    private void handleResume() {
        if (!isPaused) return;
        isPaused = false;
        persistState();
        startForegroundWithNotification();
        startCountdown(remainingMillis);
        sendLocalBroadcast(new Intent(BROADCAST_RESUMED));
    }

    private void handleStop() {
        cancelTimer();
        clearRunningState();
        stopForeground(true);
        stopSelf();
        sendLocalBroadcast(new Intent(BROADCAST_STOPPED));
    }

    // ──────────────────────────────────────────────
    //  Countdown logic
    // ──────────────────────────────────────────────

    private void startCountdown(long millis) {
        cancelTimer();
        remainingMillis = millis;
        totalMillis = millis;  // for this phase
        persistRemaining();

        timer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                // Broadcast tick to UI
                Intent tick = new Intent(BROADCAST_TICK);
                tick.putExtra("millis", remainingMillis);
                tick.putExtra("total_millis", totalMillis);
                sendLocalBroadcast(tick);
                // Update notification
                updateNotification(formatMillis(remainingMillis));
                // Persist cheaply
                persistRemaining();
            }

            @Override
            public void onFinish() {
                advancePhase();
            }
        };
        timer.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Advance to the next phase upon completion of the current one.
     */
    private void advancePhase() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (phase == PHASE_WORK) {
            // Work session completed → increment count and go to break
            int count = p.getInt("pomodoro_count", 0);
            p.edit().putInt("pomodoro_count", count + 1).apply();
            workCount++;

            int cycles = p.getInt("cycles_before_long", 4);
            if (cycles <= 0) cycles = 4;

            if (workCount % cycles == 0) {
                phase = PHASE_LONG;
                totalMillis = p.getInt("long_break", 15) * 60L * 1000L;
            } else {
                phase = PHASE_SHORT;
                totalMillis = p.getInt("short_break", 5) * 60L * 1000L;
            }
        } else {
            // Break finished → go back to work
            phase = PHASE_WORK;
            totalMillis = p.getInt("pomodoro_length", 25) * 60L * 1000L;
        }

        remainingMillis = totalMillis;
        isPaused = false;
        persistState();

        // Notify UI of phase change
        Intent pi = new Intent(BROADCAST_PHASE_CHANGED);
        pi.putExtra("phase", phase);
        pi.putExtra("total_millis", totalMillis);
        sendLocalBroadcast(pi);

        // Update notification and keep counting
        updateNotification(getPhaseLabel() + " · " + formatMillis(remainingMillis));
        startCountdown(remainingMillis);
    }

    /**
     * Send an explicit broadcast scoped to our own package.
     * Avoids implicit broadcast restrictions on API 26+.
     */
    private void sendLocalBroadcast(Intent intent) {
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    // ──────────────────────────────────────────────
    //  State persistence
    // ──────────────────────────────────────────────

    private void restoreState() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean running = p.getBoolean(KEY_RUNNING, false);
        isPaused = p.getBoolean(KEY_PAUSED, false);
        phase = p.getInt(KEY_PHASE, PHASE_WORK);
        workCount = p.getInt(KEY_WORK_COUNT, 0);
        totalMillis = p.getLong(KEY_TOTAL, 25 * 60 * 1000L);

        if (running) {
            long endTime = p.getLong(KEY_END_TIME, 0L);
            long now = System.currentTimeMillis();
            if (!isPaused && endTime > now) {
                remainingMillis = endTime - now;
            } else if (!isPaused && endTime <= now) {
                // Timer expired while process was dead — advance phase
                remainingMillis = 0;
            } else {
                remainingMillis = p.getLong(KEY_REMAINING, 25 * 60 * 1000L);
            }
        } else {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            remainingMillis = sp.getInt("pomodoro_length", 25) * 60L * 1000L;
            totalMillis = remainingMillis;
        }
    }

    private void persistState() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, true)
            .putBoolean(KEY_PAUSED, isPaused)
            .putLong(KEY_REMAINING, remainingMillis)
            .putLong(KEY_TOTAL, totalMillis)
            .putLong(KEY_END_TIME, System.currentTimeMillis() + remainingMillis)
            .putInt(KEY_PHASE, phase)
            .putInt(KEY_WORK_COUNT, workCount)
            .apply();
    }

    private void persistRemaining() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putLong(KEY_REMAINING, remainingMillis)
            .putLong(KEY_END_TIME, System.currentTimeMillis() + remainingMillis)
            .apply();
    }

    private void clearRunningState() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, false)
            .putBoolean(KEY_PAUSED, false)
            .remove(KEY_REMAINING)
            .remove(KEY_END_TIME)
            .apply();
    }

    private boolean isTimerActive() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_RUNNING, false);
    }

    // ──────────────────────────────────────────────
    //  Notification
    // ──────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(com.example.todolist.R.string.pomodoro_channel),
                NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void startForegroundWithNotification() {
        Notification n = buildNotification(getPhaseLabel() + " · " + formatMillis(remainingMillis));
        startForeground(NOTIFY_ID, n);
    }

    private void updateNotification(String text) {
        Notification n = buildNotification(text);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFY_ID, n);
    }

    private Notification buildNotification(String text) {
        // Pause/Resume action
        Intent pauseIntent = new Intent(this, PomodoroService.class);
        pauseIntent.setAction(isPaused ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent piPause = PendingIntent.getService(this, 2, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Stop action
        Intent stopIntent = new Intent(this, PomodoroService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String pauseLabel = isPaused
            ? getString(com.example.todolist.R.string.resume)
            : getString(com.example.todolist.R.string.pause);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getPhaseLabel())
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .addAction(new NotificationCompat.Action(0, pauseLabel, piPause))
            .addAction(new NotificationCompat.Action(0, getString(com.example.todolist.R.string.stop), piStop));

        // Progress bar
        int max = (int) (totalMillis / 1000);
        int progress = max - (int) (remainingMillis / 1000);
        b.setProgress(max, Math.max(0, Math.min(max, progress)), false);

        return b.build();
    }

    // ──────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────

    private String getPhaseLabel() {
        switch (phase) {
            case PHASE_SHORT: return getString(com.example.todolist.R.string.short_break_text);
            case PHASE_LONG:  return getString(com.example.todolist.R.string.long_break_text);
            default:          return getString(com.example.todolist.R.string.work);
        }
    }

    private String formatMillis(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
