package com.example.todolist.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.PomodoroTimerActivity;
import com.example.todolist.R;
import com.example.todolist.adapter.PomodoroAdapter;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.PomodoroTask;
import com.example.todolist.service.PomodoroService;
import com.example.todolist.util.DateUtils;
import com.example.todolist.util.UserSession;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PomodoroFragment extends Fragment {

    private static final String PREFS = "app_prefs";

    private PomodoroAdapter taskAdapter;
    private TextView tvTimer, tvPhase, tvStatusLabel;
    private Button btnPause, btnStop;
    private LinearLayout layoutTimerActions;
    private ExecutorService exec = Executors.newSingleThreadExecutor();

    private boolean isPaused;
    private boolean isRunning;
    private int currentPhase = PomodoroService.PHASE_WORK;
    private long currentTotalMillis;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        tvTimer = v.findViewById(R.id.text_timer);
        tvPhase = v.findViewById(R.id.text_phase);
        tvStatusLabel = v.findViewById(R.id.tv_timer_status_label);
        btnPause = v.findViewById(R.id.btn_pause);
        btnStop = v.findViewById(R.id.btn_stop);
        layoutTimerActions = v.findViewById(R.id.layout_timer_actions);

        // Recover state from prefs
        syncStateFromPrefs();

        // Tap timer area → full-screen
        v.findViewById(R.id.layout_timer_status).setOnClickListener(view -> {
            openFullScreenTimer();
        });

        btnPause.setOnClickListener(view -> togglePause());
        btnStop.setOnClickListener(view -> stopTimer());

        // Settings button
        v.findViewById(R.id.btn_settings).setOnClickListener(view -> {
            Intent i = new Intent(getContext(), com.example.todolist.PomodoroSettingsActivity.class);
            startActivity(i);
        });

        // Task list
        RecyclerView tasksRv = v.findViewById(R.id.recycler_tasks);
        tasksRv.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new PomodoroAdapter();
        tasksRv.setAdapter(taskAdapter);

        taskAdapter.setOnTaskClickListener(task -> {
            Intent i = new Intent(getContext(), PomodoroTimerActivity.class);
            i.putExtra("task_name", task.name);
            i.putExtra("task_duration", task.duration_minutes);
            startActivity(i);
        });

        taskAdapter.setOnMoreClickListener((task, anchor) -> showTaskOptions(task));
        v.findViewById(R.id.btn_add_task).setOnClickListener(view -> showAddTaskDialog());

        loadTasks();
        return v;
    }

    private void openFullScreenTimer() {
        if (getContext() == null) return;
        SharedPreferences p = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int minutes = p.getInt("pomodoro_length", 25);
        Intent i = new Intent(getContext(), PomodoroTimerActivity.class);
        i.putExtra("task_duration", minutes);
        startActivity(i);
    }

    // ──────────────────────────────────────────────
    //  State sync from SharedPreferences
    // ──────────────────────────────────────────────

    private void syncStateFromPrefs() {
        if (getContext() == null) return;
        SharedPreferences p = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        isRunning = p.getBoolean("pomodoro_running", false);
        isPaused = p.getBoolean("pomodoro_paused", false);
        currentPhase = p.getInt("pomodoro_phase", PomodoroService.PHASE_WORK);
        currentTotalMillis = p.getLong("pomodoro_total", p.getInt("pomodoro_length", 25) * 60L * 1000L);

        if (isRunning) {
            long remaining = p.getLong("pomodoro_remaining", currentTotalMillis);
            tvTimer.setText(DateUtils.formatMillis(remaining));
            tvStatusLabel.setText(getPhaseLabel(currentPhase));
            layoutTimerActions.setVisibility(View.VISIBLE);
            if (isPaused) {
                tvPhase.setText("已暂停");
                btnPause.setText(getString(R.string.resume));
            } else {
                tvPhase.setText(getPhaseLabel(currentPhase));
                btnPause.setText(getString(R.string.pause));
            }
        } else {
            int minutes = p.getInt("pomodoro_length", 25);
            tvTimer.setText(String.format("%02d:00", minutes));
            tvStatusLabel.setText("番茄钟");
            tvPhase.setText("点击进入专注模式");
            btnPause.setText(getString(R.string.pause));
            layoutTimerActions.setVisibility(View.GONE);
        }
    }

    // ──────────────────────────────────────────────
    //  Inline timer control (only when timer running in background)
    // ──────────────────────────────────────────────

    private void togglePause() {
        if (getContext() == null) return;
        Intent i = new Intent(getContext(), PomodoroService.class);
        i.setAction(isPaused ? PomodoroService.ACTION_RESUME : PomodoroService.ACTION_PAUSE);
        getContext().startService(i);
    }

    private void stopTimer() {
        if (getContext() == null) return;
        Intent i = new Intent(getContext(), PomodoroService.class);
        i.setAction(PomodoroService.ACTION_STOP);
        getContext().startService(i);

        isRunning = false;
        isPaused = false;
        syncStateFromPrefs();
    }

    // ──────────────────────────────────────────────
    //  Task management
    // ──────────────────────────────────────────────

    private void showAddTaskDialog() {
        showTaskEditDialog(null, -1);
    }

    private void showTaskEditDialog(@Nullable PomodoroTask existingTask, int defaultMinutes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_pomodoro_task, null);
        EditText editName = dialogView.findViewById(R.id.edit_task_name);
        EditText editCustom = dialogView.findViewById(R.id.edit_custom_minutes);

        TextView chip25 = dialogView.findViewById(R.id.chip_25);
        TextView chip30 = dialogView.findViewById(R.id.chip_30);
        TextView chip45 = dialogView.findViewById(R.id.chip_45);
        TextView chip60 = dialogView.findViewById(R.id.chip_60);
        TextView[] chips = {chip25, chip30, chip45, chip60};
        int[] chipValues = {25, 30, 45, 60};

        final int[] selectedMinutes = {defaultMinutes > 0 ? defaultMinutes : 25};

        for (int i = 0; i < chips.length; i++) {
            final int minutes = chipValues[i];
            TextView chip = chips[i];
            chip.setSelected(minutes == selectedMinutes[0]);
            chip.setOnClickListener(v -> {
                selectedMinutes[0] = minutes;
                editCustom.setText("");
                for (TextView c : chips) c.setSelected(c == chip);
            });
        }

        boolean isEdit = existingTask != null;
        if (isEdit) {
            editName.setText(existingTask.name);
            boolean foundChip = false;
            for (int i = 0; i < chips.length; i++) {
                if (chipValues[i] == existingTask.duration_minutes) {
                    chips[i].setSelected(true);
                    selectedMinutes[0] = chipValues[i];
                    foundChip = true;
                } else {
                    chips[i].setSelected(false);
                }
            }
            if (!foundChip) {
                editCustom.setText(String.valueOf(existingTask.duration_minutes));
                selectedMinutes[0] = existingTask.duration_minutes;
            }
        }

        String title = isEdit ? "编辑专注任务" : "新建专注任务";
        builder.setView(dialogView)
            .setTitle(title)
            .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                String name = editName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "请输入任务名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                String customText = editCustom.getText().toString().trim();
                int duration = customText.isEmpty() ? selectedMinutes[0] : Integer.parseInt(customText);
                if (duration <= 0) duration = 25;

                final int finalDuration = duration;
                exec.execute(() -> {
                    if (isEdit) {
                        existingTask.name = name;
                        existingTask.duration_minutes = finalDuration;
                        AppDatabase.getInstance(getContext()).pomodoroTaskDao().update(existingTask);
                    } else {
                        PomodoroTask task = new PomodoroTask(name, "task", finalDuration, System.currentTimeMillis());
                        task.user_id = UserSession.getCurrentUser(getContext());
                        AppDatabase.getInstance(getContext()).pomodoroTaskDao().insert(task);
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadTasks);
                    }
                });
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void showTaskOptions(PomodoroTask task) {
        new AlertDialog.Builder(getContext())
            .setTitle(task.name)
            .setItems(new String[]{"编辑", "删除"}, (dialog, which) -> {
                if (which == 0) {
                    showTaskEditDialog(task, task.duration_minutes);
                } else if (which == 1) {
                    new AlertDialog.Builder(getContext())
                        .setTitle("删除任务")
                        .setMessage("确定要删除「" + task.name + "」吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            exec.execute(() -> {
                                AppDatabase.getInstance(getContext()).pomodoroTaskDao().delete(task);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(this::loadTasks);
                                }
                            });
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                }
            })
            .show();
    }

    private void loadTasks() {
        exec.execute(() -> {
            if (getContext() == null) return;
            final String userId = UserSession.getCurrentUser(getContext());
            List<PomodoroTask> tasks = AppDatabase.getInstance(getContext()).pomodoroTaskDao().getByUser(userId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> taskAdapter.setData(tasks));
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Broadcast receiver
    // ──────────────────────────────────────────────

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            View root = getView();
            if (root == null) return;

            String act = intent.getAction();

            if (PomodoroService.BROADCAST_TICK.equals(act)) {
                long millis = intent.getLongExtra("millis", 0L);
                currentTotalMillis = intent.getLongExtra("total_millis", currentTotalMillis);
                tvTimer.setText(DateUtils.formatMillis(millis));

            } else if (PomodoroService.BROADCAST_PAUSED.equals(act)) {
                isPaused = true;
                btnPause.setText(getString(R.string.resume));
                tvPhase.setText("已暂停");

            } else if (PomodoroService.BROADCAST_RESUMED.equals(act)) {
                isPaused = false;
                btnPause.setText(getString(R.string.pause));
                tvPhase.setText(getPhaseLabel(currentPhase));

            } else if (PomodoroService.BROADCAST_STOPPED.equals(act)) {
                isRunning = false;
                isPaused = false;
                syncStateFromPrefs();

            } else if (PomodoroService.BROADCAST_PHASE_CHANGED.equals(act)) {
                currentPhase = intent.getIntExtra("phase", PomodoroService.PHASE_WORK);
                currentTotalMillis = intent.getLongExtra("total_millis", currentTotalMillis);
                tvPhase.setText(getPhaseLabel(currentPhase));
                tvStatusLabel.setText(getPhaseLabel(currentPhase));

            } else if (PomodoroService.BROADCAST_FINISHED.equals(act)) {
                tvTimer.setText("00:00");
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        syncStateFromPrefs();
        loadTasks();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null) {
            IntentFilter f = new IntentFilter();
            f.addAction(PomodoroService.BROADCAST_TICK);
            f.addAction(PomodoroService.BROADCAST_FINISHED);
            f.addAction(PomodoroService.BROADCAST_PAUSED);
            f.addAction(PomodoroService.BROADCAST_RESUMED);
            f.addAction(PomodoroService.BROADCAST_STOPPED);
            f.addAction(PomodoroService.BROADCAST_PHASE_CHANGED);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(receiver, f);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null) {
            try { getContext().unregisterReceiver(receiver); } catch (Exception ignored) {}
        }
    }

    private String getPhaseLabel(int phase) {
        switch (phase) {
            case PomodoroService.PHASE_SHORT: return getString(R.string.short_break_text);
            case PomodoroService.PHASE_LONG:  return getString(R.string.long_break_text);
            default:                          return getString(R.string.work);
        }
    }
}
