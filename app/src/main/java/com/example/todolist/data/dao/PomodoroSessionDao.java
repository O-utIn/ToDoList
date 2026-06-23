package com.example.todolist.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.todolist.data.entity.PomodoroSession;
import java.util.List;

@Dao
public interface PomodoroSessionDao {
    @Insert
    long insert(PomodoroSession session);

    @Update
    int update(PomodoroSession session);

    @Query("SELECT * FROM pomodoro_session WHERE task_id = :taskId ORDER BY start_time DESC")
    List<PomodoroSession> getByTaskId(long taskId);

    /** Count completed sessions for a user. */
    @Query("SELECT COUNT(*) FROM pomodoro_session WHERE completed = 1 AND (user_id = :userId OR user_id = '')")
    int getTotalCompletedCountForUser(String userId);

    /** Global count (for stats). */
    @Query("SELECT COUNT(*) FROM pomodoro_session WHERE completed = 1")
    int getTotalCompletedCount();

    @Query("SELECT * FROM pomodoro_session WHERE end_time IS NULL LIMIT 1")
    PomodoroSession getRunningSession();
}
