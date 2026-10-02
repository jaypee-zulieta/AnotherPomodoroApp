package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    private MaterialCardView cvEmptyState;

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
        cvEmptyState = findViewById(R.id.cv_empty_state);
        FloatingActionButton fabAddTask = findViewById(R.id.fab_add_task);

        taskAdapter = new TaskAdapter();
        taskAdapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskEdit(PomodoroTask task) {
                Intent intent = new Intent(HomeActivity.this, AddTaskActivity.class);
                intent.putExtra(AddTaskActivity.EXTRA_USER_ID, user.getUid());
                intent.putExtra(AddTaskActivity.EXTRA_TASK, task);
                startActivity(intent);
            }

            @Override
            public void onTaskDelete(PomodoroTask task) {
                confirmDeleteTask(task);
            }

            @Override
            public void onTaskStartTimer(PomodoroTask task) {
                Intent intent = new Intent(HomeActivity.this, TimerActivity.class);
                intent.putExtra(TimerActivity.EXTRA_TASK_TITLE, task.getTitle());
                intent.putExtra(TimerActivity.EXTRA_WORK_TIME, task.getWorkTime());
                intent.putExtra(TimerActivity.EXTRA_BREAK_TIME, task.getBreakTime());
                intent.putExtra(TimerActivity.EXTRA_TASK_ID, task.getId());
                startActivity(intent);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddTaskActivity.class);
            intent.putExtra(AddTaskActivity.EXTRA_USER_ID, user.getUid());
            startActivity(intent);
        });

        MaterialButton btnEmptyAddTask = findViewById(R.id.btn_empty_add_task);
        if (btnEmptyAddTask != null) {
            btnEmptyAddTask.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AddTaskActivity.class);
                intent.putExtra(AddTaskActivity.EXTRA_USER_ID, user.getUid());
                startActivity(intent);
            });
        }

        setupToolbar(user);
        listenForTasks(user.getUid());
    }

    private void setupToolbar(FirebaseUser user) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageView profileImageView = findViewById(R.id.iv_profile);
        TextView initialTextView = findViewById(R.id.tv_profile_initial);

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

    private void listenForTasks(String userId) {
        tasksListener = firestoreService.getTasks(userId, (value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                List<PomodoroTask> tasks = value.toObjects(PomodoroTask.class);
                taskAdapter.setTasks(tasks);
                cvEmptyState.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            tasksListener.remove();
        }
    }

}