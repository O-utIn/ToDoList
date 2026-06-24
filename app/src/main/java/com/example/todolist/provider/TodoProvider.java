package com.example.todolist.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.util.UserSession;
import java.util.List;

/**
 * ContentProvider that exposes the todo_item table for cross-app data sharing.
 * Supports querying all todos, pending (incomplete) todos, and their counts.
 *
 * Authority: com.example.todolist.provider
 * Paths:
 *   /todos              — all todos for current user
 *   /todos/count        — total count of todos
 *   /todos/pending      — pending (incomplete) todos
 *   /todos/pending/count — count of pending todos
 */
public class TodoProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.todolist.provider";
    private static final int TODOS = 1;
    private static final int TODOS_COUNT = 2;
    private static final int TODOS_PENDING = 3;
    private static final int TODOS_PENDING_COUNT = 4;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // More specific paths must be registered first
        uriMatcher.addURI(AUTHORITY, "todos/pending/count", TODOS_PENDING_COUNT);
        uriMatcher.addURI(AUTHORITY, "todos/pending",       TODOS_PENDING);
        uriMatcher.addURI(AUTHORITY, "todos/count",         TODOS_COUNT);
        uriMatcher.addURI(AUTHORITY, "todos",               TODOS);
    }

    private static final String[] COLUMNS_FULL = {
        "id", "title", "note", "due_date", "is_completed", "priority", "user_id"
    };

    private static final String[] COLUMNS_COUNT = { "count" };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (getContext() == null) return null;

        int match = uriMatcher.match(uri);
        final String userId = UserSession.getCurrentUser(getContext());
        List<TodoItem> todos;

        switch (match) {
            case TODOS_COUNT:
                todos = AppDatabase.getInstance(getContext()).todoDao().getByUser(userId);
                return buildCountCursor(todos.size());

            case TODOS_PENDING_COUNT:
                todos = AppDatabase.getInstance(getContext()).todoDao().getPendingByUser(userId);
                return buildCountCursor(todos.size());

            case TODOS:
                todos = AppDatabase.getInstance(getContext()).todoDao().getByUser(userId);
                return buildTodoCursor(todos);

            case TODOS_PENDING:
                todos = AppDatabase.getInstance(getContext()).todoDao().getPendingByUser(userId);
                return buildTodoCursor(todos);

            default:
                return null;
        }
    }

    private Cursor buildCountCursor(int count) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS_COUNT);
        cursor.addRow(new Object[]{count});
        return cursor;
    }

    private Cursor buildTodoCursor(List<TodoItem> todos) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS_FULL);
        for (TodoItem t : todos) {
            cursor.addRow(new Object[]{
                t.id,
                t.title,
                t.note,
                t.due_date,
                t.is_completed,
                t.priority,
                t.user_id
            });
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);
        if (match == TODOS || match == TODOS_PENDING) {
            return "vnd.android.cursor.dir/vnd.com.example.todolist.todos";
        }
        if (match == TODOS_COUNT || match == TODOS_PENDING_COUNT) {
            return "vnd.android.cursor.item/vnd.com.example.todolist.todos";
        }
        return null;
    }

    @Nullable @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { return null; }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) { return 0; }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) { return 0; }
}
