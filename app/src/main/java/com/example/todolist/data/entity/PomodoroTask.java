package com.example.todolist.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_task")
public class PomodoroTask {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public String name;
    public String icon;
    public int duration_minutes;
    public long create_time;

    /** Owner username. Empty string = legacy / not logged in. */
    public String user_id = "";

    public PomodoroTask() {}

    @Ignore
    public PomodoroTask(String name, String icon, int duration_minutes, long create_time) {
        this.name = name;
        this.icon = icon;
        this.duration_minutes = duration_minutes;
        this.create_time = create_time;
    }
}
