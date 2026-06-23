package com.example.todolist.ai;

import android.content.Context;
import android.util.Log;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.data.entity.PomodoroTask;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.util.UserSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and executes commands embedded in AI responses.
 * Commands are wrapped in: [CMD]...[/CMD] tags containing JSON.
 */
public class CommandParser {

    private static final String TAG = "CommandParser";
    private static final Pattern CMD_PATTERN =
            Pattern.compile("\\[CMD\\](.*?)\\[\\/CMD\\]", Pattern.DOTALL);
    private static final Gson gson = new Gson();

    private final AppDatabase db;
    private final String userId;

    public CommandParser(Context ctx) {
        this.db = AppDatabase.getInstance(ctx);
        this.userId = UserSession.getCurrentUser(ctx);
    }

    /**
     * Process an AI response text. Extract and execute any commands, return
     * a {@link Result} containing the cleaned text and execution summary.
     */
    public Result process(String aiText) {
        StringBuilder cleanedText = new StringBuilder(aiText);
        StringBuilder execLog = new StringBuilder();

        Matcher m = CMD_PATTERN.matcher(aiText);
        while (m.find()) {
            String json = m.group(1).trim();
            String outcome = execute(json);
            if (outcome != null) {
                execLog.append(outcome).append("\n");
            }
            // Remove the command block from displayed text
            int start = cleanedText.indexOf(m.group());
            int end = start + m.group().length();
            if (start >= 0) {
                cleanedText.replace(start, end, "");
            }
        }

        String display = cleanedText.toString().trim();
        if (display.isEmpty()) {
            display = execLog.toString().trim();
        }

        return new Result(display, execLog.toString().trim());
    }

    private String execute(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String action = obj.has("action") ? obj.get("action").getAsString() : "";
            if (action.isEmpty()) return null;

            switch (action) {
                case "create_todo":   return execCreateTodo(obj);
                case "delete_todo":   return execDeleteTodo(obj);
                case "complete_todo": return execCompleteTodo(obj);
                case "create_habit":  return execCreateHabit(obj);
                case "delete_habit":  return execDeleteHabit(obj);
                case "create_pomo":   return execCreatePomodoro(obj);
                case "delete_pomo":   return execDeletePomodoro(obj);
                case "list_todos":    return execListTodos();
                case "list_habits":   return execListHabits();
                case "list_pomos":    return execListPomodoros();
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse command error: " + e.getMessage());
            return "❌ 命令执行失败: " + e.getMessage();
        }
    }

    // --- Todo ---

    private String execCreateTodo(JsonObject o) {
        String title = o.has("title") ? o.get("title").getAsString() : "未命名待办";
        String note = o.has("note") ? o.get("note").getAsString() : "";
        int priority = o.has("priority") ? o.get("priority").getAsInt() : 1;

        long due = 0;
        if (o.has("due") && !o.get("due").isJsonNull()) {
            due = parseDateTime(o.get("due").getAsString());
        }

        TodoItem item = new TodoItem(title, note, due, 0, priority);
        item.user_id = userId;
        long id = db.todoDao().insert(item);
        return "✅ 已创建待办 #" + id + "：「" + title + "」优先级" + priority + (due > 0 ? " 截止" + fmtDate(due) : "");
    }

    private String execDeleteTodo(JsonObject o) {
        if (o.has("id")) {
            long id = o.get("id").getAsLong();
            TodoItem t = db.todoDao().getById(id);
            if (t == null) return "❌ 未找到待办 #" + id;
            int deleted = db.todoDao().delete(t);
            Log.d(TAG, "deleteTodo id=" + id + " title=" + t.title + " deleted=" + deleted);
            return deleted > 0 ? "✅ 已删除待办：「" + t.title + "」"
                               : "❌ 删除失败 #" + id + "（数据库操作失败）";
        }
        if (o.has("title")) {
            String title = o.get("title").getAsString();
            String lowerTitle = title.toLowerCase();
            for (TodoItem t : db.todoDao().getByUser(userId)) {
                if (t.title.toLowerCase().contains(lowerTitle)) {
                    int deleted = db.todoDao().delete(t);
                    Log.d(TAG, "deleteTodo byTitle='" + title + "' matched='" + t.title + "' deleted=" + deleted);
                    return deleted > 0 ? "✅ 已删除待办：「" + t.title + "」"
                                       : "❌ 删除失败";
                }
            }
            return "❌ 未找到包含「" + title + "」的待办";
        }
        return "❌ 请指定要删除的待办 ID 或标题";
    }

    private String execCompleteTodo(JsonObject o) {
        if (o.has("id")) {
            long id = o.get("id").getAsLong();
            TodoItem t = db.todoDao().getById(id);
            if (t == null) return "❌ 未找到待办 #" + id;
            t.is_completed = 1;
            db.todoDao().update(t);
            return "✅ 已标记完成：「" + t.title + "」";
        }
        if (o.has("title")) {
            String title = o.get("title").getAsString();
            for (TodoItem t : db.todoDao().getByUser(userId)) {
                if (t.title.contains(title) && t.is_completed == 0) {
                    t.is_completed = 1;
                    db.todoDao().update(t);
                    return "✅ 已标记完成：「" + t.title + "」";
                }
            }
            return "❌ 未找到匹配的未完成待办";
        }
        return "❌ 请指定要完成的待办 ID 或标题";
    }

    // --- Habit ---

    private String execCreateHabit(JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : "新习惯";
        String desc = o.has("desc") ? o.get("desc").getAsString() : "";
        String icon = o.has("icon") ? o.get("icon").getAsString() : "✅";
        String freq = o.has("freq") ? o.get("freq").getAsString() : "每日";
        String color = o.has("color") ? o.get("color").getAsString() : "#FFD54F";
        String timeStr = o.has("time") ? o.get("time").getAsString() : null; // e.g. "08:00" or "8:00"
        String days = o.has("days") ? o.get("days").getAsString() : null;    // e.g. "124" for Mon+Tue+Thu

        // Build schedule_config JSON with optional time and days
        String resolvedDays = ""; // resolved weekdays for "每周" mode
        StringBuilder cfg = new StringBuilder();
        switch (freq) {
            case "每周":
                cfg.append("{\"mode\":\"weekly\"");
                // Parse days: accept digit string ("124") or Chinese ("一二四")
                resolvedDays = parseDays(days, desc);
                if (!resolvedDays.isEmpty()) {
                    cfg.append(",\"days\":\"").append(resolvedDays).append("\"");
                }
                break;
            default:
                cfg.append("{\"mode\":\"daily\"");
                break;
        }
        // Parse time and add to config
        int timeMinutes = parseTimeToMinutes(timeStr);
        if (timeMinutes >= 0) {
            cfg.append(",\"time\":").append(timeMinutes);
        }
        cfg.append("}");
        String scheduleConfig = cfg.toString();

        HabitItem h = new HabitItem(name, desc, icon, freq, color, scheduleConfig, System.currentTimeMillis());
        h.user_id = userId;
        long id = db.habitDao().insert(h);
        Log.d(TAG, "createHabit: name=" + name + " freq=" + freq + " days=" + resolvedDays + " time=" + timeStr + " cfg=" + scheduleConfig + " id=" + id);

        String daysDisplay = resolvedDays.isEmpty() ? "" : " " + daysToChineseDisplay(resolvedDays);
        String timeDisplay = timeMinutes >= 0 ? " " + timeStr + "提醒" : "";
        return id > 0 ? "✅ 已创建习惯 #" + id + "：「" + name + "」" + freq + daysDisplay + timeDisplay + "打卡"
                      : "❌ 创建习惯失败";
    }

    /** Parse "HH:mm" or "H:mm" to minutes since midnight. Returns -1 if invalid. */
    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return -1;
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                if (h >= 0 && h < 24 && m >= 0 && m < 60) {
                    return h * 60 + m;
                }
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    /**
     * Parse days-of-week from the AI-provided string. Accepts two formats:
     *   Digit format:  "124"  → "124" (Mon+Tue+Thu)
     *   Chinese format: "周一、二、四" → "124"
     * Also falls back to scanning the desc field for day names if days is empty.
     * Returns a string of digits 1-7, or empty if nothing recognized.
     */
    private static String parseDays(String days, String desc) {
        // 1) If days is already a pure digit string, validate and return
        if (days != null && !days.isEmpty()) {
            String cleaned = days.replaceAll("[^1-7]", "");
            if (!cleaned.isEmpty()) return cleaned;
            // Try parsing Chinese day names from days param
            String fromChinese = parseChineseDays(days);
            if (!fromChinese.isEmpty()) return fromChinese;
        }
        // 2) Fallback: try to extract days from desc (e.g. "周一、二、四")
        if (desc != null && !desc.isEmpty()) {
            String fromDesc = parseChineseDays(desc);
            if (!fromDesc.isEmpty()) return fromDesc;
        }
        return "";
    }

    /** Parse Chinese day-of-week names from text. Returns digit string or "". */
    private static String parseChineseDays(String text) {
        StringBuilder result = new StringBuilder();
        // Map: Chinese day names to digit
        String[] patterns = {"周一", "周二", "周三", "周四", "周五", "周六", "周日",
                             "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日",
                             "周1", "周2", "周3", "周4", "周5", "周6", "周7"};
        int[] digits =       {1, 2, 3, 4, 5, 6, 7,
                              1, 2, 3, 4, 5, 6, 7,
                              1, 2, 3, 4, 5, 6, 7};
        for (int i = 0; i < patterns.length; i++) {
            if (text.contains(patterns[i]) && result.indexOf(String.valueOf(digits[i])) < 0) {
                result.append(digits[i]);
            }
        }
        return result.toString();
    }

    /** Convert digit day string to Chinese display: "124" → "周一、二、四". */
    private static String daysToChineseDisplay(String days) {
        if (days == null || days.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String[] names = {"", "一", "二", "三", "四", "五", "六", "日"};
        for (char c : days.toCharArray()) {
            int d = Character.digit(c, 10);
            if (d >= 1 && d <= 7) {
                if (sb.length() > 0) sb.append("、");
                sb.append(names[d]);
            }
        }
        return "周" + sb.toString();
    }

    private String execDeleteHabit(JsonObject o) {
        if (o.has("id")) {
            long id = o.get("id").getAsLong();
            HabitItem h = db.habitDao().getById(id);
            if (h == null) return "❌ 未找到习惯 #" + id;
            int deleted = db.habitDao().delete(h);
            Log.d(TAG, "deleteHabit id=" + id + " name=" + h.name + " deleted=" + deleted);
            return deleted > 0 ? "✅ 已删除习惯：「" + h.name + "」"
                               : "❌ 删除习惯失败";
        }
        if (o.has("name")) {
            String name = o.get("name").getAsString();
            String lowerName = name.toLowerCase();
            for (HabitItem h : db.habitDao().getByUser(userId)) {
                if (h.name.toLowerCase().contains(lowerName)) {
                    int deleted = db.habitDao().delete(h);
                    return deleted > 0 ? "✅ 已删除习惯：「" + h.name + "」"
                                       : "❌ 删除习惯失败";
                }
            }
            return "❌ 未找到包含「" + name + "」的习惯";
        }
        return "❌ 请指定要删除的习惯 ID 或名称";
    }

    // --- Pomodoro ---

    private String execCreatePomodoro(JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : "专注任务";
        String icon = o.has("icon") ? o.get("icon").getAsString() : "🍅";
        int mins = o.has("minutes") ? o.get("minutes").getAsInt() : 25;

        PomodoroTask p = new PomodoroTask(name, icon, mins, System.currentTimeMillis());
        p.user_id = userId;
        long id = db.pomodoroTaskDao().insert(p);
        Log.d(TAG, "createPomo: name=" + name + " mins=" + mins + " id=" + id);
        return id > 0 ? "✅ 已创建番茄钟任务 #" + id + "：「" + name + "」" + mins + "分钟"
                      : "❌ 创建番茄钟任务失败";
    }

    private String execDeletePomodoro(JsonObject o) {
        if (o.has("id")) {
            long id = o.get("id").getAsLong();
            PomodoroTask p = db.pomodoroTaskDao().getById(id);
            if (p == null) return "❌ 未找到番茄钟任务 #" + id;
            int deleted = db.pomodoroTaskDao().delete(p);
            Log.d(TAG, "deletePomo id=" + id + " name=" + p.name + " deleted=" + deleted);
            return deleted > 0 ? "✅ 已删除番茄钟任务：「" + p.name + "」"
                               : "❌ 删除番茄钟任务失败";
        }
        if (o.has("name")) {
            String name = o.get("name").getAsString();
            String lowerName = name.toLowerCase();
            for (PomodoroTask p : db.pomodoroTaskDao().getByUser(userId)) {
                if (p.name.toLowerCase().contains(lowerName)) {
                    int deleted = db.pomodoroTaskDao().delete(p);
                    return deleted > 0 ? "✅ 已删除番茄钟任务：「" + p.name + "」"
                                       : "❌ 删除番茄钟任务失败";
                }
            }
            return "❌ 未找到匹配的番茄钟任务";
        }
        return "❌ 请指定要删除的番茄钟任务 ID 或名称";
    }

    // --- List queries ---

    private String execListTodos() {
        java.util.List<TodoItem> items = db.todoDao().getByUser(userId);
        if (items.isEmpty()) return "📋 当前没有待办事项";
        StringBuilder sb = new StringBuilder("📋 当前待办列表：\n");
        for (TodoItem t : items) {
            String status = t.is_completed == 1 ? "✅" : "⬜";
            sb.append(status).append(" #").append(t.id).append(" ").append(t.title);
            if (t.due_date > 0) sb.append(" 截止").append(fmtDate(t.due_date));
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String execListHabits() {
        java.util.List<HabitItem> items = db.habitDao().getByUser(userId);
        if (items.isEmpty()) return "📋 当前没有习惯";
        StringBuilder sb = new StringBuilder("📋 当前习惯列表：\n");
        for (HabitItem h : items) {
            sb.append("• #").append(h.id).append(" ").append(h.name)
                    .append(" (").append(h.frequency != null ? h.frequency : "每日").append(")\n");
        }
        return sb.toString().trim();
    }

    private String execListPomodoros() {
        java.util.List<PomodoroTask> items = db.pomodoroTaskDao().getByUser(userId);
        if (items.isEmpty()) return "📋 当前没有番茄钟任务";
        StringBuilder sb = new StringBuilder("📋 番茄钟任务列表：\n");
        for (PomodoroTask p : items) {
            sb.append("• #").append(p.id).append(" ").append(p.name)
                    .append(" ").append(p.duration_minutes).append("分钟\n");
        }
        return sb.toString().trim();
    }

    // --- Helpers ---

    private long parseDateTime(String s) {
        try {
            // Try "yyyy-MM-dd HH:mm" or "yyyy-MM-dd"
            SimpleDateFormat sdf;
            if (s.contains(":")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            Date d = sdf.parse(s);
            return d != null ? d.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String fmtDate(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(ts));
    }

    /** Result of command processing. */
    public static class Result {
        public final String displayText;  // cleaned text to show to user
        public final String execLog;      // execution result for AI context

        Result(String display, String log) {
            this.displayText = display;
            this.execLog = log;
        }
    }
}
