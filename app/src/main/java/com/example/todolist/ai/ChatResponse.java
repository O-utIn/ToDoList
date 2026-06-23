package com.example.todolist.ai;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response from DeepSeek / OpenAI-compatible chat completion API.
 */
public class ChatResponse {

    public String id;

    public List<Choice> choices;

    public static class Choice {
        public int index;

        public Message message;

        @SerializedName("finish_reason")
        public String finishReason;
    }

    public static class Message {
        public String role;

        public String content;
    }

    /** Convenience: extract the first choice's message content. */
    public String getContent() {
        if (choices != null && !choices.isEmpty()
                && choices.get(0) != null
                && choices.get(0).message != null) {
            return choices.get(0).message.content;
        }
        return null;
    }
}
