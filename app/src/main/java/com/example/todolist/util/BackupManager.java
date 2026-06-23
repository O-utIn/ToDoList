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
     * Export all todos + habits as JSON.
     * Saves to both:
     *   1) App-private storage (for restore function)
     *   2) Public Downloads folder (for user access)
     *
     * @return Human-readable path for display.
     */
    public static String exportAll(Context ctx) throws Exception {
        AppDatabase db = AppDatabase.getInstance(ctx);
        List<TodoItem> todos = db.todoDao().getAllTodos();
        List<HabitItem> habits = db.habitDao().getAllHabits();

        // Build JSON
        JSONObject root = new JSONObject();
        JSONArray jtodos = new JSONArray();
        for (TodoItem t : todos) {
            JSONObject o = new JSONObject();
            o.put("id", t.id == null ? JSONObject.NULL : t.id);
            o.put("title", t.title);
            o.put("note", t.note);
            o.put("due_date", t.due_date);
            o.put("is_completed", t.is_completed);
            o.put("priority", t.priority);
            jtodos.put(o);
        }
        JSONArray jhabits = new JSONArray();
        for (HabitItem h : habits) {
            JSONObject o = new JSONObject();
            o.put("id", h.id == null ? JSONObject.NULL : h.id);
            o.put("name", h.name);
            o.put("icon_res", h.icon_res);
            o.put("frequency", h.frequency);
            o.put("create_time", h.create_time);
            jhabits.put(o);
        }
        root.put("todos", jtodos);
        root.put("habits", jhabits);

        String jsonContent = root.toString(2);
        String fileName = "todo_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";

        // 1) Always save to app-private storage
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
                // API 29+: Use MediaStore (no permission needed)
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

                // Mark as complete
                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                ctx.getContentResolver().update(uri, values, null, null);

                return "下载/" + fileName;
            } else {
                // API < 29: Write to public Downloads directory
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

    // ---- internal ----

    private static void restoreFromJson(Context ctx, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        AppDatabase db = AppDatabase.getInstance(ctx);

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
                db.todoDao().insert(new TodoItem(title, note, due, is_completed, priority));
            }
        }
        JSONArray jhabits = root.optJSONArray("habits");
        if (jhabits != null) {
            for (int i = 0; i < jhabits.length(); i++) {
                JSONObject o = jhabits.getJSONObject(i);
                String name = o.optString("name", "");
                String icon = o.optString("icon_res", "");
                String freq = o.optString("frequency", "daily");
                long create = o.optLong("create_time", System.currentTimeMillis());
                String userId = o.optString("user_id", "");
                db.habitDao().insert(new HabitItem(name, icon, freq, create));
            }
        }
    }
}
