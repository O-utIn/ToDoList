package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.data.entity.PomodoroTask;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Pomodoro task list in the Focus tab.
 */
public class PomodoroAdapter extends RecyclerView.Adapter<PomodoroAdapter.VH> {

    private List<PomodoroTask> data = new ArrayList<>();
    private OnTaskClickListener clickListener;
    private OnMoreClickListener moreListener;

    public interface OnTaskClickListener {
        void onTaskClick(PomodoroTask task);
    }

    public interface OnMoreClickListener {
        void onMoreClick(PomodoroTask task, View anchor);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.moreListener = listener;
    }

    public void setData(List<PomodoroTask> items) {
        this.data = items;
        notifyDataSetChanged();
    }

    public PomodoroTask getItem(int pos) {
        return data.get(pos);
    }

    public PomodoroTask removeAt(int pos) {
        PomodoroTask item = data.remove(pos);
        notifyItemRemoved(pos);
        return item;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pomodoro_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PomodoroTask task = data.get(position);
        holder.tvName.setText(task.name != null ? task.name : "");
        holder.tvDuration.setText(task.duration_minutes + "分钟");
        holder.tvIcon.setText(getIconEmoji(task.icon));

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTaskClick(task);
        });

        holder.btnMore.setOnClickListener(v -> {
            if (moreListener != null) moreListener.onMoreClick(task, holder.btnMore);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private String getIconEmoji(String icon) {
        if (icon == null) return "📋";
        switch (icon) {
            case "work": return "💼";
            case "study": return "📚";
            case "exercise": return "🏃";
            case "read": return "📖";
            case "code": return "💻";
            case "task": default: return "📋";
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvDuration, btnMore;
        VH(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_task_icon);
            tvName = itemView.findViewById(R.id.tv_task_name);
            tvDuration = itemView.findViewById(R.id.tv_task_duration);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }
}
