package com.example.todolist.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_message")
public class ChatMessage {

    public static final int TYPE_SENT = 1;
    public static final int TYPE_RECEIVED = 0;

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @NonNull
    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "type")
    public int type; // 0=received, 1=sent

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    public ChatMessage(@NonNull String content, int type, long timestamp, @NonNull String sessionId) {
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.sessionId = sessionId;
    }
}
