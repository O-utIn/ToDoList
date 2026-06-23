package com.example.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.util.CryptoUtils;
import com.example.todolist.util.HashUtils;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple local login/register screen.
 * Credentials are stored in SharedPreferences (username → SHA256 hashed password).
 * Features: account autocomplete from login history, remember-password toggle with auto-fill.
 */
public class LoginActivity extends AppCompatActivity {

    private AutoCompleteTextView editUsername;
    private EditText editPassword;
    private Button btnLogin, btnRegister;
    private SwitchMaterial switchRememberPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        switchRememberPassword = findViewById(R.id.switch_remember_password);

        // Load remember-password toggle state (defaults to true)
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean remember = prefs.getBoolean("remember_password", true);
        switchRememberPassword.setChecked(remember);

        // Load login history for autocomplete
        setupAutocomplete();

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void setupAutocomplete() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet("login_history", new LinkedHashSet<>());
        List<String> history = new ArrayList<>(historySet);
        // Reverse to show most recent first
        Collections.reverse(history);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            history
        );
        editUsername.setAdapter(adapter);

        // Dynamically size dropdown: max 4 visible items, then scroll
        int itemCount = Math.min(history.size(), 4);
        if (itemCount == 0) itemCount = 1; // avoid zero-height when list is empty
        editUsername.setDropDownHeight(itemCount * dpToPx(48));

        // When user selects an account from dropdown, auto-fill password if remembered
        editUsername.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = (String) parent.getItemAtPosition(position);
            autoFillPassword(selectedUser);
        });
    }

    /**
     * Auto-fill password field if a remembered password exists for the given username.
     * Also syncs the remember-password switch state.
     */
    private void autoFillPassword(String username) {
        String rememberedPwd = getRememberedPassword(username);
        if (rememberedPwd != null) {
            editPassword.setText(rememberedPwd);
            switchRememberPassword.setChecked(true);
        } else {
            editPassword.setText("");
            // Don't force switch off — user may have turned it off intentionally
        }
    }

    private void attemptLogin() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String storedHash = prefs.getString("user_" + username + "_password", null);

        if (storedHash == null) {
            Toast.makeText(this, "账号不存在，请先注册", Toast.LENGTH_SHORT).show();
            return;
        }

        if (HashUtils.sha256(password).equals(storedHash)) {
            // Login success
            onLoginSuccess(username, password);
        } else {
            Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptRegister() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "密码至少需要4位", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String existing = prefs.getString("user_" + username + "_password", null);

        if (existing != null) {
            Toast.makeText(this, "账号已存在，请直接登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register
        String hash = HashUtils.sha256(password);
        prefs.edit()
            .putString("user_" + username + "_password", hash)
            .apply();

        onLoginSuccess(username, password);
        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
    }

    private void onLoginSuccess(String username, String password) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Save remember-password toggle state
        boolean remember = switchRememberPassword.isChecked();
        editor.putBoolean("remember_password", remember);

        // Always set logged_in_user so the app works during this session.
        // If "remember password" is off, it will be cleared on next cold start.
        editor.putString("logged_in_user", username);

        // Store or clear remembered password based on toggle
        if (remember) {
            rememberPassword(editor, username, password);
        } else {
            clearRememberedPassword(editor, username);
        }

        // Always add to login history for autocomplete
        addToLoginHistory(editor, username);

        editor.apply();

        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    // ---- Password remembering (AES/GCM encrypted via Android Keystore) ----

    private static final String REM_PWD_PREFIX = "rem_pwd_";

    private void rememberPassword(SharedPreferences.Editor editor, String username, String password) {
        String encrypted = CryptoUtils.encrypt(password);
        if (encrypted != null) {
            editor.putString(REM_PWD_PREFIX + username, encrypted);
        } else {
            // Encryption failed — clear any stale value so we don't leave old data
            editor.remove(REM_PWD_PREFIX + username);
            android.util.Log.e("LoginActivity", "Password encryption failed, cleared stored value");
        }
    }

    private void clearRememberedPassword(SharedPreferences.Editor editor, String username) {
        editor.remove(REM_PWD_PREFIX + username);
    }

    private String getRememberedPassword(String username) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String encrypted = prefs.getString(REM_PWD_PREFIX + username, null);
        if (encrypted == null) return null;
        return CryptoUtils.decrypt(encrypted);
    }

    /**
     * Add username to login history set (most recent last for ordering).
     */
    private void addToLoginHistory(SharedPreferences.Editor editor, String username) {
        Set<String> history = new LinkedHashSet<>(
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getStringSet("login_history", new LinkedHashSet<>())
        );
        history.remove(username); // remove then re-add so it becomes most recent
        history.add(username);
        editor.putStringSet("login_history", history);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
