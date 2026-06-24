package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.todolist.fragments.DiscoverFragment;
import com.example.todolist.fragments.HabitFragment;
import com.example.todolist.fragments.MineFragment;
import com.example.todolist.fragments.PomodoroFragment;
import com.example.todolist.fragments.TodoFragment;
import com.example.todolist.receiver.ForceOfflineReceiver;
import com.example.todolist.util.LocationHelper;
import com.example.todolist.util.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Shared calendar state
    private LocalDate selectedDate;
    private ForceOfflineReceiver forceOfflineReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Safe init of selectedDate with fallback
        try {
            selectedDate = LocalDate.now();
        } catch (Exception e) {
            Log.e(TAG, "LocalDate.now() failed", e);
            selectedDate = java.time.LocalDate.of(2026, 6, 6);
        }

        // Create notification channels (safe)
        try {
            NotificationHelper.createChannels(this);
        } catch (Exception e) {
            Log.e(TAG, "createChannels failed", e);
        }

        // Auto-fetch fresh location on launch (if permitted)
        // Uses LiveData — MineFragment will pick up the result when opened
        try {
            if (LocationHelper.hasPermission(this)) {
                LocationHelper.getInstance(this).requestSingleLocation(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "auto location fetch failed", e);
        }

        // Enforce lock if password set and not unlocked
        try {
            SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            String hash = prefs.getString("password_hash", null);
            boolean unlocked = prefs.getBoolean("app_unlocked", false);
            if (hash != null && !unlocked) {
                Intent i = new Intent(this, LockActivity.class);
                startActivity(i);
            }

            // If "remember password" was off last session, clear the logged-in user on cold start
            boolean rememberPassword = prefs.getBoolean("remember_password", true);
            if (!rememberPassword) {
                prefs.edit().remove("logged_in_user").apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "lock check failed", e);
        }

        // Register force offline receiver (dynamic only)
        try {
            forceOfflineReceiver = new ForceOfflineReceiver();
            IntentFilter filter = new IntentFilter("com.example.todolist.FORCE_OFFLINE");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(forceOfflineReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(forceOfflineReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "register force offline receiver failed", e);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HabitFragment())
                .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_habit) fragment = new HabitFragment();
            else if (id == R.id.nav_todo) fragment = new TodoFragment();
            else if (id == R.id.nav_pomodoro) fragment = new PomodoroFragment();
            else if (id == R.id.nav_discover) fragment = new DiscoverFragment();
            else if (id == R.id.nav_mine) fragment = new MineFragment();
            else fragment = new HabitFragment();

            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (forceOfflineReceiver != null) {
            try { unregisterReceiver(forceOfflineReceiver); } catch (Exception ignored) {}
        }
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }
}
