package com.example.todolist.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * DeepSeek / OpenAI-compatible chat completion API.
 */
public interface ChatApi {

    @POST("chat/completions")
    Call<ChatResponse> chat(
            @Header("Authorization") String auth,
            @Body ChatRequest request
    );
}
