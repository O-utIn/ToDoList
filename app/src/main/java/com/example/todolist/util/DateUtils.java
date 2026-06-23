package com.example.todolist.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for date formatting and calendar operations.
 */
public class DateUtils {

    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy年MM月");

    /** Convert a LocalDate to a yyyyMMdd long stamp. */
    public static long toDateStamp(LocalDate date) {
        return Long.parseLong(date.format(STAMP_FMT));
    }

    /** Convert a yyyyMMdd long stamp back to LocalDate. */
    public static LocalDate fromDateStamp(long stamp) {
        return LocalDate.parse(String.valueOf(stamp), STAMP_FMT);
    }

    /** Format a LocalDate as "yyyy年MM月" for display. */
    public static String formatYearMonth(LocalDate date) {
        return date.format(DISPLAY_FMT);
    }

    /** Get the Monday of the week containing the given date. */
    public static LocalDate getMondayOfWeek(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /** Get the display name for a day of week (e.g., "周一"). */
    public static String getDayOfWeekShort(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE);
    }

    /** Get the day-of-month number. */
    public static int getDayOfMonth(LocalDate date) {
        return date.getDayOfMonth();
    }

    /** Check if two LocalDates represent the same calendar day. */
    public static boolean isSameDay(LocalDate a, LocalDate b) {
        return a.equals(b);
    }

    /** Generate the 7 days (Mon-Sun) for the week containing the given date. */
    public static List<LocalDate> getWeekDays(LocalDate date) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate monday = getMondayOfWeek(date);
        for (int i = 0; i < 7; i++) {
            days.add(monday.plusDays(i));
        }
        return days;
    }

    /** Format milliseconds to "MM:SS" string. */
    public static String formatMillis(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Generate a 42-cell grid (6 weeks × 7 days) for a month calendar.
     * Leading/trailing slots are null; actual days are LocalDate instances.
     * @param anyDayInMonth any date within the target month
     */
    public static List<LocalDate> getMonthGrid(LocalDate anyDayInMonth) {
        List<LocalDate> grid = new ArrayList<>(42);
        LocalDate first = anyDayInMonth.withDayOfMonth(1);
        // DayOfWeek: MONDAY=1 … SUNDAY=7. Offset for Monday-first calendar:
        int offset = first.getDayOfWeek().getValue() - 1; // Mon=0, Sun=6
        // Leading blanks
        for (int i = 0; i < offset; i++) grid.add(null);
        // Days of month
        int daysInMonth = first.lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) grid.add(first.withDayOfMonth(d));
        // Trailing blanks to fill 42
        while (grid.size() < 42) grid.add(null);
        return grid;
    }

    /** Format a due date timestamp to a human-readable string. */
    public static String formatDueDate(long timestamp) {
        if (timestamp <= 0) return "";
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        LocalDate date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "今天";
        if (date.equals(today.plusDays(1))) return "明天";
        if (date.equals(today.minusDays(1))) return "昨天";
        return date.format(DateTimeFormatter.ofPattern("MM/dd"));
    }
}
