package dev.jaypee.anotherpomodoroapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_WORK_TIME = "extra_work_time";
    public static final String EXTRA_BREAK_TIME = "extra_break_time";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private TextView tvCountdown, tvTimerMode;
    private Button btnStartPause;

    private long timeLeftInMillis;
    private boolean isTimerRunning = false;
    private boolean isWorkMode = true;

    private int workTimeMinutes = 25;
    private int breakTimeMinutes = 5;
    private String taskTitle = "Pomodoro Session";

    private final BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PomodoroService.BROADCAST_UPDATE.equals(intent.getAction())) {
                timeLeftInMillis = intent.getLongExtra(PomodoroService.EXTRA_TIME_LEFT, timeLeftInMillis);
                isWorkMode = intent.getBooleanExtra(PomodoroService.EXTRA_IS_WORK_MODE, isWorkMode);
                isTimerRunning = intent.getBooleanExtra(PomodoroService.EXTRA_IS_RUNNING, isTimerRunning);
                String taskName = intent.getStringExtra(PomodoroService.EXTRA_TASK_NAME);
                if (taskName != null) {
                    taskTitle = taskName;
                }

                tvTimerMode.setText(isWorkMode ? R.string.focus_time : R.string.break_time);
                btnStartPause.setText(isTimerRunning ? R.string.pause : (timeLeftInMillis < workTimeMinutes * 60 * 1000L ? R.string.resume : R.string.start));
                updateCountDownText();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_timer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_timer), (v, insets) -> {
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

        if (getIntent().hasExtra(EXTRA_TASK_TITLE)) {
            taskTitle = getIntent().getStringExtra(EXTRA_TASK_TITLE);
            workTimeMinutes = getIntent().getIntExtra(EXTRA_WORK_TIME, 25);
            breakTimeMinutes = getIntent().getIntExtra(EXTRA_BREAK_TIME, 5);
            timeLeftInMillis = workTimeMinutes * 60 * 1000L;
            isWorkMode = true;
            isTimerRunning = false;
        } else {
            taskTitle = PomodoroService.staticTaskTitle;
            isWorkMode = PomodoroService.staticIsWorkMode;
            isTimerRunning = PomodoroService.staticIsRunning;
            timeLeftInMillis = PomodoroService.staticTimeLeftInMillis;
        }

        tvCountdown = findViewById(R.id.tv_countdown);
        tvTimerMode = findViewById(R.id.tv_timer_mode);
        TextView tvTimerTaskTitle = findViewById(R.id.tv_timer_task_title);
        btnStartPause = findViewById(R.id.btn_start_pause);
        Button btnReset = findViewById(R.id.btn_reset);
        ImageView ibBack = findViewById(R.id.ib_back);

        ibBack.setOnClickListener(v -> finish());

        tvTimerTaskTitle.setText(taskTitle);
        tvTimerMode.setText(isWorkMode ? R.string.focus_time : R.string.break_time);
        updateCountDownText();

        btnStartPause.setText(isTimerRunning ? R.string.pause : (timeLeftInMillis < workTimeMinutes * 60 * 1000L ? R.string.resume : R.string.start));

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startOrResumeTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(PomodoroService.BROADCAST_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timerReceiver, filter);
        }

        Intent queryIntent = new Intent(this, PomodoroService.class);
        queryIntent.setAction(PomodoroService.ACTION_QUERY_STATE);
        startService(queryIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(timerReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void startOrResumeTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
                return;
            }
        }

        Intent serviceIntent = new Intent(this, PomodoroService.class);
        if (isTimerRunning) {
            return;
        }

        // If paused mid-session, resume; otherwise start fresh
        if (timeLeftInMillis > 0 && timeLeftInMillis < workTimeMinutes * 60 * 1000L && !PomodoroService.staticIsRunning && timeLeftInMillis != workTimeMinutes * 60 * 1000L) {
            serviceIntent.setAction(PomodoroService.ACTION_RESUME);
        } else {
            serviceIntent.setAction(PomodoroService.ACTION_START);
            serviceIntent.putExtra(PomodoroService.EXTRA_TASK_TITLE, taskTitle);
            serviceIntent.putExtra(PomodoroService.EXTRA_WORK_TIME, workTimeMinutes);
            serviceIntent.putExtra(PomodoroService.EXTRA_BREAK_TIME, breakTimeMinutes);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        isTimerRunning = true;
        btnStartPause.setText(R.string.pause);
    }

    private void pauseTimer() {
        Intent serviceIntent = new Intent(this, PomodoroService.class);
        serviceIntent.setAction(PomodoroService.ACTION_PAUSE);
        startService(serviceIntent);

        isTimerRunning = false;
        btnStartPause.setText(R.string.resume);
    }

    private void resetTimer() {
        Intent serviceIntent = new Intent(this, PomodoroService.class);
        serviceIntent.setAction(PomodoroService.ACTION_RESET);
        startService(serviceIntent);

        isTimerRunning = false;
        isWorkMode = true;
        tvTimerMode.setText(R.string.focus_time);
        timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        updateCountDownText();
        btnStartPause.setText(R.string.start);
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountdown.setText(timeLeftFormatted);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startOrResumeTimer();
            } else {
                Toast.makeText(this, "Notification permission is required to show the timer in the panel.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
