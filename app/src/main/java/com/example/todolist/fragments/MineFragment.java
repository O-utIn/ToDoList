package com.example.todolist.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.todolist.LoginActivity;
import com.example.todolist.R;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.dao.PomodoroSessionDao;
import com.example.todolist.util.BackupManager;
import com.example.todolist.util.LocationHelper;
import com.example.todolist.util.LocationHelper.LocationInfo;
import com.example.todolist.util.UserSession;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MineFragment extends Fragment {

    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;

    // Location
    private View locationCard;
    private TextView tvMineAddress, tvMineCoords, tvMineAccuracy;
    private LocationHelper locationHelper;

    // Preset avatar options (emoji)
    private static final String[] AVATARS = {
        "🧑", "👩", "👨", "🧔", "👩‍🦰", "👨‍🦱", "🧑‍🦳", "👩‍🦲",
        "🧑‍💻", "👩‍💻", "👨‍🎓", "🧑‍🎨", "👩‍🔧", "👨‍🍳", "🧑‍🏫", "👩‍⚕️",
        "👨‍🚀", "🧑‍🎤", "🦊", "🐱", "🐶", "🐼", "🐨", "🦁",
        "🐰", "🌟", "⭐", "🔥", "💪", "🎯", "📋", "✅",
        "💡", "🎵", "🌱", "☕", "📚", "🎮", "🏃", "🧘"
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_mine, container, false);
        prefs = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // --- User info area ---
        refreshLoginState(v);
        setupAvatarClick(v);
        v.findViewById(R.id.tv_login_status).setOnClickListener(view -> {
            // Click username text → login / logout
            handleLoginLogout(v);
        });

        // --- Location card ---
        locationCard = v.findViewById(R.id.location_card);
        tvMineAddress = v.findViewById(R.id.tv_mine_address);
        tvMineCoords = v.findViewById(R.id.tv_mine_coords);
        tvMineAccuracy = v.findViewById(R.id.tv_mine_accuracy);
        View btnRefreshLoc = v.findViewById(R.id.btn_refresh_location);

        locationHelper = LocationHelper.getInstance(requireContext());

        // Observe location
        locationHelper.getLocationLiveData().observe(getViewLifecycleOwner(), info -> {
            if (info == null) return;
            updateLocationDisplay(info);
        });

        btnRefreshLoc.setOnClickListener(view -> {
            if (LocationHelper.hasPermission(requireContext())) {
                tvMineAddress.setText("正在获取位置...");
                locationHelper.requestSingleLocation(requireContext());
            } else {
                // Delegate to the LocationFragment-style permission request
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        });

        // Auto-fetch once if permitted
        if (LocationHelper.hasPermission(requireContext())) {
            locationHelper.getLastLocation(requireContext());
        }

        // --- Build settings list dynamically ---
        LinearLayout settingsContainer = v.findViewById(R.id.settings_container);
        addSettingRow(settingsContainer, "🤖", "DeepSeek AI 对话设置", false, view -> showDeepSeekKeyDialog());
        addSettingRow(settingsContainer, "💾", "备份管理", false, view -> showBackupManager());
        addSettingRow(settingsContainer, "🛡️", "使用权限", false, view -> {
            openAppPermissionSettings();
        });

        // --- Load stats ---
        loadStats(v);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        View root = getView();
        if (root != null) {
            refreshLoginState(root);
            loadStats(root);
        }
    }

    private void setupAvatarClick(View v) {
        TextView avatarView = v.findViewById(R.id.avatar_placeholder);
        avatarView.setOnClickListener(view -> {
            String loggedInUser = prefs.getString("logged_in_user", null);
            if (loggedInUser != null) {
                // Logged in — show avatar picker
                showAvatarPicker(avatarView, loggedInUser);
            } else {
                // Not logged in — go to login
                Intent i = new Intent(getContext(), LoginActivity.class);
                startActivity(i);
            }
        });
    }

    private void handleLoginLogout(View v) {
        String loggedInUser = prefs.getString("logged_in_user", null);
        if (loggedInUser != null) {
            // Already logged in — offer logout
            new AlertDialog.Builder(getContext())
                .setTitle(loggedInUser)
                .setMessage("是否退出登录？")
                .setPositiveButton("退出", (d, w) -> {
                    prefs.edit().remove("logged_in_user").apply();
                    View root = getView();
                    refreshLoginState(root);
                    loadStats(root);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        } else {
            // Not logged in — go to login
            Intent i = new Intent(getContext(), LoginActivity.class);
            startActivity(i);
        }
    }

    private void showAvatarPicker(TextView avatarView, String username) {
        // Inflate the picker layout
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_avatar_picker, null);
        GridView gridAvatars = dialogView.findViewById(R.id.grid_avatars);

        // Load current avatar
        String currentAvatar = prefs.getString("avatar_" + username, "👤");

        // Set up grid adapter
        AvatarAdapter adapter = new AvatarAdapter(currentAvatar);
        gridAvatars.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .create();

        gridAvatars.setOnItemClickListener((parent, view, position, id) -> {
            String selected = AVATARS[position];
            // Save avatar for this user
            prefs.edit().putString("avatar_" + username, selected).apply();
            // Update display
            avatarView.setText(selected);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void refreshLoginState(View v) {
        if (v == null) return;
        TextView tvLogin = v.findViewById(R.id.tv_login_status);
        TextView avatarView = v.findViewById(R.id.avatar_placeholder);
        String loggedInUser = prefs.getString("logged_in_user", null);
        if (loggedInUser != null && !loggedInUser.isEmpty()) {
            tvLogin.setText(loggedInUser);
            // Load user's avatar
            String avatar = prefs.getString("avatar_" + loggedInUser, "👤");
            avatarView.setText(avatar);
        } else {
            tvLogin.setText("未登录");
            avatarView.setText("👤");
        }
    }

    private void loadStats(View v) {
        exec.execute(() -> {
            try {
                final String userId = UserSession.getCurrentUser(getContext());
                int todoCount = AppDatabase.getInstance(getContext()).todoDao().getByUser(userId).size();
                int habitCount = AppDatabase.getInstance(getContext()).habitDao().getByUser(userId).size();
                PomodoroSessionDao psDao = AppDatabase.getInstance(getContext()).pomodoroSessionDao();
                int pomodoroCount = psDao.getTotalCompletedCountForUser(userId);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            updateStatView(v, R.id.stat_habits, habitCount, "习惯");
                            updateStatView(v, R.id.stat_todos, todoCount, "待办");
                            updateStatView(v, R.id.stat_pomodoros, pomodoroCount, "专注完成次数");
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    private void updateStatView(View parent, int statId, int count, String label) {
        View statView = parent.findViewById(statId);
        if (statView != null) {
            TextView tvCount = statView.findViewById(R.id.tv_stat_count);
            TextView tvLabel = statView.findViewById(R.id.tv_stat_label);
            if (tvCount != null) tvCount.setText(String.valueOf(count));
            if (tvLabel != null) tvLabel.setText(label);
        }
    }

    private void addSettingRow(LinearLayout container, String icon, String name, boolean hasSwitch,
                               View.OnClickListener clickListener) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_settings_row, container, false);
        TextView tvIcon = row.findViewById(R.id.tv_setting_icon);
        TextView tvName = row.findViewById(R.id.tv_setting_name);
        TextView tvArrow = row.findViewById(R.id.tv_setting_arrow);

        tvIcon.setText(icon);
        tvName.setText(name);
        tvArrow.setVisibility(View.VISIBLE);
        row.findViewById(R.id.switch_setting).setVisibility(View.GONE);

        row.setOnClickListener(clickListener);
        container.addView(row);
    }

    // ---- Location display ----

    private void updateLocationDisplay(LocationInfo info) {
        if (locationCard == null) return;
        if (info.isError || info.isEmpty) {
            tvMineAddress.setText(info.addressLine != null ? info.addressLine : "无法获取位置");
            tvMineCoords.setText("");
            tvMineAccuracy.setText("");
            return;
        }
        tvMineAddress.setText(info.addressLine);
        tvMineCoords.setText(String.format(Locale.US, "%.6f, %.6f", info.latitude, info.longitude));
        tvMineAccuracy.setText(String.format(Locale.US, "精度: %.0f 米", info.accuracy));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            tvMineAddress.setText("正在获取位置...");
            locationHelper.requestSingleLocation(requireContext());
        }
    }

    // ---- DeepSeek API Key dialog ----

    private void showDeepSeekKeyDialog() {
        android.view.View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_api_key, null);
        com.google.android.material.textfield.TextInputEditText input =
                dialogView.findViewById(R.id.edit_api_key);

        // Read the current user's API key (user-specific)
        Context ctx = getContext();
        String prefKey = com.example.todolist.ai.ChatClient.getKeyPrefName(ctx);
        String existing = prefs.getString(prefKey, "");
        if (!existing.isEmpty()) input.setText(existing);

        new AlertDialog.Builder(ctx)
                .setTitle("设置 DeepSeek API Key")
                .setView(dialogView)
                .setPositiveButton("保存", (d, w) -> {
                    String key = input.getText().toString().trim();
                    if (!android.text.TextUtils.isEmpty(key)) {
                        prefs.edit().putString(prefKey, key).apply();
                        com.example.todolist.ai.ChatClient.resetInstance();
                        Toast.makeText(ctx, "API Key 已保存", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ---- Backup management ----

    private int selectedBackupIndex = -1;

    private void showBackupManager() {
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_backup_manager, null);
        LinearLayout containerList = dialogView.findViewById(R.id.container_backup_list);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_empty_hint);
        Button btnExport = dialogView.findViewById(R.id.btn_export_new);
        Button btnRestore = dialogView.findViewById(R.id.btn_restore);
        Button btnClose = dialogView.findViewById(R.id.btn_close);

        selectedBackupIndex = -1;

        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setView(dialogView)
            .create();

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Render backup list via helper method
        refreshBackupList(containerList, tvEmpty);

        // Export new backup
        btnExport.setOnClickListener(v -> {
            exec.execute(() -> {
                try {
                    String path = BackupManager.exportAll(getContext());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(),
                                "备份已保存到 " + path + "\n可在「下载」目录中查看",
                                Toast.LENGTH_LONG).show();
                            refreshBackupList(containerList, tvEmpty);
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "备份失败: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
                    }
                }
            });
        });

        // Restore selected
        btnRestore.setOnClickListener(v -> {
            File[] backups = BackupManager.listPrivateBackups(getContext());
            if (selectedBackupIndex < 0 || selectedBackupIndex >= backups.length) {
                Toast.makeText(getContext(), "请先选择一个备份文件", Toast.LENGTH_SHORT).show();
                return;
            }
            File selected = backups[selectedBackupIndex];
            exec.execute(() -> {
                try {
                    BackupManager.importFromFile(getContext(), selected);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "恢复完成", Toast.LENGTH_SHORT).show();
                            View root = getView();
                            if (root != null) loadStats(root);
                            dialog.dismiss();
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "恢复失败: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
                    }
                }
            });
        });

        dialog.show();
    }

    private void refreshBackupList(LinearLayout containerList, TextView tvEmpty) {
        containerList.removeAllViews();
        File[] backups = BackupManager.listPrivateBackups(getContext());

        if (backups.length == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (int i = 0; i < backups.length; i++) {
                File f = backups[i];
                View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_backup_row, containerList, false);
                TextView tvName = row.findViewById(R.id.tv_backup_name);
                View btnDelete = row.findViewById(R.id.btn_delete_backup);

                TextView tvCheck = row.findViewById(R.id.tv_checkmark);
                tvName.setText(f.getName());
                final int idx = i;

                // Apply selected state
                if (idx == selectedBackupIndex) {
                    tvCheck.setVisibility(View.VISIBLE);
                    row.setBackgroundResource(R.drawable.task_card_bg);
                } else {
                    tvCheck.setVisibility(View.INVISIBLE);
                    row.setBackgroundResource(R.drawable.settings_row_bg);
                }

                // Click row → select for restore
                row.setOnClickListener(v2 -> {
                    selectedBackupIndex = idx;
                    // Refresh to update all rows' selected state
                    refreshBackupList(containerList, tvEmpty);
                });

                // Delete button
                btnDelete.setOnClickListener(v2 -> {
                    new AlertDialog.Builder(getContext())
                        .setTitle("删除备份")
                        .setMessage("确定要删除 " + f.getName() + " 吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            f.delete();
                            selectedBackupIndex = -1;
                            refreshBackupList(containerList, tvEmpty);
                            Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                });

                containerList.addView(row);
            }
        }
    }

    private void openAppPermissionSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
        startActivity(intent);
    }

    // --- Avatar grid adapter ---

    private class AvatarAdapter extends BaseAdapter {
        private final String currentAvatar;

        AvatarAdapter(String currentAvatar) {
            this.currentAvatar = currentAvatar;
        }

        @Override
        public int getCount() {
            return AVATARS.length;
        }

        @Override
        public Object getItem(int position) {
            return AVATARS[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View cell = convertView;
            if (cell == null) {
                cell = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_avatar_cell, parent, false);
            }
            TextView tvCell = cell.findViewById(R.id.tv_avatar_cell);
            String avatar = AVATARS[position];
            tvCell.setText(avatar);
            // Highlight current selection
            cell.setAlpha(avatar.equals(currentAvatar) ? 1.0f : 0.5f);
            return cell;
        }
    }
}
