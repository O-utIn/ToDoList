package com.example.todolist.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.todolist.data.entity.TodoItem;
import java.util.List;

@Dao
public interface TodoDao {
    @Insert
    long insert(TodoItem todo);

    @Update
    int update(TodoItem todo);

    @Delete
    int delete(TodoItem todo);

    @Query("SELECT * FROM todo_item ORDER BY priority DESC, due_date ASC")
    List<TodoItem> getAllTodos();

    @Query("SELECT * FROM todo_item WHERE id = :id LIMIT 1")
    TodoItem getById(long id);

    /** Get todos owned by a specific user. Also includes legacy items (user_id=''). */
    @Query("SELECT * FROM todo_item WHERE user_id = :userId OR user_id = '' ORDER BY priority DESC, due_date ASC")
    List<TodoItem> getByUser(String userId);

    /** Get todo by id scoped to user. */
    @Query("SELECT * FROM todo_item WHERE id = :id AND (user_id = :userId OR user_id = '') LIMIT 1")
    TodoItem getByIdForUser(long id, String userId);

    /** Get pending (incomplete) todos for a specific user, plus legacy items. */
    @Query("SELECT * FROM todo_item WHERE is_completed = 0 AND (user_id = :userId OR user_id = '') ORDER BY priority DESC, due_date ASC")
    List<TodoItem> getPendingByUser(String userId);
}
