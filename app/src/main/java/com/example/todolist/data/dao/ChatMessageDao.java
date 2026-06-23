package com.example.todolist.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.todolist.data.entity.ChatMessage;
import java.util.List;

@Dao
public interface ChatMessageDao {

    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY timestamp ASC")
    List<ChatMessage> getBySession(String sessionId);

    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    List<ChatMessage> getRecentBySession(String sessionId, int limit);

    @Insert
    long insert(ChatMessage message);

    @Delete
    void delete(ChatMessage message);

    @Query("DELETE FROM chat_message WHERE session_id = :sessionId")
    void deleteBySession(String sessionId);

    @Query("SELECT COUNT(*) FROM chat_message WHERE session_id = :sessionId")
    int countBySession(String sessionId);
}
