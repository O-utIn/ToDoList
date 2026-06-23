package com.example.todolist.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.time.LocalDate;

@Entity(tableName = "habit_item")
public class HabitItem {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public String name;
    public String description;
    public String icon_res;
    public String frequency;
    public String color;
    public long create_time;

    /**
     * JSON schedule config. Examples:
     *   {"mode":"daily"}                         — every day, any time
     *   {"mode":"daily","time":480}              — every day at 08:00 (minutes since midnight)
     *   {"mode":"weekly","days":[1,3,5],"time":540} — Mon/Wed/Fri at 09:00
     *   {"mode":"specific","date":1718409600000,"time":840} — specific date at 14:00
     */
    public String schedule_config;

    /** Owner username. Empty string = legacy / not logged in. */
    public String user_id = "";

    public HabitItem() {}

    @Ignore
    public HabitItem(String name, String description, String icon_res, String frequency,
                     String color, String scheduleConfig, long create_time) {
        this.name = name;
        this.description = description;
        this.icon_res = icon_res;
        this.frequency = frequency;
        this.color = color;
        this.schedule_config = scheduleConfig;
        this.create_time = create_time;
    }

    @Ignore
    public HabitItem(String name, String icon_res, String frequency, long create_time) {
        this.name = name;
        this.description = "";
        this.icon_res = icon_res;
        this.frequency = frequency;
        this.color = "#FFD54F";
        this.schedule_config = "{\"mode\":\"daily\"}";
        this.create_time = create_time;
    }

    /**
     * Check whether this habit should appear on the given date based on its schedule_config.
     */
    public boolean isActiveOnDate(LocalDate date) {
        if (schedule_config == null || schedule_config.isEmpty()) return true; // legacy → daily
        try {
            org.json.JSONObject json = new org.json.JSONObject(schedule_config);
            String mode = json.optString("mode", "daily");

            switch (mode) {
                case "daily":
                    return true;
                case "weekly": {
                    String days = json.optString("days", "");
                    if (days.isEmpty()) return true; // no days specified → all days
                    int dow = date.getDayOfWeek().getValue(); // Mon=1 … Sun=7
                    return days.contains(String.valueOf(dow));
                }
                case "specific": {
                    long target = json.optLong("date", 0);
                    if (target <= 0) return true;
                    java.time.LocalDate targetDate = java.time.Instant.ofEpochMilli(target)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                    return date.equals(targetDate);
                }
                default:
                    return true;
            }
        } catch (Exception e) {
            return true; // broken config → treat as daily
        }
    }

    // --- Helper to parse schedule mode ---
    public String getScheduleMode() {
        if (schedule_config == null || schedule_config.isEmpty()) return "daily";
        try {
            org.json.JSONObject json = new org.json.JSONObject(schedule_config);
            return json.optString("mode", "daily");
        } catch (Exception e) {
            return "daily";
        }
    }

    /** Convert day-of-week digits (1=Mon...7=Sun) to Chinese abbreviation. */
    private static String daysToChinese(String days) {
        if (days == null || days.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String[] names = {"", "一", "二", "三", "四", "五", "六", "日"};
        for (char c : days.toCharArray()) {
            try {
                int d = Character.digit(c, 10);
                if (d >= 1 && d <= 7) sb.append(names[d]);
            } catch (Exception ignored) {}
        }
        return sb.toString();
    }

    /** Human-readable schedule summary for display. */
    public String getScheduleSummary() {
        if (schedule_config == null || schedule_config.isEmpty()) return "每日";
        try {
            org.json.JSONObject json = new org.json.JSONObject(schedule_config);
            String mode = json.optString("mode", "daily");
            int time = json.optInt("time", -1);

            String timeStr = "";
            if (time >= 0) {
                int h = time / 60, m = time % 60;
                timeStr = String.format(" %02d:%02d", h, m);
            }

            switch (mode) {
                case "daily":   return "每日" + timeStr;
                case "weekly": {
                    String days = json.optString("days", "");
                    return "每周" + (days.isEmpty() ? "" : " " + daysToChinese(days)) + timeStr;
                }
                case "specific": {
                    long date = json.optLong("date", 0);
                    if (date > 0) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault());
                        return sdf.format(new java.util.Date(date)) + timeStr;
                    }
                    return "指定日期" + timeStr;
                }
                default: return "每日";
            }
        } catch (Exception e) {
            return "每日";
        }
    }
}
