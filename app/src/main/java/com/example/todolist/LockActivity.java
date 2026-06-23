package com.example.todolist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.util.HashUtils;

public class LockActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);
        EditText et = findViewById(R.id.edit_password);
        Button ok = findViewById(R.id.btn_unlock);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = et.getText().toString();
                SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                String hash = prefs.getString("password_hash", null);
                if (hash == null) {
                    // no password set
                    setResult(Activity.RESULT_OK);
                    finish();
                    return;
                }
                if (HashUtils.sha256(input).equals(hash)) {
                    prefs.edit().putBoolean("app_unlocked", true).apply();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(LockActivity.this, getString(R.string.password_incorrect), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
