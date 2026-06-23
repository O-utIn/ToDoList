package com.example.todolist;

import android.app.Application;

/**
 * Application subclass. Database is initialized lazily by AppDatabase.getInstance().
 */
public class ToDoListApplication extends Application {

    private static ToDoListApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static ToDoListApplication getInstance() {
        return instance;
    }
}
