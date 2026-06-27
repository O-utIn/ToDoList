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
 * Scoring rules (all local, no network required):
 * 1. Time-slot match (weight 2.0): keyword → preferred slot; no keyword → neutral (0)
 * 2. 7-day completion rate (weight 3.0): high rate → bonus; no history → neutral;
 *    very low rate (neglect) → penalty
 * 3. Streak bonus: 3+ consecutive days → +1.0; 5+ → +2.0
 * 4. Neglect penalty: 7+ days since last check → -2.0; 3+ days → -1.0
 * 5. Already checked today → hard exclude (score = -Infinity)
 *
 * Qualification: score >= 2.0.  Hard cap: top 2 only.
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
        TIME_SLOTS.put("早睡", 2);   TIME_SLOTS.put("放松", 2);
    }
    // 0 = morning (5-11), 1 = afternoon (11-18), 2 = evening (18-24)

    public RecommendationEngine(Context context) {
        this.context = context;
    }

    /**
     * Recommend up to 2 habits for the current time of day.
     * Only habits with score >= 2.0 qualify.
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

            // 1) Hard exclude: already checked today
            HabitCheck todayCheck = AppDatabase.getInstance(context).habitCheckDao()
                .getByHabitAndDate(habit.id, todayStamp, userId);
            if (todayCheck != null) continue;         // already done → skip entirely

            double score = 0.0;

            // 2) Time-slot match
            Integer preferredSlot = resolveTimeSlot(habit);
            if (preferredSlot != null && preferredSlot == currentSlot) {
                score += 2.0;                          // matches current time slot
            } else if (preferredSlot != null) {
                score -= 1.0;                          // mismatched slot → mild penalty
            }
            // preferredSlot == null → neutral (0), habit has no time association

            // 3) 7-day completion rate
            double rate = getRecentCompletionRate(habit.id, todayStamp, 7, userId);
            if (rate >= 0.01) {                        // has at least some history
                if (rate >= 0.7) {
                    score += 3.0;                      // strong habit
                } else if (rate >= 0.3) {
                    score += rate * 3.0;               // moderate habit (scaled)
                } else {
                    score -= 1.0;                      // neglected habit (rate < 0.3)
                }
            }
            // No history → neutral (0), don't give free points to new habits

            // 4) Streak bonus: consecutive days completed (counting back from yesterday)
            int streak = getConsecutiveStreak(habit.id, todayStamp, userId);
            if (streak >= 5) {
                score += 2.0;
            } else if (streak >= 3) {
                score += 1.0;
            }

            // 5) Neglect penalty: days since last completed check
            int daysSinceLast = getDaysSinceLastCheck(habit.id, todayStamp, userId);
            if (daysSinceLast >= 7) {
                score -= 2.0;
            } else if (daysSinceLast >= 3) {
                score -= 1.0;
            }

            if (score >= 1.0) {
                scored.add(new ScoredHabit(habit, score));
            }
        }

        // Sort by score descending, then return top-2 hard cap
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        int hardLimit = Math.min(limit, 2);            // NEVER more than 2
        List<HabitItem> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(hardLimit, scored.size()); i++) {
            recommendations.add(scored.get(i).habit);
        }
        return recommendations;
    }

    /** Resolve which time slot a habit is associated with, or null if unknown. */
    private Integer resolveTimeSlot(HabitItem habit) {
        // Exact name match
        Integer slot = TIME_SLOTS.get(habit.name);
        if (slot != null) return slot;
        // Keyword substring match
        if (habit.name != null) {
            for (Map.Entry<String, Integer> entry : TIME_SLOTS.entrySet()) {
                if (habit.name.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;   // unknown → neutral
    }

    private int getCurrentTimeSlot() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 11) return 0;   // morning
        if (hour >= 11 && hour < 18) return 1;  // afternoon
        return 2;                                 // evening
    }

    /** Completion rate over the last {@code days} days (excluding today). */
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
        return total > 0 ? (double) completed / total : 0.0;   // no history → 0
    }

    /**
     * Count consecutive completed days going backwards from yesterday.
     * Returns 0 if yesterday was not completed.
     */
    private int getConsecutiveStreak(Long habitId, long todayStamp, String userId) {
        if (habitId == null) return 0;
        LocalDate today = DateUtils.fromDateStamp(todayStamp);
        int streak = 0;
        for (int i = 1; i <= 30; i++) {           // max 30-day lookback
            LocalDate date = today.minusDays(i);
            long stamp = DateUtils.toDateStamp(date);
            HabitCheck check = AppDatabase.getInstance(context).habitCheckDao()
                .getByHabitAndDate(habitId, stamp, userId);
            if (check != null && check.checked == 1) {
                streak++;
            } else {
                break;                              // streak broken
            }
        }
        return streak;
    }

    /** Days since the most recent completed check (excluding today).
     *  Returns 0 for habits with no history (they are new, not neglected). */
    private int getDaysSinceLastCheck(Long habitId, long todayStamp, String userId) {
        if (habitId == null) return 0;
        LocalDate today = DateUtils.fromDateStamp(todayStamp);
        for (int i = 1; i <= 30; i++) {
            LocalDate date = today.minusDays(i);
            long stamp = DateUtils.toDateStamp(date);
            HabitCheck check = AppDatabase.getInstance(context).habitCheckDao()
                .getByHabitAndDate(habitId, stamp, userId);
            if (check != null && check.checked == 1) return i;
        }
        return 0;   // no history → treat as new, not neglected
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
