package dev.jaypee.anotherpomodoroapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import io.noties.markwon.Markwon;

public class AddTaskActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_TASK = "extra_task";

    private String userId;
    private PomodoroTask taskToEdit;
    private FirestoreService firestoreService;
    private GeminiRecommendationService geminiService;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_add_task), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
            v.setPadding(
                    paddingPx + systemBars.left,
                    paddingPx + systemBars.top,
                    paddingPx + systemBars.right,
                    paddingPx + systemBars.bottom
            );
            return insets;
        });

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        taskToEdit = (PomodoroTask) getIntent().getSerializableExtra(EXTRA_TASK);

        if (userId == null && (taskToEdit == null || taskToEdit.getUserId() == null)) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (userId == null && taskToEdit != null) {
            userId = taskToEdit.getUserId();
        }

        firestoreService = new FirestoreService();
        geminiService = new GeminiRecommendationService();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        MaterialButton btnDismissAi = findViewById(R.id.btn_dismiss_ai);
        MaterialButton btnActivateAi = findViewById(R.id.btn_activate_ai);

        com.google.android.material.textfield.TextInputLayout tilManualTitle = findViewById(R.id.til_manual_title);
        EditText etManualTitle = findViewById(R.id.et_manual_title);

        View chatContainer = findViewById(R.id.ll_chat_container);
        RecyclerView rvChat = findViewById(R.id.rv_chat);
        EditText etChatInput = findViewById(R.id.et_chat_input);
        MaterialButton btnSend = findViewById(R.id.btn_send);
        ProgressBar pbAi = findViewById(R.id.pb_ai_loading);
        TextView tvAiError = findViewById(R.id.tv_ai_error);

        View durationContainer = findViewById(R.id.ll_duration_container);
        EditText etWork = findViewById(R.id.et_work_time);
        EditText etBreak = findViewById(R.id.et_break_time);

        Button btnDelete = findViewById(R.id.btn_delete_task);
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnSaveTask = findViewById(R.id.btn_save_task);

        btnCancel.setOnClickListener(v -> finish());

        ChatAdapter chatAdapter = new ChatAdapter(Markwon.create(this));
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
        chatAdapter.addMessage(new ChatMessage("What are you planning to do today?", false));

        boolean isEditing = (taskToEdit != null);

        if (isEditing) {
            btnSaveTask.setText(R.string.save_changes);
            btnDelete.setVisibility(View.VISIBLE);

            tilManualTitle.setVisibility(View.VISIBLE);
            chatContainer.setVisibility(View.GONE);
            btnDismissAi.setVisibility(View.GONE);
            btnActivateAi.setVisibility(View.VISIBLE);
            durationContainer.setVisibility(View.VISIBLE);

            etManualTitle.setText(taskToEdit.getTitle());
            etWork.setText(String.valueOf(taskToEdit.getWorkTime()));
            etBreak.setText(String.valueOf(taskToEdit.getBreakTime()));
        } else {
            btnSaveTask.setText(R.string.add_task);
            btnDelete.setVisibility(View.GONE);

            tilManualTitle.setVisibility(View.GONE);
            chatContainer.setVisibility(View.VISIBLE);
            btnDismissAi.setVisibility(View.VISIBLE);
            btnActivateAi.setVisibility(View.GONE);
            durationContainer.setVisibility(View.GONE);
        }

        btnDismissAi.setOnClickListener(v -> {
            tvAiError.setVisibility(View.GONE);
            tilManualTitle.setError(null);
            chatContainer.setVisibility(View.GONE);
            tilManualTitle.setVisibility(View.VISIBLE);
            durationContainer.setVisibility(View.VISIBLE);
            btnDismissAi.setVisibility(View.GONE);
            btnActivateAi.setVisibility(View.VISIBLE);

            android.view.animation.AlphaAnimation fadeIn = new android.view.animation.AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(500);
            tilManualTitle.startAnimation(fadeIn);
            durationContainer.startAnimation(fadeIn);
        });

        btnActivateAi.setOnClickListener(v -> {
            tilManualTitle.setError(null);
            tilManualTitle.setVisibility(View.GONE);
            chatContainer.setVisibility(View.VISIBLE);
            btnActivateAi.setVisibility(View.GONE);
            btnDismissAi.setVisibility(View.VISIBLE);

            android.view.animation.AlphaAnimation fadeIn = new android.view.animation.AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(500);
            chatContainer.startAnimation(fadeIn);
        });

        etManualTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilManualTitle.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        etChatInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvAiError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
            String userInput = etChatInput.getText().toString().trim();
            if (userInput.isEmpty()) {
                return;
            }

            chatAdapter.addMessage(new ChatMessage(userInput, true));
            etChatInput.setText("");
            rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);

            pbAi.setVisibility(View.VISIBLE);
            tvAiError.setVisibility(View.GONE);
            btnSend.setEnabled(false);

            geminiService.getChatResponse(chatAdapter.getMessages(), new GeminiRecommendationService.RecommendationCallback() {
                @Override
                public void onSuccess(String aiResponse, int workTime, int breakTime) {
                    pbAi.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    chatAdapter.addMessage(new ChatMessage(aiResponse, false));
                    rvChat.scrollToPosition(chatAdapter.getItemCount() - 1);

                    if (workTime != -1) {
                        etWork.setText(String.valueOf(workTime));
                    }
                    if (breakTime != -1) {
                        etBreak.setText(String.valueOf(breakTime));
                    }

                    if (durationContainer.getVisibility() != View.VISIBLE) {
                        durationContainer.setVisibility(View.VISIBLE);
                        android.view.animation.AlphaAnimation fadeIn = new android.view.animation.AlphaAnimation(0.0f, 1.0f);
                        fadeIn.setDuration(800);
                        durationContainer.startAnimation(fadeIn);
                        btnDismissAi.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    pbAi.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    tvAiError.setText(t.getMessage());
                    tvAiError.setVisibility(View.VISIBLE);
                    Toast.makeText(AddTaskActivity.this, "AI failed", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnSaveTask.setOnClickListener(v -> {
            String title;
            if (tilManualTitle.getVisibility() == View.VISIBLE) {
                String manualTitle = etManualTitle.getText().toString().trim();
                if (manualTitle.isEmpty()) {
                    tilManualTitle.setError("Task description title is required");
                    return;
                }
                title = manualTitle;
            } else {
                String chatTitle = null;
                for (ChatMessage msg : chatAdapter.getMessages()) {
                    if (msg.isUser()) {
                        chatTitle = msg.getText();
                        break;
                    }
                }
                if (chatTitle == null || durationContainer.getVisibility() != View.VISIBLE) {
                    tvAiError.setText(R.string.error_describe_task);
                    tvAiError.setVisibility(View.VISIBLE);
                    return;
                }
                title = chatTitle;
            }

            String workStr = etWork.getText().toString().trim();
            String breakStr = etBreak.getText().toString().trim();

            int workTime = workStr.isEmpty() ? 25 : Integer.parseInt(workStr);
            int breakTime = breakStr.isEmpty() ? 5 : Integer.parseInt(breakStr);

            if (isEditing) {
                taskToEdit.setTitle(title);
                taskToEdit.setWorkTime(workTime);
                taskToEdit.setBreakTime(breakTime);

                firestoreService.saveTask(taskToEdit, aVoid -> {
                    Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
                    finish();
                }, e -> Toast.makeText(this, "Failed to update task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                PomodoroTask newTask = new PomodoroTask(
                        null,
                        userId,
                        title,
                        workTime,
                        breakTime,
                        System.currentTimeMillis()
                );

                firestoreService.saveTask(newTask, aVoid -> {
                    Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
                    finish();
                }, e -> Toast.makeText(this, "Failed to save task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (taskToEdit != null && taskToEdit.getId() != null) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_delete_title)
                        .setMessage(R.string.confirm_delete_message)
                        .setPositiveButton(R.string.delete_task, (dialog, which) -> {
                            firestoreService.deleteTask(taskToEdit.getId(), aVoid -> {
                                Toast.makeText(AddTaskActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            }, e -> Toast.makeText(AddTaskActivity.this, "Failed to delete task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }
}