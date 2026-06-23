package com.example.todolist.receiver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Broadcast receiver for forced logout.
 * When the FORCE_OFFLINE broadcast is received, shows an un-dismissable dialog
 * and redirects the user to the login/lock screen.
 */
public class ForceOfflineReceiver extends BroadcastReceiver {

    public static final String ACTION_FORCE_OFFLINE = "com.example.todolist.FORCE_OFFLINE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_FORCE_OFFLINE.equals(intent.getAction())) return;

        // Show dialog on main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            // Clear app_unlocked so user must re-enter password
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("app_unlocked", false).apply();

            // Show alert
            new AlertDialog.Builder(context)
                .setTitle("账号安全")
                .setMessage("您的账号已在其他设备登录或已失效，请重新登录。")
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    // Broadcast handled — activity will finish via the lock check
                    dialog.dismiss();
                })
                .show();
        });
    }
}
