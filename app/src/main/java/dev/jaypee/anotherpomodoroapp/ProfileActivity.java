package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirestoreService firestoreService;
    private ListenerRegistration tasksListener;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestoreService = new FirestoreService();

        FirebaseUser user = firebaseAuth.getCurrentUser();

        ImageView profileImageView = findViewById(R.id.iv_profile_picture);
        TextView initialTextView = findViewById(R.id.tv_profile_initial);
        ProgressBar progressBar = findViewById(R.id.pb_profile_loading);
        TextView nameTextView = findViewById(R.id.tv_profile_name);
        TextView emailTextView = findViewById(R.id.tv_profile_email);
        TextView tasksCountTextView = findViewById(R.id.tv_tasks_count);
        TextView sessionsCountTextView = findViewById(R.id.tv_sessions_count);
        TextView focusTimeTextView = findViewById(R.id.tv_focus_time);
        Button backHomeButton = findViewById(R.id.btn_back_home);
        Button logoutButton = findViewById(R.id.btn_logout);
        Button deleteAccountButton = findViewById(R.id.btn_delete_account);

        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            nameTextView.setText(name != null ? name : "No Name");
            emailTextView.setText(email != null ? email : "No Email");

            if (photoUrl != null) {
                initialTextView.setVisibility(View.GONE);
                profileImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(profileImageView);
            } else {
                profileImageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                initialTextView.setVisibility(View.VISIBLE);
                if (name != null && !name.isEmpty()) {
                    initialTextView.setText(String.valueOf(name.charAt(0)).toUpperCase());
                } else {
                    initialTextView.setText("?");
                }
            }

            // Load task stats
            tasksListener = firestoreService.getTasks(user.getUid(), (value, error) -> {
                if (error == null && value != null) {
                    List<PomodoroTask> tasks = value.toObjects(PomodoroTask.class);
                    int totalTasks = tasks.size();
                    int totalMinutes = 0;
                    for (PomodoroTask task : tasks) {
                        totalMinutes += task.getWorkTime();
                    }
                    if (tasksCountTextView != null) {
                        tasksCountTextView.setText(String.valueOf(totalTasks));
                    }
                    if (sessionsCountTextView != null) {
                        sessionsCountTextView.setText(String.valueOf(totalTasks));
                    }
                    if (focusTimeTextView != null) {
                        if (totalMinutes >= 60) {
                            int hours = totalMinutes / 60;
                            int mins = totalMinutes % 60;
                            focusTimeTextView.setText(mins > 0 ? String.format(java.util.Locale.getDefault(), "%dh %dm", hours, mins) : String.format(java.util.Locale.getDefault(), "%dh", hours));
                        } else {
                            focusTimeTextView.setText(String.format(java.util.Locale.getDefault(), "%dm", totalMinutes));
                        }
                    }
                }
            });

        } else {
            // If no user is signed in, go back to sign in
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        logoutButton.setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        if (deleteAccountButton != null) {
            deleteAccountButton.setOnClickListener(v -> confirmDeleteAccount(user));
        }
    }

    private void confirmDeleteAccount(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_account_title)
                .setMessage(R.string.confirm_delete_account_message)
                .setPositiveButton(R.string.delete_account, (dialog, which) -> deleteAccountAndTasks(user))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAccountAndTasks(FirebaseUser user) {
        if (user == null) return;

        showLoadingDialog();

        // 1. First delete all tasks associated with this user
        firestoreService.deleteAllUserTasks(user.getUid(), aVoid -> {
            // 2. Then delete the Firebase auth user
            user.delete().addOnCompleteListener(task -> {
                dismissLoadingDialog();
                if (task.isSuccessful()) {
                    firebaseAuth.signOut();
                    Toast.makeText(ProfileActivity.this, R.string.account_deleted_success, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Exception e = task.getException();
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(ProfileActivity.this, R.string.reauth_required_message, Toast.LENGTH_LONG).show();
                    } else {
                        String errorMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
                        Toast.makeText(ProfileActivity.this, getString(R.string.delete_account_failed, errorMsg), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }, e -> {
            dismissLoadingDialog();
            String errorMsg = (e != null && e.getMessage() != null) ? e.getMessage() : "Unknown error";
            Toast.makeText(ProfileActivity.this, getString(R.string.delete_account_failed, errorMsg), Toast.LENGTH_LONG).show();
        });
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setPadding(32, 32, 32, 32);
        builder.setView(progressBar);
        builder.setMessage(R.string.deleting_account);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            tasksListener.remove();
        }
        dismissLoadingDialog();
    }
}
