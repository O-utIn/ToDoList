package com.example.todolist.ai;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiClient {
    private static AiApi api;

    public static AiApi getApi(Context ctx) {
        if (api != null) return api;
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String base = prefs.getString("ai_api_url", "");
        if (base == null) base = "";
        base = base.trim();
        if (base.isEmpty()) {
            return null;
        }
        if (!base.endsWith("/")) base = base + "/";
        OkHttpClient ok = new OkHttpClient.Builder().build();
        Retrofit r = new Retrofit.Builder()
                .baseUrl(base)
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = r.create(AiApi.class);
        return api;
    }
}
