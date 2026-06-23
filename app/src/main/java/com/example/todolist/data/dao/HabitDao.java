package com.example.todolist.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.todolist.data.entity.HabitItem;
import java.util.List;

@Dao
public interface HabitDao {
    @Insert
    long insert(HabitItem habit);

    @Update
    int update(HabitItem habit);

    @Delete
    int delete(HabitItem habit);

    @Query("SELECT * FROM habit_item ORDER BY create_time DESC")
    List<HabitItem> getAllHabits();

    @Query("SELECT * FROM habit_item WHERE id = :id LIMIT 1")
    HabitItem getById(long id);

    /** Get habits owned by a specific user. Also includes legacy items (user_id=''). */
    @Query("SELECT * FROM habit_item WHERE user_id = :userId OR user_id = '' ORDER BY create_time DESC")
    List<HabitItem> getByUser(String userId);
}
