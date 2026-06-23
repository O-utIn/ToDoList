package com.example.todolist.adapter;

import android.graphics.Paint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.util.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.VH> {

    private List<TodoItem> data = new ArrayList<>();
    private OnItemClickListener listener;
    private OnCheckListener checkListener;

    public interface OnItemClickListener { void onClick(TodoItem item); }
    public interface OnCheckListener { void onCheck(TodoItem item, boolean checked); }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }
    public void setOnCheckListener(OnCheckListener l) { this.checkListener = l; }

    public void setData(List<TodoItem> items) {
        this.data = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public TodoItem getItem(int pos) { return data.get(pos); }
    public TodoItem removeAt(int pos) {
        TodoItem item = data.remove(pos);
        notifyItemRemoved(pos);
        return item;
    }
    public void insertAt(int pos, TodoItem item) {
        data.add(pos, item);
        notifyItemInserted(pos);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_todo, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TodoItem item = data.get(position);
        holder.title.setText(item.title == null ? "" : item.title);

        // Priority: color + label
        int priorityColor;
        String priorityLabel;
        switch (item.priority) {
            case 3: priorityColor = 0xFFF44336; priorityLabel = "高"; break; // red
            case 2: priorityColor = 0xFFFF9800; priorityLabel = "中"; break; // orange
            default: priorityColor = 0xFF4CAF50; priorityLabel = "低"; break; // green
        }

        // Left border strip
        try {
            if (holder.priorityStrip.getBackground() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DrawableCompat.setTint(holder.priorityStrip.getBackground(), priorityColor);
                }
            }
        } catch (Exception ignored) {}

        // Priority label chip
        holder.priorityLabel.setText(priorityLabel);
        try {
            if (holder.priorityLabel.getBackground() != null) {
                DrawableCompat.setTint(holder.priorityLabel.getBackground(), priorityColor);
            }
        } catch (Exception ignored) {}

        // Due date
        String dueText = DateUtils.formatDueDate(item.due_date);
        holder.due.setText(dueText.isEmpty() ? "" : dueText);

        // Due time
        if (item.due_date > 0) {
            holder.dueTime.setText(new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(item.due_date)));
            holder.dueTime.setVisibility(View.VISIBLE);
        } else {
            holder.dueTime.setVisibility(View.GONE);
        }

        // Note preview
        if (item.note != null && !item.note.isEmpty()) {
            holder.notePreview.setText(item.note);
            holder.notePreview.setVisibility(View.VISIBLE);
        } else {
            holder.notePreview.setVisibility(View.GONE);
        }

        // Completed state: strikethrough + gray
        updateStrikethrough(holder, item.is_completed == 1);

        // Checkbox
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(item.is_completed == 1);
        holder.checkbox.setOnCheckedChangeListener((btn, checked) -> {
            item.is_completed = checked ? 1 : 0;
            updateStrikethrough(holder, checked);
            if (checkListener != null) checkListener.onCheck(item, checked);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    private void updateStrikethrough(VH holder, boolean completed) {
        if (completed) {
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.title.setTextColor(0xFF9E9E9E);
        } else {
            holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.title.setTextColor(0xFF212121);
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, priorityLabel, due, dueTime, notePreview;
        View priorityStrip;
        CheckBox checkbox;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            priorityLabel = itemView.findViewById(R.id.text_priority_label);
            priorityStrip = itemView.findViewById(R.id.view_priority_strip);
            due = itemView.findViewById(R.id.text_due);
            dueTime = itemView.findViewById(R.id.text_due_time);
            notePreview = itemView.findViewById(R.id.text_note_preview);
            checkbox = itemView.findViewById(R.id.checkbox_done);
        }
    }
}
