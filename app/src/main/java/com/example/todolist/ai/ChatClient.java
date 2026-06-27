package com.example.todolist.ai;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit client for DeepSeek chat API.
 * Reads API key and base URL from SharedPreferences, per user.
 * Each account has its own isolated API key and URL.
 */
public class ChatClient {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/";
    private static final int TIMEOUT_SECONDS = 60; // AI responses can be slow

    /** Legacy global key — used for backward compatibility migration only. */
    private static final String LEGACY_KEY = "deepseek_api_key";
    private static final String LEGACY_URL = "deepseek_api_url";

    private static volatile ChatApi instance;
    private static String lastKey;
    private static String lastUrl;
    private static String lastUser;

    // ── User-specific key helpers ──

    /** Returns the user-specific suffix for preference keys. Never null. */
    private static String getUserSuffix(Context ctx) {
        String uid = com.example.todolist.util.UserSession.getCurrentUser(ctx);
        return (uid == null || uid.isEmpty()) ? "default" : uid;
    }

    /** Returns the SharedPreferences key for this user's API key. */
    public static String getKeyPrefName(Context ctx) {
        return "deepseek_api_key_" + getUserSuffix(ctx);
    }

    /** Returns the SharedPreferences key for this user's API URL. */
    public static String getUrlPrefName(Context ctx) {
        return "deepseek_api_url_" + getUserSuffix(ctx);
    }

    /**
     * Read the API key for the current user, with automatic migration
     * from the legacy global key on first access.
     */
    private static String readApiKey(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userKey = getKeyPrefName(ctx);
        String key = prefs.getString(userKey, "").trim();
        if (key.isEmpty()) {
            // Backward compatibility: migrate legacy global key ONLY if this is the
            // sole account (login_history has ≤1 entry). Otherwise new users would
            // inherit another user's key, which is a privacy leak.
            String legacyVal = prefs.getString(LEGACY_KEY, "").trim();
            if (!legacyVal.isEmpty()) {
                java.util.Set<String> history = prefs.getStringSet("login_history", new java.util.LinkedHashSet<>());
                if (history.size() <= 1) {
                    // Safe to migrate: only one user exists
                    key = legacyVal;
                    prefs.edit()
                        .putString(userKey, key)
                        .remove(LEGACY_KEY)
                        .apply();
                }
                // Multiple accounts: key stays "" (empty) — do NOT fall back to legacy
            }
        }
        return key;
    }

    /**
     * Read the API base URL for the current user, with automatic migration
     * from the legacy global URL on first access.
     */
    private static String readApiUrl(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userUrlKey = getUrlPrefName(ctx);
        String url = prefs.getString(userUrlKey, "").trim();
        if (url.isEmpty()) {
            // Backward compatibility: migrate legacy global URL only for single-user
            String legacyUrl = prefs.getString(LEGACY_URL, "").trim();
            if (!legacyUrl.isEmpty()) {
                java.util.Set<String> history = prefs.getStringSet("login_history", new java.util.LinkedHashSet<>());
                if (history.size() <= 1) {
                    // Safe to migrate: only one user exists
                    url = legacyUrl;
                    prefs.edit()
                        .putString(userUrlKey, url)
                        .remove(LEGACY_URL)
                        .apply();
                }
                // Multiple accounts: url stays "" — do NOT fall back to legacy
            }
        }
        if (url.isEmpty()) url = DEFAULT_BASE_URL;
        if (!url.endsWith("/")) url += "/";
        return url;
    }

    // ── Public API ──

    /**
     * Returns a ChatApi instance. Returns null if no API key is configured.
     */
    public static ChatApi getApi(Context ctx) {
        String key = readApiKey(ctx);
        if (key.isEmpty()) return null;

        String url = readApiUrl(ctx);
        String user = getUserSuffix(ctx);

        // Reuse instance if key, url, and user haven't changed
        if (instance != null && key.equals(lastKey) && url.equals(lastUrl) && user.equals(lastUser)) {
            return instance;
        }

        synchronized (ChatClient.class) {
            if (instance != null && key.equals(lastKey) && url.equals(lastUrl) && user.equals(lastUser)) {
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
            lastUser = user;
        }
        return instance;
    }

    /**
     * Get the API key (for use in Authorization header).
     */
    public static String getApiKey(Context ctx) {
        return "Bearer " + readApiKey(ctx);
    }

    /**
     * Check if an API key has been configured for the current user.
     */
    public static boolean isConfigured(Context ctx) {
        return !readApiKey(ctx).isEmpty();
    }

    /**
     * Reset the singleton so the next getApi() reads fresh settings.
     */
    public static void resetInstance() {
        synchronized (ChatClient.class) {
            instance = null;
            lastKey = null;
            lastUrl = null;
            lastUser = null;
        }
    }
}
