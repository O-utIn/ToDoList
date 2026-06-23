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
import java.util.List;

/**
 * ContentProvider that exposes the todo_item table for cross-app data sharing.
 * Supports querying incomplete todos count.
 *
 * Authority: com.example.todolist.provider
 * Paths:
 *   /todos          — all todos
 *   /todos/count    — count of todos
 */
public class TodoProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.todolist.provider";
    private static final int TODOS = 1;
    private static final int TODOS_COUNT = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "todos", TODOS);
        uriMatcher.addURI(AUTHORITY, "todos/count", TODOS_COUNT);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int match = uriMatcher.match(uri);
        if (match == TODOS_COUNT) {
            MatrixCursor cursor = new MatrixCursor(new String[]{"count"});
            if (getContext() != null) {
                List<TodoItem> todos = AppDatabase.getInstance(getContext()).todoDao().getAllTodos();
                cursor.addRow(new Object[]{todos.size()});
            } else {
                cursor.addRow(new Object[]{0});
            }
            return cursor;
        } else if (match == TODOS) {
            // Return basic todo data
            MatrixCursor cursor = new MatrixCursor(new String[]{"id", "title", "is_completed"});
            if (getContext() != null) {
                List<TodoItem> todos = AppDatabase.getInstance(getContext()).todoDao().getAllTodos();
                for (TodoItem t : todos) {
                    cursor.addRow(new Object[]{t.id, t.title, t.is_completed});
                }
            }
            return cursor;
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);
        if (match == TODOS) return "vnd.android.cursor.dir/vnd.com.example.todolist.todos";
        if (match == TODOS_COUNT) return "vnd.android.cursor.item/vnd.com.example.todolist.todos";
        return null;
    }

    @Nullable @Override public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) { return null; }
    @Override public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }
}
