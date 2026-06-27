package com.example.todolist.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.todolist.data.entity.HabitCheck;
import java.util.List;

@Dao
public interface HabitCheckDao {
    @Insert
    long insert(HabitCheck check);

    @Delete
    int delete(HabitCheck check);

    @Query("SELECT * FROM habit_check WHERE habit_id = :habitId AND date_stamp = :dateStamp AND (user_id = :userId OR user_id = '') LIMIT 1")
    HabitCheck getByHabitAndDate(long habitId, long dateStamp, String userId);

    @Query("SELECT * FROM habit_check WHERE date_stamp = :dateStamp AND (user_id = :userId OR user_id = '')")
    List<HabitCheck> getByDate(long dateStamp, String userId);

    /** Get all check-in records for a specific user (used by backup). */
    @Query("SELECT * FROM habit_check WHERE user_id = :userId")
    List<HabitCheck> getByUser(String userId);
}
