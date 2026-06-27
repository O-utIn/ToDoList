package com.example.todolist.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.data.entity.HabitCheck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupManager {

    private static final String TAG = "BackupManager";

    /**
     * Export all of the CURRENT user's data as JSON:
     *   - Todos (all fields)
     *   - Habits (all fields)
     *   - Habit check-in records
     *
     * Saves to both app-private storage (for restore) and public Downloads.
     *
     * @return Human-readable path for display.
     */
    public static String exportAll(Context ctx) throws Exception {
        final String userId = UserSession.getCurrentUser(ctx);
        AppDatabase db = AppDatabase.getInstance(ctx);

        // ── Read current user's LIVE data from database ──
        List<TodoItem> todos = db.todoDao().getByUser(userId);
        List<HabitItem> habits = db.habitDao().getByUser(userId);
        List<HabitCheck> allChecks = db.habitCheckDao().getByUser(userId);

        // ── Build JSON ──
        JSONObject root = new JSONObject();
        root.put("export_version", 2);
        root.put("exported_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        root.put("user", userId);

        // Todos
        JSONArray jtodos = new JSONArray();
        for (TodoItem t : todos) {
            JSONObject o = new JSONObject();
            o.put("id", t.id == null ? JSONObject.NULL : t.id);
            o.put("title", t.title);
            o.put("note", t.note);
            o.put("due_date", t.due_date);
            o.put("is_completed", t.is_completed);
            o.put("priority", t.priority);
            o.put("user_id", t.user_id != null ? t.user_id : "");
            jtodos.put(o);
        }
        root.put("todos", jtodos);

        // Habits (full fields)
        JSONArray jhabits = new JSONArray();
        for (HabitItem h : habits) {
            JSONObject o = new JSONObject();
            o.put("id", h.id == null ? JSONObject.NULL : h.id);
            o.put("name", h.name);
            o.put("description", h.description != null ? h.description : "");
            o.put("icon_res", h.icon_res);
            o.put("frequency", h.frequency);
            o.put("color", h.color != null ? h.color : "#FFD54F");
            o.put("schedule_config", h.schedule_config != null ? h.schedule_config : "{\"mode\":\"daily\"}");
            o.put("create_time", h.create_time);
            o.put("user_id", h.user_id != null ? h.user_id : "");
            jhabits.put(o);
        }
        root.put("habits", jhabits);

        // Habit check-in records
        JSONArray jchecks = new JSONArray();
        for (HabitCheck c : allChecks) {
            JSONObject o = new JSONObject();
            o.put("habit_id", c.habit_id);
            o.put("date_stamp", c.date_stamp);
            o.put("checked", c.checked);
            o.put("user_id", c.user_id != null ? c.user_id : "");
            jchecks.put(o);
        }
        root.put("habit_checks", jchecks);

        String jsonContent = root.toString(2);
        String fileName = "todo_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";

        // 1) Save to app-private storage
        File privateDir = new File(ctx.getFilesDir(), "backups");
        if (!privateDir.exists()) privateDir.mkdirs();
        File privateFile = new File(privateDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(privateFile)) {
            fos.write(jsonContent.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        }
        Log.d(TAG, "Private backup: " + privateFile.getAbsolutePath());

        // 2) Save to public Downloads folder
        String publicPath = saveToPublicDownloads(ctx, fileName, jsonContent);
        Log.d(TAG, "Public backup: " + publicPath);

        return publicPath != null ? publicPath : privateFile.getAbsolutePath();
    }

    /**
     * Save file to public Downloads folder.
     * API 29+ uses MediaStore; API < 29 uses direct file I/O.
     */
    private static String saveToPublicDownloads(Context ctx, String fileName, String content) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = ctx.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return null;

                try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                    if (os == null) return null;
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                ctx.getContentResolver().update(uri, values, null, null);

                return "下载/" + fileName;
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File outFile = new File(downloadsDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(content.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                }
                return outFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "saveToPublicDownloads failed", e);
            return null;
        }
    }

    /**
     * Import from a backup JSON file.
     * Clears the current user's existing data first to avoid duplicates,
     * then restores from the backup file.
     */
    public static void importFromFile(Context ctx, File file) throws Exception {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) { fis.read(data); }
        String s = new String(data, StandardCharsets.UTF_8);
        restoreFromJson(ctx, s);
    }

    /**
     * Import from a content URI (user-picked file via SAF).
     */
    public static void importFromUri(Context ctx, Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
        }
        restoreFromJson(ctx, sb.toString());
    }

    /**
     * List private backup files for restore picker.
     */
    public static File[] listPrivateBackups(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "backups");
        if (!dir.exists()) return new File[0];
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        return files != null ? files : new File[0];
    }

    // ── Internal restore ──

    private static void restoreFromJson(Context ctx, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        final String currentUser = UserSession.getCurrentUser(ctx);
        AppDatabase db = AppDatabase.getInstance(ctx);

        // ---- Clear current user's existing data first (prevents duplicates) ----
        // Delete habit checks for this user
        List<HabitCheck> userChecks = db.habitCheckDao().getByUser(currentUser);
        for (HabitCheck c : userChecks) db.habitCheckDao().delete(c);

        // Delete habits for this user
        List<HabitItem> userHabits = db.habitDao().getByUser(currentUser);
        for (HabitItem h : userHabits) db.habitDao().delete(h);

        // Delete todos for this user
        List<TodoItem> userTodos = db.todoDao().getByUser(currentUser);
        for (TodoItem t : userTodos) db.todoDao().delete(t);

        Log.d(TAG, "Cleared existing data for user '" + currentUser + "': "
            + userTodos.size() + " todos, " + userHabits.size() + " habits, "
            + userChecks.size() + " checks");

        // ---- Restore todos ----
        JSONArray jtodos = root.optJSONArray("todos");
        if (jtodos != null) {
            for (int i = 0; i < jtodos.length(); i++) {
                JSONObject o = jtodos.getJSONObject(i);
                String title = o.optString("title", "");
                String note = o.optString("note", "");
                long due = o.optLong("due_date", 0L);
                int is_completed = o.optInt("is_completed", 0);
                int priority = o.optInt("priority", 1);
                String userId = o.optString("user_id", "");

                TodoItem t = new TodoItem(title, note, due, is_completed, priority);
                // Always assign to current user on restore (ignore source user)
                t.user_id = currentUser;
                db.todoDao().insert(t);
            }
        }

        // ---- Restore habits ----
        // Build a mapping from old habit ID → new habit ID for check-in restoration
        java.util.Map<Long, Long> habitIdMap = new java.util.HashMap<>();

        JSONArray jhabits = root.optJSONArray("habits");
        if (jhabits != null) {
            for (int i = 0; i < jhabits.length(); i++) {
                JSONObject o = jhabits.getJSONObject(i);
                long oldId = o.optLong("id", 0);

                String name = o.optString("name", "");
                String desc = o.optString("description", "");
                String icon = o.optString("icon_res", "");
                String freq = o.optString("frequency", "每日");
                String color = o.optString("color", "#FFD54F");
                String schedCfg = o.optString("schedule_config", "{\"mode\":\"daily\"}");
                long create = o.optLong("create_time", System.currentTimeMillis());

                HabitItem h = new HabitItem(name, desc, icon, freq, color, schedCfg, create);
                h.user_id = currentUser;
                long newId = db.habitDao().insert(h);
                if (oldId > 0) habitIdMap.put(oldId, newId);
            }
        }

        // ---- Restore habit check-ins (map old habit IDs to new ones) ----
        JSONArray jchecks = root.optJSONArray("habit_checks");
        if (jchecks != null) {
            for (int i = 0; i < jchecks.length(); i++) {
                JSONObject o = jchecks.getJSONObject(i);
                long oldHabitId = o.optLong("habit_id", 0);
                long dateStamp = o.optLong("date_stamp", 0);
                int checked = o.optInt("checked", 0);

                // Map old habit ID to new habit ID
                Long newHabitId = habitIdMap.get(oldHabitId);
                if (newHabitId == null) continue; // habit wasn't restored, skip check-in

                HabitCheck c = new HabitCheck(newHabitId, dateStamp, checked);
                c.user_id = currentUser;
                db.habitCheckDao().insert(c);
            }
        }
    }
}
