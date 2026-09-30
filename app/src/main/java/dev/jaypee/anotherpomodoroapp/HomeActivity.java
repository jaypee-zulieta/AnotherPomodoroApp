package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirestoreService firestoreService;
    private GeminiRecommendationService geminiService;
    private TaskAdapter taskAdapter;
    private ListenerRegistration tasksListener;
    private RecyclerView rvTasks;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        firestoreService = new FirestoreService();
        geminiService = new GeminiRecommendationService();

        rvTasks = findViewById(R.id.rv_tasks);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        FloatingActionButton fabAddTask = findViewById(R.id.fab_add_task);

        taskAdapter = new TaskAdapter();
        taskAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskEdit(PomodoroTask task) {
                showEditTaskDialog(task);
            }

            @Override
            public void onTaskDelete(PomodoroTask task) {
                confirmDeleteTask(task);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        fabAddTask.setOnClickListener(v -> showAddTaskDialog(user.getUid()));

        setupBottomNavigation(user);
        listenForTasks(user.getUid());
    }

    private void listenForTasks(String userId) {
        tasksListener = firestoreService.getTasks(userId, (value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                List<PomodoroTask> tasks = value.toObjects(PomodoroTask.class);
                taskAdapter.setTasks(tasks);
                tvEmptyState.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showAddTaskDialog(String userId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView rvChat = dialogView.findViewById(R.id.rv_chat);
        EditText etWork = dialogView.findViewById(R.id.et_work_time);
        EditText etBreak = dialogView.findViewById(R.id.et_break_time);
        EditText etChatInput = dialogView.findViewById(R.id.et_chat_input);
        Button btnSend = dialogView.findViewById(R.id.btn_send);
        ProgressBar pbAi = dialogView.findViewById(R.id.pb_ai_loading);
        TextView tvAiError = dialogView.findViewById(R.id.tv_ai_error);
        Button btnAdd = dialogView.findViewById(R.id.btn_add_task);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View durationContainer = dialogView.findViewById(R.id.ll_duration_container);
        Button btnDismissAi = dialogView.findViewById(R.id.btn_dismiss_ai);
        Button btnActivateAi = dialogView.findViewById(R.id.btn_activate_ai);
        View chatContainer = dialogView.findViewById(R.id.ll_chat_container);
        com.google.android.material.textfield.TextInputLayout tilChatInput = dialogView.findViewById(R.id.til_chat_input);
        com.google.android.material.textfield.TextInputLayout tilManualTitle = dialogView.findViewById(R.id.til_manual_title);
        EditText etManualTitle = dialogView.findViewById(R.id.et_manual_title);

        ChatAdapter chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
        chatAdapter.addMessage(new ChatMessage("What are you planning to do today?", false));

        btnDismissAi.setOnClickListener(v -> {
            tilChatInput.setError(null);
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
                tilChatInput.setError(null);
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
                    Toast.makeText(HomeActivity.this, "AI failed", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnAdd.setOnClickListener(v -> {
            String title = "Pomodoro Task";
            if (tilManualTitle.getVisibility() == View.VISIBLE) {
                String manualTitle = etManualTitle.getText().toString().trim();
                if (manualTitle.isEmpty()) {
                    tilManualTitle.setError("Task description title is required");
                    return;
                }
                title = manualTitle;
            } else {
                boolean hasUserSpoken = false;
                for (ChatMessage msg : chatAdapter.getMessages()) {
                    if (msg.isUser()) {
                        title = msg.getText();
                        hasUserSpoken = true;
                        break;
                    }
                }
                if (!hasUserSpoken || durationContainer.getVisibility() != View.VISIBLE) {
                    tilChatInput.setError("Please describe your task to the AI coach first or choose Dismiss AI");
                    return;
                }
            }

            String workStr = etWork.getText().toString().trim();
            String breakStr = etBreak.getText().toString().trim();

            int workTime = workStr.isEmpty() ? 25 : Integer.parseInt(workStr);
            int breakTime = breakStr.isEmpty() ? 5 : Integer.parseInt(breakStr);

            PomodoroTask task = new PomodoroTask(
                    null,
                    userId,
                    title,
                    workTime,
                    breakTime,
                    System.currentTimeMillis()
            );

            firestoreService.saveTask(task, aVoid -> {
                dialog.dismiss();
                Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
            }, e -> {
                Toast.makeText(this, "Failed to save task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            tasksListener.remove();
        }
    }

    private void setupBottomNavigation(FirebaseUser user) {
        ImageView profileImageView = findViewById(R.id.iv_nav_profile);
        TextView initialTextView = findViewById(R.id.tv_nav_initial);

        Uri photoUrl = user.getPhotoUrl();
        String name = user.getDisplayName();

        if (photoUrl != null) {
            initialTextView.setVisibility(View.GONE);
            profileImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(profileImageView);
        } else {
            profileImageView.setVisibility(View.GONE);
            initialTextView.setVisibility(View.VISIBLE);
            if (name != null && !name.isEmpty()) {
                initialTextView.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                initialTextView.setText("?");
            }
        }

        profileImageView.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });

        initialTextView.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });
    }

    private void showEditTaskDialog(PomodoroTask task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        com.google.android.material.textfield.TextInputLayout tilTitle = dialogView.findViewById(R.id.til_edit_title);
        EditText etTitle = dialogView.findViewById(R.id.et_edit_title);
        EditText etWork = dialogView.findViewById(R.id.et_edit_work_time);
        EditText etBreak = dialogView.findViewById(R.id.et_edit_break_time);
        Button btnSave = dialogView.findViewById(R.id.btn_save_task);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_edit);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_task);

        etTitle.setText(task.getTitle());
        etWork.setText(String.valueOf(task.getWorkTime()));
        etBreak.setText(String.valueOf(task.getBreakTime()));

        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            if (newTitle.isEmpty()) {
                tilTitle.setError("Task title is required");
                return;
            }

            String workStr = etWork.getText().toString().trim();
            String breakStr = etBreak.getText().toString().trim();

            int workTime = workStr.isEmpty() ? 25 : Integer.parseInt(workStr);
            int breakTime = breakStr.isEmpty() ? 5 : Integer.parseInt(breakStr);

            task.setTitle(newTitle);
            task.setWorkTime(workTime);
            task.setBreakTime(breakTime);

            firestoreService.saveTask(task, aVoid -> {
                dialog.dismiss();
                Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
            }, e -> {
                Toast.makeText(this, "Failed to update task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteTask(task);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void confirmDeleteTask(PomodoroTask task) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete_task, (dialog, which) -> {
                    firestoreService.deleteTask(task.getId(), aVoid -> {
                        Toast.makeText(HomeActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                    }, e -> {
                        Toast.makeText(HomeActivity.this, "Failed to delete task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
