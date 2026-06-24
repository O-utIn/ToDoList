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
import com.example.todolist.EditHabitActivity;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.adapter.CalendarMonthAdapter;
import com.example.todolist.adapter.HabitAdapter;
import com.example.todolist.ai.recommendation.RecommendationEngine;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.dao.HabitCheckDao;
import com.example.todolist.data.entity.HabitCheck;
import com.example.todolist.data.entity.HabitItem;
import com.example.todolist.util.DateUtils;
import com.example.todolist.util.UserSession;
import com.google.android.material.snackbar.Snackbar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HabitFragment extends Fragment {

    private HabitAdapter habitAdapter;
    private CalendarMonthAdapter calendarAdapter;
    private TextView tvMonthYear, btnExpand;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate viewingMonth = LocalDate.now().withDayOfMonth(1);
    private boolean calendarExpanded = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_habit, container, false);

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
            loadHabits();
            syncDateWithActivity();
        });

        // Expand/collapse toggle — click title row or chevron
        View headerRow = v.findViewById(R.id.header_calendar);
        View.OnClickListener toggleExpand = view -> {
            calendarExpanded = !calendarExpanded;
            btnExpand.setText(calendarExpanded ? "▲" : "▼");
            refreshCalendarGrid();
            refreshCalendarCounts();
        };
        headerRow.setOnClickListener(toggleExpand);
        btnExpand.setOnClickListener(toggleExpand);

        // Prev/next: move by week when collapsed, by month when expanded
        v.findViewById(R.id.btn_prev_month).setOnClickListener(view -> {
            if (calendarExpanded) viewingMonth = viewingMonth.minusMonths(1);
            else { viewingMonth = viewingMonth.minusWeeks(1); selectedDate = selectedDate.minusWeeks(1); }
            refreshCalendarGrid();
            refreshCalendarCounts();
            if (!calendarExpanded) { loadHabits(); syncDateWithActivity(); }
        });
        v.findViewById(R.id.btn_next_month).setOnClickListener(view -> {
            if (calendarExpanded) viewingMonth = viewingMonth.plusMonths(1);
            else { viewingMonth = viewingMonth.plusWeeks(1); selectedDate = selectedDate.plusWeeks(1); }
            refreshCalendarGrid();
            refreshCalendarCounts();
            if (!calendarExpanded) { loadHabits(); syncDateWithActivity(); }
        });

        // --- Habit list ---
        RecyclerView habitsRv = v.findViewById(R.id.recycler_habits);
        habitsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        habitAdapter = new HabitAdapter();
        habitsRv.setAdapter(habitAdapter);

        // Click habit item → edit
        habitAdapter.setOnItemClickListener(item -> {
            Intent i = new Intent(getContext(), EditHabitActivity.class);
            if (item.id != null) i.putExtra("habit_id", item.id);
            startActivity(i);
        });

        habitAdapter.setOnCheckListener((item, checked) -> {
            long stamp = DateUtils.toDateStamp(selectedDate);
            final String userId = UserSession.getCurrentUser(getContext());
            exec.execute(() -> {
                HabitCheckDao dao = AppDatabase.getInstance(getContext()).habitCheckDao();
                HabitCheck existing = dao.getByHabitAndDate(item.id, stamp, userId);
                if (checked) {
                    if (existing == null) {
                        HabitCheck c = new HabitCheck(item.id, stamp, 1);
                        c.user_id = userId;
                        dao.insert(c);
                    }
                } else {
                    if (existing != null) dao.delete(existing);
                }
                // Refresh badge counts and recommendations after check change
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        refreshCalendarCounts();
                        loadHabits();
                    });
                }
            });
        });

        // Left swipe → confirm delete
        ItemTouchHelper.SimpleCallback habitSwipeCallback = new ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    habitAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    return;
                }
                habitAdapter.notifyItemChanged(pos);
                HabitItem target = habitAdapter.getItem(pos);
                new AlertDialog.Builder(getContext())
                    .setTitle("删除习惯")
                    .setMessage("确定要删除「" + target.name + "」吗？\n该习惯的所有打卡记录也将被清除。")
                    .setPositiveButton("删除", (dialog, which) -> performHabitDelete(pos, target, v))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            }
        };
        new ItemTouchHelper(habitSwipeCallback).attachToRecyclerView(habitsRv);

        // --- Add habit button ---
        v.findViewById(R.id.btn_add_habit).setOnClickListener(view -> {
            Intent i = new Intent(getContext(), EditHabitActivity.class);
            startActivity(i);
        });

        if (getActivity() instanceof MainActivity) {
            selectedDate = ((MainActivity) getActivity()).getSelectedDate();
            viewingMonth = selectedDate.withDayOfMonth(1);
        }
        refreshCalendarGrid();
        loadHabits();
        refreshCalendarCounts();

        return v;
    }

    private void refreshCalendarGrid() {
        tvMonthYear.setText(DateUtils.formatYearMonth(viewingMonth));
        List<LocalDate> grid;
        if (calendarExpanded) {
            grid = DateUtils.getMonthGrid(viewingMonth);
        } else {
            grid = DateUtils.getWeekDays(selectedDate);
        }
        // Only update grid + selection; keep existing counts to avoid flicker
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
        loadHabits();
        refreshCalendarCounts();
    }

    private void loadHabits() {
        // Capture selected date on main thread to avoid any threading issues
        final LocalDate date = this.selectedDate;
        exec.execute(() -> {
            if (getContext() == null) return;
            final String userId = UserSession.getCurrentUser(getContext());
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<HabitItem> all = db.habitDao().getByUser(userId);

            // Filter: only show habits active on the selected date
            List<HabitItem> filtered = new ArrayList<>();
            for (HabitItem h : all) {
                if (h.isActiveOnDate(date)) filtered.add(h);
            }

            long stamp = DateUtils.toDateStamp(date);
            List<HabitCheck> checks = db.habitCheckDao().getByDate(stamp, userId);

            Map<Long, Boolean> checkedMap = new HashMap<>();
            if (checks != null) {
                for (HabitCheck c : checks) checkedMap.put(c.habit_id, c.checked == 1);
            }

            // --- Smart recommendation ---
            RecommendationEngine engine = new RecommendationEngine(getContext());
            List<HabitItem> recommended = engine.recommend(3);
            Set<Long> recommendedIds = new HashSet<>();
            for (HabitItem rh : recommended) {
                if (rh.id != null) recommendedIds.add(rh.id);
            }

            // Reorder: recommended habits (active on this date) to the top
            List<HabitItem> sorted = new ArrayList<>();
            for (HabitItem rh : recommended) {
                for (int i = 0; i < filtered.size(); i++) {
                    if (filtered.get(i).id != null && filtered.get(i).id.equals(rh.id)) {
                        sorted.add(filtered.remove(i));
                        break;
                    }
                }
            }
            sorted.addAll(filtered);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    habitAdapter.setData(sorted);
                    habitAdapter.setCheckedMap(checkedMap);
                    habitAdapter.setRecommendedIds(recommendedIds);
                });
            }
        });
    }

    /**
     * Count habits that are active on each date but NOT yet checked (pending).
     */
    private void loadCountsForDates(List<LocalDate> dates) {
        exec.execute(() -> {
            if (getContext() == null) return;
            final String userId = UserSession.getCurrentUser(getContext());
            List<HabitItem> allHabits = AppDatabase.getInstance(getContext())
                .habitDao().getByUser(userId);
            Map<LocalDate, Integer> counts = new HashMap<>();

            for (LocalDate date : dates) {
                if (date == null) continue;
                long stamp = DateUtils.toDateStamp(date);
                List<HabitCheck> checks = AppDatabase.getInstance(getContext())
                    .habitCheckDao().getByDate(stamp, userId);
                // Build set of checked habit IDs for this date
                java.util.Set<Long> checkedIds = new java.util.HashSet<>();
                if (checks != null) {
                    for (HabitCheck c : checks) {
                        if (c.checked == 1) checkedIds.add(c.habit_id);
                    }
                }
                // Count active habits that are NOT checked
                int pendingCount = 0;
                for (HabitItem h : allHabits) {
                    if (h.isActiveOnDate(date) && (h.id == null || !checkedIds.contains(h.id))) {
                        pendingCount++;
                    }
                }
                if (pendingCount > 0) counts.put(date, pendingCount);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                    calendarAdapter.updateCounts(counts));
            }
        });
    }

    private void performHabitDelete(int pos, HabitItem item, View rootView) {
        habitAdapter.removeAt(pos);
        final Context ctx = getContext();
        if (ctx == null) return;
        exec.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(ctx);
            List<HabitCheck> checks = db.habitCheckDao().getByDate(
                DateUtils.toDateStamp(selectedDate), UserSession.getCurrentUser(ctx));
            for (HabitCheck c : checks) {
                if (c.habit_id == item.id) db.habitCheckDao().delete(c);
            }
            db.habitDao().delete(item);
        });
        Snackbar.make(rootView, getString(R.string.deleted), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.undo), view -> {
                exec.execute(() -> {
                    long newId = AppDatabase.getInstance(ctx).habitDao().insert(item);
                    item.id = newId;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> habitAdapter.insertAt(pos, item));
                    }
                });
            }).show();
    }
}
