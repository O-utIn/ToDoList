package com.example.todolist.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "todo_item")
public class TodoItem {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public String title;
    public String note;
    public long due_date;
    public int is_completed;
    public int priority;

    /** Owner username. Empty string = legacy / not logged in. */
    public String user_id = "";

    public TodoItem() {}

    @Ignore
    public TodoItem(String title, String note, long due_date, int is_completed, int priority) {
        this.title = title;
        this.note = note;
        this.due_date = due_date;
        this.is_completed = is_completed;
        this.priority = priority;
    }
}
