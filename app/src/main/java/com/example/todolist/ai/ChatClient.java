package com.example.todolist.ai;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit client for DeepSeek chat API.
 * Reads API key and base URL from SharedPreferences.
 */
public class ChatClient {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/";
    private static final int TIMEOUT_SECONDS = 60; // AI responses can be slow

    private static volatile ChatApi instance;
    private static String lastKey;
    private static String lastUrl;

    /**
     * Returns a ChatApi instance. Returns null if no API key is configured.
     */
    public static ChatApi getApi(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String key = prefs.getString("deepseek_api_key", "").trim();
        if (key.isEmpty()) return null;

        String url = prefs.getString("deepseek_api_url", DEFAULT_BASE_URL).trim();
        if (url.isEmpty()) url = DEFAULT_BASE_URL;
        if (!url.endsWith("/")) url += "/";

        // Reuse instance if key and url haven't changed
        if (instance != null && key.equals(lastKey) && url.equals(lastUrl)) {
            return instance;
        }

        synchronized (ChatClient.class) {
            if (instance != null && key.equals(lastKey) && url.equals(lastUrl)) {
                return instance;
            }

            OkHttpClient ok = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            Retrofit r = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = r.create(ChatApi.class);
            lastKey = key;
            lastUrl = url;
        }
        return instance;
    }

    /**
     * Get the API key (for use in Authorization header).
     */
    public static String getApiKey(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return "Bearer " + prefs.getString("deepseek_api_key", "").trim();
    }

    /**
     * Check if an API key has been configured.
     */
    public static boolean isConfigured(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        return !prefs.getString("deepseek_api_key", "").trim().isEmpty();
    }

    /**
     * Reset the singleton so the next getApi() reads fresh settings.
     */
    public static void resetInstance() {
        synchronized (ChatClient.class) {
            instance = null;
            lastKey = null;
            lastUrl = null;
        }
    }
}
