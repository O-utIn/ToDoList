package com.example.todolist.ai;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiClient {
    private static AiApi api;
    private static String lastUser;

    /** Legacy global key — used for backward compatibility migration only. */
    private static final String LEGACY_URL = "ai_api_url";

    private static String getUserSuffix(Context ctx) {
        String uid = com.example.todolist.util.UserSession.getCurrentUser(ctx);
        return (uid == null || uid.isEmpty()) ? "default" : uid;
    }

    private static String readApiUrl(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String userKey = "ai_api_url_" + getUserSuffix(ctx);
        String base = prefs.getString(userKey, "").trim();
        if (base.isEmpty()) {
            // Backward compatibility: migrate legacy global URL only for single-user
            String legacyUrl = prefs.getString(LEGACY_URL, "").trim();
            if (!legacyUrl.isEmpty()) {
                java.util.Set<String> history = prefs.getStringSet("login_history", new java.util.LinkedHashSet<>());
                if (history.size() <= 1) {
                    // Safe to migrate: only one user exists
                    base = legacyUrl;
                    prefs.edit()
                        .putString(userKey, base)
                        .remove(LEGACY_URL)
                        .apply();
                }
                // Multiple accounts: base stays "" — do NOT fall back to legacy
            }
        }
        return base;
    }

    public static AiApi getApi(Context ctx) {
        String user = getUserSuffix(ctx);
        String base = readApiUrl(ctx);
        if (base.isEmpty()) {
            return null;
        }
        if (!base.endsWith("/")) base = base + "/";

        // Rebuild if user changed
        if (api != null && user.equals(lastUser)) return api;

        synchronized (AiClient.class) {
            if (api != null && user.equals(lastUser)) return api;

            OkHttpClient ok = new OkHttpClient.Builder().build();
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(base)
                    .client(ok)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = r.create(AiApi.class);
            lastUser = user;
        }
        return api;
    }
}
