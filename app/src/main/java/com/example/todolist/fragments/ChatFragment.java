package com.example.todolist.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.adapter.ChatAdapter;
import com.example.todolist.ai.ChatApi;
import com.example.todolist.ai.ChatClient;
import com.example.todolist.ai.ChatRequest;
import com.example.todolist.ai.ChatResponse;
import com.example.todolist.ai.CommandParser;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private String currentUserId = ""; // tracks who is logged in for this session

    /** Session ID is user-specific: different users get isolated chat histories. */
    private String getSessionId() {
        String uid = com.example.todolist.util.UserSession.getCurrentUser(appCtx != null ? appCtx : requireContext());
        return "ai_chat_" + (uid.isEmpty() ? "default" : uid);
    }
    /** Returns the system prompt with today's date read from the device system clock. */
    private String getSystemPrompt() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String todayStr = today.getYear() + "年" + today.getMonthValue() + "月" + today.getDayOfMonth() + "日";
        String weekday = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE);
        java.time.LocalDate tomorrow = today.plusDays(1);

        return
            // ── P0: CRITICAL — operation commands ──
            "🔴 核心规则：当用户要求创建/删除/完成/查询待办、习惯或番茄钟时，你必须在回复末尾附加 [CMD]命令[/CMD]！\n" +
            "没有命令的回复会导致操作无法执行。\n\n" +
            "命令格式：[CMD]{\"action\":\"动作\",\"参数\":\"值\"}[/CMD]\n" +
            "命令和自然语言写在同一消息里，命令放末尾。\n\n" +
            "### 命令列表：\n" +
            "  create_todo → title, note(可选), priority(1低/2中/3高), due(可选,格式yyyy-MM-dd HH:mm)\n" +
            "  delete_todo → title(标题关键词) 或 id(编号)\n" +
            "  complete_todo → title 或 id\n" +
            "  list_todos → 无参数\n" +
            "  create_habit → name, freq(每日/每周), days(每周时必填,数字1-7对应周一到周日,如\"124\"=周一/二/四), time(可选,HH:mm如09:00), desc(可选)\n" +
            "  delete_habit → name 或 id\n" +
            "  list_habits → 无参数\n" +
            "  create_pomo → name, minutes(如25)\n" +
            "  delete_pomo → name 或 id\n" +
            "  list_pomos → 无参数\n\n" +
            "### 示例：\n" +
            "用户:「创建待办买菜」→ 你:「好的，已创建。[CMD]{\"action\":\"create_todo\",\"title\":\"买菜\",\"priority\":1}[/CMD]」\n" +
            "用户:「删除买菜」→ 你:「好的。[CMD]{\"action\":\"delete_todo\",\"title\":\"买菜\"}[/CMD]」\n" +
            "用户:「每天7点晨跑」→ 你:「好的。[CMD]{\"action\":\"create_habit\",\"name\":\"晨跑\",\"freq\":\"每日\",\"time\":\"07:00\"}[/CMD]」\n" +
            "用户:「每周一三五早上8点阅读」→ 你:「好的。[CMD]{\"action\":\"create_habit\",\"name\":\"阅读\",\"freq\":\"每周\",\"days\":\"135\",\"time\":\"08:00\"}[/CMD]」\n\n" +
            "⏰ 今天=" + todayStr + " " + weekday +
            "  明天=" + (tomorrow.getMonthValue() + "月" + tomorrow.getDayOfMonth() + "日") +
            "  后天=" + (today.plusDays(2).getMonthValue() + "月" + today.plusDays(2).getDayOfMonth() + "日") + "\n" +
            "回复简短(≤100字)，命令放在末尾。\n\n" +
            // ── P1: Software overview ──
            "## 关于本软件\n" +
            "你是「待办清单」App内置助手。5个标签：习惯、待办、番茄钟、AI助手、我的。\n" +
            "· 习惯：日历+打卡，每日/每周，可设提醒\n" +
            "· 待办：优先级+截止日期，左滑删除，勾选完成\n" +
            "· 番茄钟：倒计时，前台服务，通知栏操作\n" +
            "· 我的：头像/统计/备份/密码锁/API设置\n" +
            "所有数据本地存储(Room)。";
    }

    private RecyclerView recyclerChat;
    private EditText etInput;
    private Button btnSend;
    private View btnClearChat;
    private ChatAdapter chatAdapter;
    private ExecutorService exec;
    private Handler handler;

    // Safe references
    private Context appCtx;
    private AppDatabase db;
    private SharedPreferences prefs;

    // Conversation context (list of messages for the API)
    private final List<ChatRequest.Message> conversation = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        Context ctx = requireContext();
        appCtx = ctx.getApplicationContext();
        db = AppDatabase.getInstance(ctx);
        prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        exec = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        recyclerChat = v.findViewById(R.id.recycler_chat);
        etInput = v.findViewById(R.id.et_chat_input);
        btnSend = v.findViewById(R.id.btn_send);
        btnClearChat = v.findViewById(R.id.btn_clear_chat);

        recyclerChat.setLayoutManager(new LinearLayoutManager(ctx));
        chatAdapter = new ChatAdapter();
        recyclerChat.setAdapter(chatAdapter);

        // Long-press to delete individual message
        chatAdapter.setOnItemLongClickListener((msg, pos) -> {
            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("删除消息")
                    .setMessage("确定要删除这条消息吗？")
                    .setPositiveButton("删除", (d, w) -> deleteMessage(pos))
                    .setNegativeButton("取消", null)
                    .show();
        });

        btnSend.setOnClickListener(view -> sendMessage());

        etInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Clear chat button
        btnClearChat.setOnClickListener(view -> showClearDialog());

        // Load history
        loadMessages();

        // Check API key
        if (!ChatClient.isConfigured(ctx)) {
            showApiKeyDialog();
        }

        return v;
    }

    // ---- Clear / Delete ----

    private void showClearDialog() {
        int msgCount = chatAdapter.getItemCount();
        if (msgCount == 0) {
            Toast.makeText(getContext(), "对话已为空", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("清空对话")
                .setMessage("确定要删除当前全部 " + msgCount + " 条消息吗？\n\n此操作不可撤销。")
                .setPositiveButton("确定清空", (d, w) -> clearConversation())
                .setNegativeButton("取消", null)
                .show();
    }

    private void clearConversation() {
        exec.execute(() -> {
            db.chatMessageDao().deleteBySession(getSessionId());
            synchronized (conversation) {
                conversation.clear();
            }
            handler.post(() -> {
                chatAdapter.setMessages(null);
                Toast.makeText(getContext(), "对话已清空", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void deleteMessage(int position) {
        ChatMessage msg = chatAdapter.getItem(position);
        if (msg == null) return;
        exec.execute(() -> {
            db.chatMessageDao().delete(msg);
            handler.post(() -> {
                chatAdapter.removeAt(position);
                // Rebuild conversation context
                rebuildContext();
                Toast.makeText(getContext(), "消息已删除", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void rebuildContext() {
        exec.execute(() -> {
            List<ChatMessage> msgs = db.chatMessageDao().getBySession(getSessionId());
            synchronized (conversation) {
                conversation.clear();
                for (ChatMessage m : msgs) {
                    String role = m.type == ChatMessage.TYPE_SENT ? "user" : "assistant";
                    conversation.add(new ChatRequest.Message(role, m.content));
                }
            }
        });
    }

    // ---- Send message + AI reply ----

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check API configuration
        if (!ChatClient.isConfigured(requireContext())) {
            showApiKeyDialog();
            return;
        }

        long now = System.currentTimeMillis();
        ChatMessage userMsg = new ChatMessage(text, ChatMessage.TYPE_SENT, now, getSessionId());

        // Save to DB
        exec.execute(() -> db.chatMessageDao().insert(userMsg));

        // Add to UI
        chatAdapter.addMessage(userMsg);
        etInput.setText("");
        scrollToBottom();

        // Add to conversation context
        conversation.add(new ChatRequest.Message("user", text));

        // Send to AI
        sendToAI();
    }

    private void sendToAI() {
        btnSend.setEnabled(false);
        etInput.setEnabled(false);

        ChatMessage thinkingMsg = new ChatMessage("思考中...", ChatMessage.TYPE_RECEIVED, System.currentTimeMillis(), getSessionId());
        chatAdapter.addMessage(thinkingMsg);
        scrollToBottom();

        exec.execute(() -> {
            String replyText = null;
            try {
                ChatApi api = ChatClient.getApi(appCtx);
                if (api == null) {
                    replyText = "未配置 DeepSeek API Key，请先设置。";
                } else {
                    List<ChatRequest.Message> msgs = new ArrayList<>();
                    msgs.add(new ChatRequest.Message("system", getSystemPrompt()));
                    msgs.addAll(conversation);

                    ChatRequest req = new ChatRequest(msgs);
                    String auth = ChatClient.getApiKey(appCtx);
                    Response<ChatResponse> resp = api.chat(auth, req).execute();

                    if (resp.isSuccessful() && resp.body() != null) {
                        replyText = resp.body().getContent();
                        if (replyText == null || replyText.isEmpty()) {
                            replyText = "（API 返回空内容）";
                        }
                    } else {
                        int code = resp.code();
                        String errBody = resp.errorBody() != null ? resp.errorBody().string() : "";
                        Log.w(TAG, "API error " + code + ": " + errBody);
                        if (code == 401) replyText = "API Key 无效，请检查设置。";
                        else if (code == 429) replyText = "请求太频繁，请稍后再试。";
                        else replyText = "请求失败 (" + code + ")，请检查网络和 API 配置。";
                    }
                }
            } catch (java.net.UnknownHostException e) {
                replyText = "无法连接到 API 服务器，请检查网络。";
            } catch (java.net.SocketTimeoutException e) {
                replyText = "API 响应超时，请稍后重试。";
            } catch (Exception e) {
                Log.e(TAG, "sendToAI error: " + e.getMessage(), e);
                replyText = "发送失败: " + e.getMessage();
            }

            // --- Process commands embedded in the AI response ---
            CommandParser parser = new CommandParser(appCtx);
            CommandParser.Result cmdResult = parser.process(replyText);
            String displayText = cmdResult.displayText;
            String execLog = cmdResult.execLog;
            boolean hadCommands = !execLog.isEmpty();

            Log.d(TAG, "Commands: " + (hadCommands ? execLog : "(none)"));

            // Build display text for user (cleaned, no CMD tags, exec result at top)
            String userDisplayText = displayText;
            if (hadCommands) {
                userDisplayText = execLog + "\n\n" + displayText;
            }

            // Store in conversation context: include the FULL AI response (with CMD tags)
            // so the AI sees command usage patterns in history, preventing degradation.
            // Also append execution result so AI knows commands were processed.
            String contextText = replyText;
            if (hadCommands) {
                contextText += "\n[已执行: " + execLog + "]";
            }
            conversation.add(new ChatRequest.Message("assistant", contextText));

            long now = System.currentTimeMillis();
            ChatMessage aiMsg = new ChatMessage(userDisplayText, ChatMessage.TYPE_RECEIVED, now, getSessionId());
            db.chatMessageDao().insert(aiMsg);

            handler.post(() -> {
                chatAdapter.removeLast(); // remove "thinking..."
                chatAdapter.addMessage(aiMsg);
                scrollToBottom();
                btnSend.setEnabled(true);
                etInput.setEnabled(true);
            });

            // If AI didn't issue a command but user clearly wanted an action, retry once
            if (!hadCommands && userLikelyWantedAction()) {
                Log.w(TAG, "AI missed command, retrying with reminder");
                // Remove the unhelpful assistant message from conversation
                conversation.remove(conversation.size() - 1);
                // Add a strong reminder and retry the API call directly
                String remind = "你刚才的回复没有包含 [CMD] 命令块！用户要求执行操作，"
                        + "你必须严格按格式发出命令。现在请仅回复命令（不要其他文字）：";
                conversation.add(new ChatRequest.Message("user", remind));
                // Show retry indicator in UI
                handler.post(() -> {
                    ChatMessage retryMsg = new ChatMessage("正在重试...", ChatMessage.TYPE_RECEIVED,
                            System.currentTimeMillis(), getSessionId());
                    chatAdapter.addMessage(retryMsg);
                });
                retryCommand(remind);
            }
        });
    }

    /** Check if the last USER message looks like a create/delete/modify request. */
    private boolean userLikelyWantedAction() {
        if (conversation.isEmpty()) return false;
        // Search backward for the last user message (not the last message,
        // which could be an assistant reply we just added).
        String text = null;
        for (int i = conversation.size() - 1; i >= 0; i--) {
            ChatRequest.Message m = conversation.get(i);
            if ("user".equals(m.role)) {
                text = m.content;
                break;
            }
        }
        if (text == null) return false;
        // Keywords indicating the user wants a CRUD operation
        return text.contains("创建") || text.contains("新建") || text.contains("添加") || text.contains("新增")
            || text.contains("删除") || text.contains("移除") || text.contains("去掉")
            || text.contains("完成") || text.contains("做完") || text.contains("标记")
            || text.contains("帮我") || text.contains("添加一个") || text.contains("加一个")
            || text.contains("番茄") || text.contains("专注") || text.contains("计时")
            || text.contains("习惯") || text.contains("打卡") || text.contains("待办");
    }

    /**
     * Retry the AI API call after it failed to include a command in its response.
     * Called only from sendToAI's retry path (single retry, no recursion).
     */
    private void retryCommand(String remind) {
        exec.execute(() -> {
            String replyText = null;
            try {
                ChatApi api = ChatClient.getApi(appCtx);
                if (api != null) {
                    List<ChatRequest.Message> msgs = new ArrayList<>();
                    msgs.add(new ChatRequest.Message("system", getSystemPrompt()));
                    msgs.addAll(conversation);
                    ChatRequest req = new ChatRequest(msgs);
                    String auth = ChatClient.getApiKey(appCtx);
                    Response<ChatResponse> resp = api.chat(auth, req).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        replyText = resp.body().getContent();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "retryCommand error: " + e.getMessage());
            }

            if (replyText == null || replyText.isEmpty()) {
                replyText = "操作失败，请重试。";
            }

            // Process commands in retry response
            CommandParser.Result cmdResult = new CommandParser(appCtx).process(replyText);
            String displayText = cmdResult.displayText;
            String execLog = cmdResult.execLog;
            boolean hadCommands = !execLog.isEmpty();

            String userDisplayText = displayText;
            if (hadCommands) {
                userDisplayText = execLog + "\n\n" + displayText;
            }

            // Store in conversation with CMD tags intact
            String contextText = replyText;
            if (hadCommands) {
                contextText += "\n[已执行: " + execLog + "]";
            }
            conversation.add(new ChatRequest.Message("assistant", contextText));

            ChatMessage aiMsg = new ChatMessage(userDisplayText, ChatMessage.TYPE_RECEIVED,
                    System.currentTimeMillis(), getSessionId());
            db.chatMessageDao().insert(aiMsg);

            handler.post(() -> {
                chatAdapter.removeLast(); // remove retry indicator
                chatAdapter.addMessage(aiMsg);
                scrollToBottom();
                btnSend.setEnabled(true);
                etInput.setEnabled(true);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Detect user switch (login/logout) and reload accordingly
        String uid = com.example.todolist.util.UserSession.getCurrentUser(appCtx);
        if (!uid.equals(currentUserId)) {
            currentUserId = uid;
            // Clear UI and reload for the new user
            handler.post(() -> chatAdapter.setMessages(null));
        }
        loadMessages();
    }

    // ---- History ----

    private void loadMessages() {
        exec.execute(() -> {
            // Use ASC order (chronological) — not DESC
            List<ChatMessage> msgs = db.chatMessageDao().getBySession(getSessionId());
            // Rebuild conversation context from history
            synchronized (conversation) {
                conversation.clear();
                for (ChatMessage m : msgs) {
                    String role = m.type == ChatMessage.TYPE_SENT ? "user" : "assistant";
                    conversation.add(new ChatRequest.Message(role, m.content));
                }
            }
            handler.post(() -> {
                chatAdapter.setMessages(msgs);
                if (!msgs.isEmpty()) scrollToBottom();
            });
        });
    }

    // ---- API Key setup ----

    private void showApiKeyDialog() {
        android.view.View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_api_key, null);
        com.google.android.material.textfield.TextInputEditText input =
                dialogView.findViewById(R.id.edit_api_key);

        String existing = prefs.getString("deepseek_api_key", "");
        if (!existing.isEmpty()) input.setText(existing);

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("设置 DeepSeek API Key")
                .setView(dialogView)
                .setPositiveButton("保存", (d, w) -> {
                    String key = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(key)) {
                        prefs.edit().putString("deepseek_api_key", key).apply();
                        ChatClient.resetInstance();
                        Toast.makeText(getContext(), "API Key 已保存", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            recyclerChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (exec != null) exec.shutdownNow();
        handler.removeCallbacksAndMessages(null);
    }
}
