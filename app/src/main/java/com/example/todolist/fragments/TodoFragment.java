package com.example.todolist.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.EditTodoActivity;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.adapter.CalendarMonthAdapter;
import com.example.todolist.adapter.TodoAdapter;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.util.DateUtils;
import com.example.todolist.util.UserSession;
import com.google.android.material.snackbar.Snackbar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodoFragment extends Fragment {

    private TodoAdapter todoAdapter;
    private CalendarMonthAdapter calendarAdapter;
    private TextView tvMonthYear, btnExpand;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate viewingMonth = LocalDate.now().withDayOfMonth(1);
    private boolean calendarExpanded = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_todo, container, false);

        // --- Calendar (collapsed=week, expanded=month) ---
        tvMonthYear = v.findViewById(R.id.tv_month_year);
        btnExpand = v.findViewById(R.id.btn_expand);
        RecyclerView monthGrid = v.findViewById(R.id.recycler_month_days);
        monthGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarMonthAdapter();
        monthGrid.setAdapter(calendarAdapter);
        calendarAdapter.setOnDayClickListener(date -> {
            selectedDate = date;
            viewingMonth = date.withDayOfMonth(1);
            refreshCalendarGrid();
            refreshCalendarCounts();
            loadTodos();
            syncDateWithActivity();
        });

        // Expand/collapse toggle
        View headerRow = v.findViewById(R.id.header_calendar);
        View.OnClickListener toggleExpand = view -> {
            calendarExpanded = !calendarExpanded;
            btnExpand.setText(calendarExpanded ? "▲" : "▼");
            refreshCalendarGrid();
            refreshCalendarCounts();
        };
        headerRow.setOnClickListener(toggleExpand);
        btnExpand.setOnClickListener(toggleExpand);

        // Prev/next: week when collapsed, month when expanded
        v.findViewById(R.id.btn_prev_month).setOnClickListener(view -> {
            if (calendarExpanded) viewingMonth = viewingMonth.minusMonths(1);
            else { viewingMonth = viewingMonth.minusWeeks(1); selectedDate = selectedDate.minusWeeks(1); }
            refreshCalendarGrid();
            refreshCalendarCounts();
            if (!calendarExpanded) { loadTodos(); syncDateWithActivity(); }
        });
        v.findViewById(R.id.btn_next_month).setOnClickListener(view -> {
            if (calendarExpanded) viewingMonth = viewingMonth.plusMonths(1);
            else { viewingMonth = viewingMonth.plusWeeks(1); selectedDate = selectedDate.plusWeeks(1); }
            refreshCalendarGrid();
            refreshCalendarCounts();
            if (!calendarExpanded) { loadTodos(); syncDateWithActivity(); }
        });

        // --- Todo list ---
        RecyclerView todosRv = v.findViewById(R.id.recycler_todos);
        todosRv.setLayoutManager(new LinearLayoutManager(getContext()));
        todoAdapter = new TodoAdapter();
        todosRv.setAdapter(todoAdapter);

        todoAdapter.setOnItemClickListener(item -> {
            Intent i = new Intent(getContext(), EditTodoActivity.class);
            if (item.id != null) i.putExtra("todo_id", item.id);
            startActivity(i);
        });

        todoAdapter.setOnCheckListener((item, checked) -> {
            final Context ctx = getContext();
            if (ctx == null) return;
            exec.execute(() -> {
                item.is_completed = checked ? 1 : 0;
                AppDatabase.getInstance(ctx).todoDao().update(item);
                com.example.todolist.widget.BadgeHelper.refresh(ctx);
                // Refresh calendar counts after check change
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> refreshCalendarCounts());
                }
            });
        });

        // Left swipe → confirm delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    todoAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    return;
                }
                todoAdapter.notifyItemChanged(pos);
                TodoItem target = todoAdapter.getItem(pos);
                new AlertDialog.Builder(getContext())
                    .setTitle("删除待办")
                    .setMessage("确定要删除「" + target.title + "」吗？")
                    .setPositiveButton("删除", (dialog, which) -> performTodoDelete(pos, target, v))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(todosRv);

        // --- Add todo button ---
        v.findViewById(R.id.btn_add_todo).setOnClickListener(view -> {
            Intent i = new Intent(getContext(), EditTodoActivity.class);
            startActivity(i);
        });

        if (getActivity() instanceof MainActivity) {
            selectedDate = ((MainActivity) getActivity()).getSelectedDate();
            viewingMonth = selectedDate.withDayOfMonth(1);
        }
        refreshCalendarGrid();
        loadTodos();
        refreshCalendarCounts();

        return v;
    }

    private void refreshCalendarGrid() {
        if (tvMonthYear == null) return;
        tvMonthYear.setText(DateUtils.formatYearMonth(viewingMonth));
        List<LocalDate> grid;
        if (calendarExpanded) {
            grid = DateUtils.getMonthGrid(viewingMonth);
        } else {
            grid = DateUtils.getWeekDays(selectedDate);
        }
        calendarAdapter.updateGrid(grid, selectedDate);
    }

    private void refreshCalendarCounts() {
        List<LocalDate> grid;
        if (calendarExpanded) {
            grid = DateUtils.getMonthGrid(viewingMonth);
        } else {
            grid = DateUtils.getWeekDays(selectedDate);
        }
        loadCountsForDates(grid);
    }

    private void syncDateWithActivity() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSelectedDate(selectedDate);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            LocalDate activityDate = ((MainActivity) getActivity()).getSelectedDate();
            if (!activityDate.equals(selectedDate)) {
                selectedDate = activityDate;
                viewingMonth = selectedDate.withDayOfMonth(1);
                refreshCalendarGrid();
            }
        }
        loadTodos();
        refreshCalendarCounts();
    }

    private void loadTodos() {
        final Context ctx = getContext();
        if (ctx == null) return;
        final long dateStamp = DateUtils.toDateStamp(selectedDate);

        exec.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(ctx);
            final String userId = UserSession.getCurrentUser(ctx);
            List<TodoItem> all = db.todoDao().getByUser(userId);
            List<TodoItem> filtered = new ArrayList<>();
            for (TodoItem t : all) {
                if (t.due_date == 0L || isSameDay(t.due_date, dateStamp)) {
                    filtered.add(t);
                }
            }

            Collections.sort(filtered, (a, b) -> {
                if (a.is_completed != b.is_completed) return a.is_completed - b.is_completed;
                if (a.due_date == 0L && b.due_date != 0L) return 1;
                if (a.due_date != 0L && b.due_date == 0L) return -1;
                if (a.due_date != b.due_date) return Long.compare(a.due_date, b.due_date);
                return b.priority - a.priority;
            });

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> todoAdapter.setData(filtered));
            }
        });
    }

    /**
     * Count todos due on each date in the given list (week or month).
     */
    private void loadCountsForDates(List<LocalDate> dates) {
        exec.execute(() -> {
            if (getContext() == null) return;
            final String userId = UserSession.getCurrentUser(getContext());
            List<TodoItem> all = AppDatabase.getInstance(getContext())
                .todoDao().getByUser(userId);
            Map<LocalDate, Integer> counts = new HashMap<>();

            for (LocalDate date : dates) {
                if (date == null) continue;
                long stamp = DateUtils.toDateStamp(date);
                int count = 0;
                for (TodoItem t : all) {
                    if (t.due_date > 0 && t.is_completed == 0 && isSameDay(t.due_date, stamp)) count++;
                }
                if (count > 0) counts.put(date, count);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                    calendarAdapter.updateCounts(counts));
            }
        });
    }

    private boolean isSameDay(long dueTimestamp, long dateStamp) {
        if (dueTimestamp <= 0 || dateStamp <= 0) return false;
        try {
            LocalDate dueDate = java.time.Instant.ofEpochMilli(dueTimestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            return DateUtils.toDateStamp(dueDate) == dateStamp;
        } catch (Exception e) {
            return false;
        }
    }

    private void performTodoDelete(int pos, TodoItem item, View rootView) {
        todoAdapter.removeAt(pos);
        final Context ctx = getContext();
        if (ctx == null) return;
        exec.execute(() -> {
            AppDatabase.getInstance(ctx).todoDao().delete(item);
            com.example.todolist.widget.BadgeHelper.refresh(ctx);
            // Refresh calendar badge counts after deletion
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> refreshCalendarCounts());
            }
        });
        Snackbar.make(rootView, getString(R.string.deleted), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.undo), view -> {
                exec.execute(() -> {
                    long newId = AppDatabase.getInstance(ctx).todoDao().insert(item);
                    item.id = newId;
                    com.example.todolist.widget.BadgeHelper.refresh(ctx);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            todoAdapter.insertAt(pos, item);
                            refreshCalendarCounts();
                        });
                    }
                });
            }).show();
    }
}
