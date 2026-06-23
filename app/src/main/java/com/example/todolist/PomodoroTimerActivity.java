package com.example.todolist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.todolist.service.PomodoroService;
import com.example.todolist.util.DateUtils;

/**
 * Full-screen Pomodoro countdown timer.
 *
 * Single toggle button cycles:  ▶ idle → ⏸ running → ▶ paused → ⏸ running → ...
 * Stop button exits full-screen and stops the service.
 */
public class PomodoroTimerActivity extends AppCompatActivity {

    private static final String PREFS = "app_prefs";

    // States for the toggle button
    private static final int STATE_IDLE   = 0; // not started yet
    private static final int STATE_RUNNING = 1; // counting down
    private static final int STATE_PAUSED  = 2; // paused

    private TextView tvTimer, tvPhase, tvTaskName, tvDurationLabel, tvHint;
    private TextView btnToggle, btnStop;
    private ProgressBar progressBar;

    private int uiState = STATE_IDLE;
    private int currentPhase = PomodoroService.PHASE_WORK;
    private long totalMillis;
    private int taskDurationMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_timer);

        // Edge-to-edge: draw behind status bar, apply top inset
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View rootView = findViewById(R.id.timer_root);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        tvTimer = findViewById(R.id.tv_timer_display);
        tvPhase = findViewById(R.id.tv_timer_phase);
        tvTaskName = findViewById(R.id.tv_timer_task_name);
        tvDurationLabel = findViewById(R.id.tv_timer_duration_label);
        tvHint = findViewById(R.id.tv_timer_hint);
        progressBar = findViewById(R.id.progress_circular);
        btnToggle = findViewById(R.id.btn_timer_toggle);
        btnStop = findViewById(R.id.btn_timer_stop);

        // Intent extras
        String taskName = getIntent().getStringExtra("task_name");
        if (taskName != null) tvTaskName.setText(taskName);
        taskDurationMinutes = getIntent().getIntExtra("task_duration", -1);
        if (taskDurationMinutes <= 0) {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            taskDurationMinutes = p.getInt("pomodoro_length", 25);
        }
        totalMillis = taskDurationMinutes * 60L * 1000L;
        tvTimer.setText(DateUtils.formatMillis(totalMillis));
        tvDurationLabel.setText(taskDurationMinutes + " 分钟");
        progressBar.setMax((int) (totalMillis / 1000));
        progressBar.setProgress(0);

        // Idle state — only toggle visible
        applyUiState(STATE_IDLE);

        // Single toggle button handles: start → pause → resume → pause → ...
        btnToggle.setOnClickListener(v -> {
            switch (uiState) {
                case STATE_IDLE:
                    startTimer();
                    break;
                case STATE_RUNNING:
                    pauseTimer();
                    break;
                case STATE_PAUSED:
                    resumeTimer();
                    break;
            }
        });

        btnStop.setOnClickListener(v -> stopTimer());
    }

    private void applyUiState(int state) {
        uiState = state;
        switch (state) {
            case STATE_IDLE:
                btnToggle.setText("▶");
                btnStop.setVisibility(View.VISIBLE);
                tvHint.setText("点击 ▶ 开始专注  |  ⏹ 退出");
                tvHint.setVisibility(View.VISIBLE);
                break;
            case STATE_RUNNING:
                btnToggle.setText("⏸");
                btnStop.setVisibility(View.VISIBLE);
                tvHint.setVisibility(View.INVISIBLE);  // invisible → keeps height
                break;
            case STATE_PAUSED:
                btnToggle.setText("▶");
                btnStop.setVisibility(View.VISIBLE);
                tvHint.setText("已暂停");
                tvHint.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void startTimer() {
        long millis = taskDurationMinutes * 60L * 1000L;
        totalMillis = millis;
        progressBar.setMax((int) (totalMillis / 1000));
        progressBar.setProgress(0);

        Intent i = new Intent(this, PomodoroService.class);
        i.setAction(PomodoroService.ACTION_START);
        i.putExtra("duration_millis", millis);
        startService(i);

        currentPhase = PomodoroService.PHASE_WORK;
        tvPhase.setText(getString(R.string.work));
        applyUiState(STATE_RUNNING);
    }

    private void pauseTimer() {
        Intent i = new Intent(this, PomodoroService.class);
        i.setAction(PomodoroService.ACTION_PAUSE);
        startService(i);
        applyUiState(STATE_PAUSED);
    }

    private void resumeTimer() {
        Intent i = new Intent(this, PomodoroService.class);
        i.setAction(PomodoroService.ACTION_RESUME);
        startService(i);
        applyUiState(STATE_RUNNING);
    }

    private void stopTimer() {
        // Only stop the service if it's actually running
        if (uiState != STATE_IDLE) {
            Intent i = new Intent(this, PomodoroService.class);
            i.setAction(PomodoroService.ACTION_STOP);
            startService(i);
        }
        finish();
    }

    // ──────────────────────────────────────────────
    //  Broadcast receiver — keep UI in sync with service
    // ──────────────────────────────────────────────

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String act = intent.getAction();

            if (PomodoroService.BROADCAST_TICK.equals(act)) {
                long millis = intent.getLongExtra("millis", 0L);
                totalMillis = intent.getLongExtra("total_millis", totalMillis);
                tvTimer.setText(DateUtils.formatMillis(millis));
                int elapsed = totalMillis > 0 ? (int) ((totalMillis - millis) / 1000) : 0;
                progressBar.setProgress(Math.max(0, Math.min(progressBar.getMax(), elapsed)));

            } else if (PomodoroService.BROADCAST_PAUSED.equals(act)) {
                applyUiState(STATE_PAUSED);
                tvPhase.setText(getString(R.string.paused));

            } else if (PomodoroService.BROADCAST_RESUMED.equals(act)) {
                applyUiState(STATE_RUNNING);
                tvPhase.setText(getPhaseLabel(currentPhase));

            } else if (PomodoroService.BROADCAST_STOPPED.equals(act)) {
                finish();

            } else if (PomodoroService.BROADCAST_PHASE_CHANGED.equals(act)) {
                currentPhase = intent.getIntExtra("phase", PomodoroService.PHASE_WORK);
                totalMillis = intent.getLongExtra("total_millis", totalMillis);
                tvPhase.setText(getPhaseLabel(currentPhase));
                progressBar.setMax((int) (totalMillis / 1000));
                progressBar.setProgress(0);
                applyUiState(STATE_RUNNING);

            } else if (PomodoroService.BROADCAST_FINISHED.equals(act)) {
                tvTimer.setText("00:00");
                progressBar.setProgress(progressBar.getMax());
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(PomodoroService.BROADCAST_TICK);
        f.addAction(PomodoroService.BROADCAST_FINISHED);
        f.addAction(PomodoroService.BROADCAST_PAUSED);
        f.addAction(PomodoroService.BROADCAST_RESUMED);
        f.addAction(PomodoroService.BROADCAST_STOPPED);
        f.addAction(PomodoroService.BROADCAST_PHASE_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, f);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    private String getPhaseLabel(int phase) {
        switch (phase) {
            case PomodoroService.PHASE_SHORT: return getString(R.string.short_break_text);
            case PomodoroService.PHASE_LONG:  return getString(R.string.long_break_text);
            default:                          return getString(R.string.work);
        }
    }
}
