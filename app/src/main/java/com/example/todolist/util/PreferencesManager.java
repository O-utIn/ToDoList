package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centralized SharedPreferences access for all app settings.
 */
public class PreferencesManager {

    private static final String PREFS_NAME = "app_prefs";
    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Password Lock ---
    public String getPasswordHash() { return prefs.getString("password_hash", null); }
    public void setPasswordHash(String hash) { prefs.edit().putString("password_hash", hash).apply(); }
    public boolean isAppUnlocked() { return prefs.getBoolean("app_unlocked", false); }
    public void setAppUnlocked(boolean unlocked) { prefs.edit().putBoolean("app_unlocked", unlocked).apply(); }

    // --- Pomodoro Settings ---
    public int getPomodoroLength() { return prefs.getInt("pomodoro_length", 25); }
    public void setPomodoroLength(int minutes) { prefs.edit().putInt("pomodoro_length", minutes).apply(); }
    public int getShortBreak() { return prefs.getInt("short_break", 5); }
    public void setShortBreak(int minutes) { prefs.edit().putInt("short_break", minutes).apply(); }
    public int getLongBreak() { return prefs.getInt("long_break", 15); }
    public void setLongBreak(int minutes) { prefs.edit().putInt("long_break", minutes).apply(); }
    public int getCyclesBeforeLong() { return prefs.getInt("cycles_before_long", 4); }
    public void setCyclesBeforeLong(int cycles) { prefs.edit().putInt("cycles_before_long", cycles).apply(); }
    public int getPomodoroCount() { return prefs.getInt("pomodoro_count", 0); }
    public void incrementPomodoroCount() { prefs.edit().putInt("pomodoro_count", getPomodoroCount() + 1).apply(); }

    // --- AI Settings ---
    public String getAiApiUrl() { return prefs.getString("ai_api_url", ""); }
    public void setAiApiUrl(String url) { prefs.edit().putString("ai_api_url", url).apply(); }
    public String getAiApiKey() { return prefs.getString("ai_api_key", ""); }
    public void setAiApiKey(String key) { prefs.edit().putString("ai_api_key", key).apply(); }
    public String getAiSystemPrompt() { return prefs.getString("ai_system_prompt", ""); }
    public void setAiSystemPrompt(String prompt) { prefs.edit().putString("ai_system_prompt", prompt).apply(); }

    // --- Password Lock Enabled ---
    public boolean isPasswordLockEnabled() { return prefs.getBoolean("password_lock_enabled", false); }
    public void setPasswordLockEnabled(boolean enabled) { prefs.edit().putBoolean("password_lock_enabled", enabled).apply(); }

    // --- Login History (for autocomplete) ---
    public java.util.Set<String> getLoginHistory() {
        return prefs.getStringSet("login_history", new java.util.LinkedHashSet<>());
    }
    public void addLoginHistory(String username) {
        java.util.Set<String> history = new java.util.LinkedHashSet<>(getLoginHistory());
        history.remove(username); // remove then re-add to put most recent last
        history.add(username);
        prefs.edit().putStringSet("login_history", history).apply();
    }

    // --- Remember Password Toggle ---
    public boolean isRememberPasswordEnabled() { return prefs.getBoolean("remember_password", true); }
    public void setRememberPasswordEnabled(boolean enabled) { prefs.edit().putBoolean("remember_password", enabled).apply(); }

    // --- Avatar ---
    public String getAvatar(String username) {
        return prefs.getString("avatar_" + username, "👤");
    }
    public void setAvatar(String username, String avatar) {
        prefs.edit().putString("avatar_" + username, avatar).apply();
    }

    // --- Daily Reminder ---
    public int getReminderHour() { return prefs.getInt("reminder_hour", 8); }
    public void setReminderHour(int hour) { prefs.edit().putInt("reminder_hour", hour).apply(); }
    public int getReminderMinute() { return prefs.getInt("reminder_minute", 0); }
    public void setReminderMinute(int minute) { prefs.edit().putInt("reminder_minute", minute).apply(); }
    public boolean isReminderEnabled() { return prefs.getBoolean("reminder_enabled", false); }
    public void setReminderEnabled(boolean enabled) { prefs.edit().putBoolean("reminder_enabled", enabled).apply(); }

    public SharedPreferences getSharedPreferences() { return prefs; }
}
