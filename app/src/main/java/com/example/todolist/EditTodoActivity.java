package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.entity.TodoItem;
import com.example.todolist.util.UserSession;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTodoActivity extends AppCompatActivity {

    private EditText editTitle, editNote, editDueDate, editDueTime;
    private TextView chipLow, chipMedium, chipHigh;
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private Long todoId = null;
    private int selectedPriority = 1; // 1=low, 2=medium, 3=high
    private Calendar dueCalendar; // null = no due date set

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_todo);

        // Default title for new todo
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText("📝 新建待办");

        editTitle = findViewById(R.id.edit_title);
        editNote = findViewById(R.id.edit_note);
        editDueDate = findViewById(R.id.edit_due_date);
        editDueTime = findViewById(R.id.edit_due_time);
        chipLow = findViewById(R.id.chip_priority_low);
        chipMedium = findViewById(R.id.chip_priority_medium);
        chipHigh = findViewById(R.id.chip_priority_high);
        Button btnSave = findViewById(R.id.btn_save);

        // Priority chips
        chipLow.setOnClickListener(v -> selectPriority(1));
        chipMedium.setOnClickListener(v -> selectPriority(2));
        chipHigh.setOnClickListener(v -> selectPriority(3));
        selectPriority(selectedPriority); // default: low

        // Due date picker
        editDueDate.setOnClickListener(v -> {
            Calendar c = dueCalendar != null ? (Calendar) dueCalendar.clone() : Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                if (dueCalendar == null) dueCalendar = Calendar.getInstance();
                dueCalendar.set(Calendar.YEAR, year);
                dueCalendar.set(Calendar.MONTH, month);
                dueCalendar.set(Calendar.DAY_OF_MONTH, day);
                editDueDate.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Due time picker
        editDueTime.setOnClickListener(v -> {
            Calendar c = dueCalendar != null ? (Calendar) dueCalendar.clone() : Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                if (dueCalendar == null) dueCalendar = Calendar.getInstance();
                dueCalendar.set(Calendar.HOUR_OF_DAY, hour);
                dueCalendar.set(Calendar.MINUTE, minute);
                editDueTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        // Save
        btnSave.setOnClickListener(v -> saveTodo());
    }

    private void selectPriority(int priority) {
        selectedPriority = priority;
        chipLow.setBackgroundResource(priority == 1 ? R.drawable.chip_selected_bg : R.drawable.chip_default_bg);
        chipLow.setTextColor(priority == 1 ? getColor(R.color.text_primary) : getColor(R.color.text_secondary));
        chipMedium.setBackgroundResource(priority == 2 ? R.drawable.chip_selected_bg : R.drawable.chip_default_bg);
        chipMedium.setTextColor(priority == 2 ? getColor(R.color.text_primary) : getColor(R.color.text_secondary));
        chipHigh.setBackgroundResource(priority == 3 ? R.drawable.chip_selected_bg : R.drawable.chip_default_bg);
        chipHigh.setTextColor(priority == 3 ? getColor(R.color.text_primary) : getColor(R.color.text_secondary));
    }

    private void saveTodo() {
        String title = editTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            editTitle.setError("请输入标题");
            return;
        }

        String note = editNote.getText().toString().trim();
        long dueDate = 0L;
        if (dueCalendar != null) {
            dueDate = dueCalendar.getTimeInMillis();
        }

        final int priority = selectedPriority;
        final long finalDue = dueDate;

        exec.execute(() -> {
            if (todoId == null) {
                TodoItem item = new TodoItem(title, note, finalDue, 0, priority);
                item.user_id = UserSession.getCurrentUser(getApplicationContext());
                AppDatabase.getInstance(getApplicationContext()).todoDao().insert(item);
            } else {
                TodoItem item = new TodoItem(title, note, finalDue, 0, priority);
                item.id = todoId;
                item.user_id = UserSession.getCurrentUser(getApplicationContext());
                AppDatabase.getInstance(getApplicationContext()).todoDao().update(item);
            }
            runOnUiThread(() -> {
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent() != null && getIntent().hasExtra("todo_id")) {
            todoId = getIntent().getLongExtra("todo_id", -1);
            if (todoId != -1) {
                // Switch title to edit mode
                TextView toolbarTitle = findViewById(R.id.toolbar_title);
                toolbarTitle.setText("📝 编辑待办");
                exec.execute(() -> {
                    TodoItem item = AppDatabase.getInstance(getApplicationContext()).todoDao().getById(todoId);
                    if (item != null) {
                        runOnUiThread(() -> {
                            editTitle.setText(item.title != null ? item.title : "");
                            editNote.setText(item.note != null ? item.note : "");
                            selectedPriority = item.priority;
                            selectPriority(selectedPriority);

                            if (item.due_date > 0) {
                                dueCalendar = Calendar.getInstance();
                                dueCalendar.setTimeInMillis(item.due_date);
                                editDueDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(new Date(item.due_date)));
                                editDueTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(new Date(item.due_date)));
                            }
                        });
                    }
                });
            }
        }
    }
}
