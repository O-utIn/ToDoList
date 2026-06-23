package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper to read the currently logged-in user from SharedPreferences.
 * Returns empty string if no user is logged in.
 */
public class UserSession {

    /**
     * Get the current user ID (username). Returns "" if not logged in.
     */
    public static String getCurrentUser(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String user = prefs.getString("logged_in_user", "");
        return user != null ? user : "";
    }
}
