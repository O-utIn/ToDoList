package com.example.todolist.ai;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request body for DeepSeek / OpenAI-compatible chat completion API.
 */
public class ChatRequest {

    public String model = "deepseek-chat";

    public List<Message> messages;

    /**
     * DeepSeek enables reasoning/thinking by default.
     * Set to false for the "deepseek-chat" model which doesn't support it in the same way.
     */
    public boolean stream = false;

    public ChatRequest(List<Message> messages) {
        this.messages = messages;
    }

    public static class Message {
        public String role;  // "system" | "user" | "assistant"
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
