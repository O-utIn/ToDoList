package com.example.todolist.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit_check",
    indices = {@Index(value = {"habit_id", "date_stamp", "user_id"}, unique = true)})
public class HabitCheck {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public long habit_id;
    public long date_stamp; // yyyyMMdd as long
    public int checked; // 0/1

    /** Owner username. Empty string = legacy / not logged in. */
    public String user_id = "";

    public HabitCheck() {}

    @Ignore
    public HabitCheck(long habit_id, long date_stamp, int checked) {
        this.habit_id = habit_id;
        this.date_stamp = date_stamp;
        this.checked = checked;
    }
}
