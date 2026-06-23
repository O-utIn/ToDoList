package com.example.todolist.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Records a daily check-in for a habit.
 * Unique constraint on (habitId + dateStamp) ensures one check-in per habit per day.
 */
@Entity(
    tableName = "habit_checkin",
    indices = {@Index(value = {"habitId", "dateStamp"}, unique = true)}
)
public class HabitCheckin {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Foreign key reference to HabitItem.id */
    private long habitId;

    /** Date represented as yyyyMMdd long (e.g., 20260606). */
    private long dateStamp;

    private boolean isCompleted = true;

    // --- Constructors ---
    public HabitCheckin() {}

    public HabitCheckin(long habitId, long dateStamp, boolean isCompleted) {
        this.habitId = habitId;
        this.dateStamp = dateStamp;
        this.isCompleted = isCompleted;
    }

    // --- Getters & Setters ---

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getHabitId() { return habitId; }
    public void setHabitId(long habitId) { this.habitId = habitId; }

    public long getDateStamp() { return dateStamp; }
    public void setDateStamp(long dateStamp) { this.dateStamp = dateStamp; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
