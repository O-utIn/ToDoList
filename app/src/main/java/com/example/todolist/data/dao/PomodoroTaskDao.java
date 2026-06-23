package com.example.todolist.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.todolist.data.entity.PomodoroTask;
import java.util.List;

@Dao
public interface PomodoroTaskDao {
    @Insert
    long insert(PomodoroTask task);

    @Update
    int update(PomodoroTask task);

    @Delete
    int delete(PomodoroTask task);

    @Query("SELECT * FROM pomodoro_task ORDER BY create_time DESC")
    List<PomodoroTask> getAllTasks();

    @Query("SELECT * FROM pomodoro_task WHERE id = :id LIMIT 1")
    PomodoroTask getById(long id);

    /** Get tasks owned by a specific user. */
    @Query("SELECT * FROM pomodoro_task WHERE user_id = :userId OR user_id = '' ORDER BY create_time DESC")
    List<PomodoroTask> getByUser(String userId);
}
