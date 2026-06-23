package com.example.todolist;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Dedicated settings page for Pomodoro timer duration configuration.
 */
public class PomodoroSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_settings);

        EditText editWork = findViewById(R.id.edit_work_duration);
        EditText editShort = findViewById(R.id.edit_short_break);
        EditText editLong = findViewById(R.id.edit_long_break);
        EditText editCycles = findViewById(R.id.edit_cycles);
        Button btnSave = findViewById(R.id.btn_save);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        editWork.setText(String.valueOf(prefs.getInt("pomodoro_length", 25)));
        editShort.setText(String.valueOf(prefs.getInt("short_break", 5)));
        editLong.setText(String.valueOf(prefs.getInt("long_break", 15)));
        editCycles.setText(String.valueOf(prefs.getInt("cycles_before_long", 4)));

        btnSave.setOnClickListener(v -> {
            try {
                int work = Integer.parseInt(editWork.getText().toString());
                int shortBrk = Integer.parseInt(editShort.getText().toString());
                int longBrk = Integer.parseInt(editLong.getText().toString());
                int cycles = Integer.parseInt(editCycles.getText().toString());

                if (work <= 0 || shortBrk <= 0 || longBrk <= 0 || cycles <= 0) {
                    Toast.makeText(this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
                    return;
                }

                prefs.edit()
                    .putInt("pomodoro_length", work)
                    .putInt("short_break", shortBrk)
                    .putInt("long_break", longBrk)
                    .putInt("cycles_before_long", cycles)
                    .apply();

                Toast.makeText(this, getString(R.string.durations_saved), Toast.LENGTH_SHORT).show();
                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
