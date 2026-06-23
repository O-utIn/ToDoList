package com.example.todolist.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "pomodoro_session",
    foreignKeys = @ForeignKey(entity = PomodoroTask.class,
        parentColumns = "id", childColumns = "task_id",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index(value = "task_id")})
public class PomodoroSession {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public long task_id;
    public long start_time;
    public Long end_time;
    public int completed; // 0/1

    /** Owner username. Empty string = legacy / not logged in. */
    public String user_id = "";

    public PomodoroSession() {}

    @Ignore
    public PomodoroSession(long task_id, long start_time) {
        this.task_id = task_id;
        this.start_time = start_time;
        this.completed = 0;
    }
}
