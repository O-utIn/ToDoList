package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the month calendar GridView (7 columns).
 * Each cell can show a count badge in the top-right corner.
 */
public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.VH> {

    private List<LocalDate> days;       // 42 cells, null = empty padding
    private LocalDate selectedDate;
    private Map<LocalDate, Integer> counts; // date → badge count
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<LocalDate> days, LocalDate selectedDate,
                        Map<LocalDate, Integer> counts) {
        this.days = days;
        this.selectedDate = selectedDate;
        this.counts = counts;
        notifyDataSetChanged();
    }

    /** Update grid and selection only, preserve existing counts (no flicker). */
    public void updateGrid(List<LocalDate> days, LocalDate selectedDate) {
        this.days = days;
        this.selectedDate = selectedDate;
        notifyDataSetChanged();
    }

    /** Update badge counts only, preserve grid and selection. */
    public void updateCounts(Map<LocalDate, Integer> counts) {
        this.counts = counts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_calendar_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        LocalDate date = days.get(position);
        TextView tvNum = holder.tvDayNumber;
        TextView tvBadge = holder.tvDayBadge;

        if (date == null) {
            // Empty cell (padding)
            tvNum.setText("");
            tvNum.setBackgroundResource(0);
            tvBadge.setVisibility(View.GONE);
            holder.itemView.setClickable(false);
            holder.itemView.setAlpha(0.3f);
            return;
        }

        holder.itemView.setClickable(true);
        holder.itemView.setAlpha(1.0f);

        LocalDate today = LocalDate.now();
        boolean isToday = date.equals(today);
        boolean isSelected = date.equals(selectedDate);

        tvNum.setText(String.valueOf(date.getDayOfMonth()));

        // Highlight: selected date gets full highlight, today gets a subtle dot
        if (isSelected) {
            tvNum.setBackgroundResource(R.drawable.calendar_day_selected_bg);
            tvNum.setTextColor(0xFF212121);
            holder.viewTodayDot.setVisibility(View.GONE);
        } else {
            tvNum.setBackgroundResource(0);
            tvNum.setTextColor(0xFF212121);
            // Show small dot if this is today but not the selected date
            holder.viewTodayDot.setVisibility(isToday ? View.VISIBLE : View.GONE);
        }

        // Badge count
        Integer count = counts != null ? counts.get(date) : null;
        if (count != null && count > 0) {
            tvBadge.setText(String.valueOf(Math.min(count, 99))); // max "99"
            tvBadge.setVisibility(View.VISIBLE);
        } else {
            tvBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(date);
        });
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDayNumber, tvDayBadge;
        View viewTodayDot;
        VH(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            tvDayBadge = itemView.findViewById(R.id.tv_day_badge);
            viewTodayDot = itemView.findViewById(R.id.view_today_dot);
        }
    }
}
