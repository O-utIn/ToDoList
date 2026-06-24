package com.example.todolist.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.todolist.data.dao.HabitDao;
import com.example.todolist.data.dao.HabitCheckDao;
import com.example.todolist.data.dao.TodoDao;
import com.example.todolist.data.dao.PomodoroTaskDao;
import com.example.todolist.data.dao.PomodoroSessionDao;
import com.example.todolist.data.dao.ChatMessageDao;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.data.entity.HabitCheck;
import com.example.todolist.data.entity.PomodoroTask;
import com.example.todolist.data.entity.PomodoroSession;
import com.example.todolist.data.entity.ChatMessage;

@Database(entities = {
    TodoItem.class,
    HabitItem.class,
    HabitCheck.class,
    PomodoroTask.class,
    PomodoroSession.class,
    ChatMessage.class
}, version = 9, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DB_NAME = "todolist.db";
    private static volatile AppDatabase INSTANCE;

    public abstract TodoDao todoDao();
    public abstract HabitDao habitDao();
    public abstract HabitCheckDao habitCheckDao();
    public abstract PomodoroTaskDao pomodoroTaskDao();
    public abstract PomodoroSessionDao pomodoroSessionDao();
    public abstract ChatMessageDao chatMessageDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DB_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void resetInstance() {
        INSTANCE = null;
    }
}
