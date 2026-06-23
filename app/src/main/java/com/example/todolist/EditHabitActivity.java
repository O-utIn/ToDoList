package com.example.todolist;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.util.UserSession;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class EditHabitActivity extends AppCompatActivity {
    private EditText editName, editDescription, editTime, editDate;
    private Button btnSave, btnModeDaily, btnModeWeekly, btnModeSpecific;
    private LinearLayout containerWeekdays, containerSpecificDate;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private Long habitId = null;
    private String selectedMode = "daily";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_habit);

        // Default title for new habit
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText("✅ 新建习惯");

        editName = findViewById(R.id.edit_name);
        editDescription = findViewById(R.id.edit_description);
        editTime = findViewById(R.id.edit_time);
        editDate = findViewById(R.id.edit_date);
        btnSave = findViewById(R.id.btn_save);

        btnModeDaily = findViewById(R.id.btn_mode_daily);
        btnModeWeekly = findViewById(R.id.btn_mode_weekly);
        btnModeSpecific = findViewById(R.id.btn_mode_specific);

        containerWeekdays = findViewById(R.id.container_weekdays);
        containerSpecificDate = findViewById(R.id.container_specific_date);

        cbMon = findViewById(R.id.cb_mon); cbTue = findViewById(R.id.cb_tue);
        cbWed = findViewById(R.id.cb_wed); cbThu = findViewById(R.id.cb_thu);
        cbFri = findViewById(R.id.cb_fri); cbSat = findViewById(R.id.cb_sat);
        cbSun = findViewById(R.id.cb_sun);

        // --- Mode selection ---
        btnModeDaily.setOnClickListener(v -> selectMode("daily"));
        btnModeWeekly.setOnClickListener(v -> selectMode("weekly"));
        btnModeSpecific.setOnClickListener(v -> selectMode("specific"));

        // Date picker for specific date
        editDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                editDate.setText(String.format("%d-%02d-%02d", y, m + 1, d));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker — show dialog instead of keyboard input
        editTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new android.app.TimePickerDialog(this, (view, h, min) -> {
                editTime.setText(String.format("%02d:%02d", h, min));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        // Default: daily mode
        selectMode("daily");

        btnSave.setOnClickListener(v -> saveHabit());
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        applyModeButtonStyle(btnModeDaily, "daily".equals(mode));
        applyModeButtonStyle(btnModeWeekly, "weekly".equals(mode));
        applyModeButtonStyle(btnModeSpecific, "specific".equals(mode));

        containerWeekdays.setVisibility("weekly".equals(mode) ? View.VISIBLE : View.GONE);
        containerSpecificDate.setVisibility("specific".equals(mode) ? View.VISIBLE : View.GONE);
    }

    /** Selected: solid yellow fill + dark text. Unselected: transparent with visible outline. */
    private void applyModeButtonStyle(Button btn, boolean selected) {
        if (selected) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFC107));
            btn.setTextColor(0xFF212121);
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x00000000));
            btn.setTextColor(0xFF757575);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent() != null && getIntent().hasExtra("habit_id")) {
            habitId = getIntent().getLongExtra("habit_id", -1);
            if (habitId != -1) {
                // Switch title to edit mode
                TextView toolbarTitle = findViewById(R.id.toolbar_title);
                toolbarTitle.setText("✅ 编辑习惯");
                exec.execute(() -> {
                    HabitItem item = AppDatabase.getInstance(getApplicationContext())
                        .habitDao().getById(habitId);
                    if (item != null) {
                        runOnUiThread(() -> loadFromItem(item));
                    }
                });
            }
        }
    }

    private void loadFromItem(HabitItem item) {
        editName.setText(item.name != null ? item.name : "");
        editDescription.setText(item.description != null ? item.description : "");

        // Parse schedule_config JSON
        try {
            JSONObject cfg = new JSONObject(
                item.schedule_config != null ? item.schedule_config : "{\"mode\":\"daily\"}");
            String mode = cfg.optString("mode", "daily");
            selectMode(mode);

            int time = cfg.optInt("time", -1);
            if (time >= 0) {
                int h = time / 60, m = time % 60;
                editTime.setText(String.format("%02d:%02d", h, m));
            }

            if ("weekly".equals(mode)) {
                String days = cfg.optString("days", "");
                cbMon.setChecked(days.contains("1"));
                cbTue.setChecked(days.contains("2"));
                cbWed.setChecked(days.contains("3"));
                cbThu.setChecked(days.contains("4"));
                cbFri.setChecked(days.contains("5"));
                cbSat.setChecked(days.contains("6"));
                cbSun.setChecked(days.contains("7"));
            }

            if ("specific".equals(mode)) {
                long date = cfg.optLong("date", 0);
                if (date > 0) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    editDate.setText(sdf.format(new java.util.Date(date)));
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveHabit() {
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            editName.setError(getString(R.string.required));
            return;
        }
        String desc = editDescription.getText().toString().trim();
        String scheduleConfig = buildScheduleConfig();

        exec.execute(() -> {
            final String userId = UserSession.getCurrentUser(getApplicationContext());
            if (habitId == null) {
                HabitItem item = new HabitItem(name, desc, "", "daily",
                    "#FFD54F", scheduleConfig, System.currentTimeMillis());
                item.user_id = userId;
                AppDatabase.getInstance(getApplicationContext()).habitDao().insert(item);
            } else {
                // Preserve existing fields
                HabitItem existing = AppDatabase.getInstance(getApplicationContext())
                    .habitDao().getById(habitId);
                if (existing == null) {
                    runOnUiThread(this::finish);
                    return;
                }
                existing.name = name;
                existing.description = desc;
                existing.schedule_config = scheduleConfig;
                existing.user_id = userId;
                AppDatabase.getInstance(getApplicationContext()).habitDao().update(existing);
            }
            runOnUiThread(this::finish);
        });
    }

    private String buildScheduleConfig() {
        try {
            JSONObject cfg = new JSONObject();
            cfg.put("mode", selectedMode);

            // Parse time
            String timeStr = editTime.getText().toString().trim();
            if (!timeStr.isEmpty()) {
                String[] parts = timeStr.split(":");
                if (parts.length == 2) {
                    int h = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    cfg.put("time", h * 60 + m);
                }
            }

            if ("weekly".equals(selectedMode)) {
                StringBuilder days = new StringBuilder();
                if (cbMon.isChecked()) days.append("1,");
                if (cbTue.isChecked()) days.append("2,");
                if (cbWed.isChecked()) days.append("3,");
                if (cbThu.isChecked()) days.append("4,");
                if (cbFri.isChecked()) days.append("5,");
                if (cbSat.isChecked()) days.append("6,");
                if (cbSun.isChecked()) days.append("7,");
                if (days.length() > 0) days.setLength(days.length() - 1);
                cfg.put("days", days.toString());
            }

            if ("specific".equals(selectedMode)) {
                String dateStr = editDate.getText().toString().trim();
                if (!dateStr.isEmpty()) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    long date = sdf.parse(dateStr).getTime();
                    cfg.put("date", date);
                }
            }

            return cfg.toString();
        } catch (Exception e) {
            return "{\"mode\":\"daily\"}";
        }
    }
}
