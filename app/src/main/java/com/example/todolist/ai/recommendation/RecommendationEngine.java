package com.example.todolist.ai.recommendation;

import android.content.Context;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.HabitCheck;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.util.DateUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based local recommendation engine for habits.
 *
 * Scores each habit based on:
 * 1. Time-of-day match (morning → morning habits, evening → evening habits)
 * 2. Historical completion rate (habits with high completion rates get boost)
 * 3. Recent streak (consecutive completions)
 *
 * Returns top 1-2 recommended habits for display with "智能推荐" badge.
 */
public class RecommendationEngine {

    private final Context context;

    // Time-of-day mapping: habit name keywords → preferred time slot
    private static final Map<String, Integer> TIME_SLOTS = new HashMap<>();
    static {
        TIME_SLOTS.put("早起", 0);   TIME_SLOTS.put("跑步", 0);   TIME_SLOTS.put("晨跑", 0);
        TIME_SLOTS.put("早餐", 0);   TIME_SLOTS.put("冥想", 0);   TIME_SLOTS.put("瑜伽", 0);
        TIME_SLOTS.put("阅读", 1);   TIME_SLOTS.put("学习", 1);   TIME_SLOTS.put("工作", 1);
        TIME_SLOTS.put("喝水", 1);   TIME_SLOTS.put("午休", 1);   TIME_SLOTS.put("健身", 1);
        TIME_SLOTS.put("晚餐", 2);   TIME_SLOTS.put("日记", 2);   TIME_SLOTS.put("复盘", 2);
        TIME_SLOTS.put("早睡", 2);   TIME_SLOTS.put("阅读", 2);   TIME_SLOTS.put("放松", 2);
    }
    // 0 = morning (5-11), 1 = afternoon (11-18), 2 = evening (18-24)

    public RecommendationEngine(Context context) {
        this.context = context;
    }

    /**
     * Recommend habits for the current time of day.
     * @param limit max number of recommendations to return
     * @return list of recommended HabitItems
     */
    public List<HabitItem> recommend(int limit) {
        String userId = com.example.todolist.util.UserSession.getCurrentUser(context);
        List<HabitItem> allHabits = AppDatabase.getInstance(context).habitDao().getByUser(userId);
        if (allHabits == null || allHabits.isEmpty()) return new ArrayList<>();

        int currentSlot = getCurrentTimeSlot();
        long todayStamp = DateUtils.toDateStamp(LocalDate.now());

        // Score each habit
        List<ScoredHabit> scored = new ArrayList<>();
        for (HabitItem habit : allHabits) {
            double score = 0.0;

            // Time-of-day match
            Integer preferredSlot = TIME_SLOTS.get(habit.name);
            if (preferredSlot == null) {
                // Try keyword matching
                for (Map.Entry<String, Integer> entry : TIME_SLOTS.entrySet()) {
                    if (habit.name != null && habit.name.contains(entry.getKey())) {
                        preferredSlot = entry.getValue();
                        break;
                    }
                }
            }
            if (preferredSlot == null) preferredSlot = 1; // default to afternoon
            if (preferredSlot == currentSlot) score += 2.0;

            // Completion rate over the last 7 days
            double completionRate = getRecentCompletionRate(habit.id, todayStamp, 7, userId);
            score += completionRate * 3.0;

            // If habit is not yet checked today, give a boost
            HabitCheck todayCheck = AppDatabase.getInstance(context).habitCheckDao()
                .getByHabitAndDate(habit.id, todayStamp, userId);
            if (todayCheck == null) score += 1.0;

            scored.add(new ScoredHabit(habit, score));
        }

        // Sort by score descending
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // Return top N
        List<HabitItem> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            if (scored.get(i).score > 0) {
                recommendations.add(scored.get(i).habit);
            }
        }
        return recommendations;
    }

    private int getCurrentTimeSlot() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 11) return 0;  // morning
        if (hour >= 11 && hour < 18) return 1; // afternoon
        return 2; // evening
    }

    private double getRecentCompletionRate(Long habitId, long todayStamp, int days, String userId) {
        if (habitId == null) return 0.0;
        LocalDate today = DateUtils.fromDateStamp(todayStamp);
        int completed = 0;
        int total = 0;
        for (int i = 1; i <= days; i++) {
            LocalDate date = today.minusDays(i);
            long stamp = DateUtils.toDateStamp(date);
            HabitCheck check = AppDatabase.getInstance(context).habitCheckDao()
                .getByHabitAndDate(habitId, stamp, userId);
            total++;
            if (check != null && check.checked == 1) completed++;
        }
        return total > 0 ? (double) completed / total : 0.5;
    }

    private static class ScoredHabit {
        final HabitItem habit;
        final double score;
        ScoredHabit(HabitItem habit, double score) {
            this.habit = habit;
            this.score = score;
        }
    }
}
