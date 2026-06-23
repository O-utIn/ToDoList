package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.data.entity.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 0;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onLongClick(ChatMessage message, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setMessages(List<ChatMessage> list) {
        messages.clear();
        if (list != null) messages.addAll(list);
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeLast() {
        if (!messages.isEmpty()) {
            int idx = messages.size() - 1;
            messages.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    public void removeAt(int position) {
        if (position >= 0 && position < messages.size()) {
            messages.remove(position);
            notifyItemRemoved(position);
        }
    }

    public ChatMessage getItem(int position) {
        if (position >= 0 && position < messages.size()) {
            return messages.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type == ChatMessage.TYPE_SENT
                ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v;
        if (viewType == VIEW_TYPE_SENT) {
            v = inflater.inflate(R.layout.item_chat_msg_sent, parent, false);
        } else {
            v = inflater.inflate(R.layout.item_chat_msg_received, parent, false);
        }
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvContent.setText(msg.content);
        holder.tvTime.setText(timeFormat.format(new Date(msg.timestamp)));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(msg, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_msg_content);
            tvTime = itemView.findViewById(R.id.tv_msg_time);
        }
    }
}
