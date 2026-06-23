package com.example.todolist.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AiApi {
    @POST("/parse")
    Call<AiParseResponse> parseTask(@Body AiParseRequest request, @Header("Authorization") String auth);
}
