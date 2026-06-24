package com.example.todolist.adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.data.entity.HabitItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.VH> {

    private List<HabitItem> data = new ArrayList<>();
    private OnCheckListener checkListener;
    private OnItemClickListener itemClickListener;
    private Map<Long, Boolean> checkedMap = new HashMap<>();
    private Set<Long> recommendedIds = new HashSet<>();

    public interface OnCheckListener { void onCheck(HabitItem item, boolean checked); }
    public void setOnCheckListener(OnCheckListener l) { this.checkListener = l; }

    public interface OnItemClickListener { void onItemClick(HabitItem item); }
    public void setOnItemClickListener(OnItemClickListener l) { this.itemClickListener = l; }

    public void setData(List<HabitItem> items) {
        this.data = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    public void setCheckedMap(Map<Long, Boolean> map) {
        this.checkedMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }
    public void setRecommendedIds(Set<Long> ids) {
        this.recommendedIds = ids != null ? ids : new HashSet<>();
        notifyDataSetChanged();
    }

    public HabitItem getItem(int pos) { return data.get(pos); }

    public HabitItem removeAt(int pos) {
        HabitItem item = data.remove(pos);
        notifyItemRemoved(pos);
        return item;
    }

    public void insertAt(int pos, HabitItem item) {
        data.add(pos, item);
        notifyItemInserted(pos);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_habit, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        HabitItem item = data.get(position);
        holder.name.setText(item.name == null ? "" : item.name);

        // Description
        String desc = item.description != null ? item.description.trim() : "";
        if (!TextUtils.isEmpty(desc)) {
            holder.description.setText(desc);
            holder.description.setVisibility(View.VISIBLE);
        } else {
            holder.description.setVisibility(View.GONE);
        }

        // Schedule summary (replaces old frequency badge)
        holder.schedule.setText(item.getScheduleSummary());

        // Recommendation badge
        boolean isRecommended = item.id != null && recommendedIds.contains(item.id);
        holder.recommendBadge.setVisibility(isRecommended ? View.VISIBLE : View.GONE);

        // Color dot
        try {
            int color = Color.parseColor(item.color != null ? item.color : "#FFD54F");
            holder.colorDot.getBackground().setTint(color);
        } catch (Exception e) {
            holder.colorDot.getBackground().setTint(0xFFFFD54F);
        }

        // Click item → edit
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(item);
        });

        // Checkbox
        holder.checkBox.setOnCheckedChangeListener(null);
        boolean checked = false;
        if (item.id != null && checkedMap.containsKey(item.id)) {
            checked = checkedMap.get(item.id);
        }
        holder.checkBox.setChecked(checked);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (item.id != null) checkedMap.put(item.id, isChecked);
            if (checkListener != null) checkListener.onCheck(item, isChecked);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, description, schedule, recommendBadge;
        View colorDot;
        CheckBox checkBox;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            description = itemView.findViewById(R.id.text_description);
            schedule = itemView.findViewById(R.id.text_schedule);
            recommendBadge = itemView.findViewById(R.id.text_recommend_badge);
            colorDot = itemView.findViewById(R.id.view_color_dot);
            checkBox = itemView.findViewById(R.id.checkbox_check);
        }
    }
}
