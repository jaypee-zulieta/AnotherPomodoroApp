package dev.jaypee.anotherpomodoroapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_WORK_TIME = "extra_work_time";
    public static final String EXTRA_BREAK_TIME = "extra_break_time";
    public static final String EXTRA_LONG_BREAK_TIME = "extra_long_break_time";
    public static final String EXTRA_LONG_BREAK_INTERVAL = "extra_long_break_interval";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_OPEN_BREAK = "extra_open_break";
    public static final String EXTRA_COMPLETED_POMODOROS = "extra_completed_pomodoros";
    public static final String ACTION_TIMER_UPDATED = "dev.jaypee.anotherpomodoroapp.action.TIMER_UPDATED";

    public static final int MODE_FOCUS = 0;
    public static final int MODE_SHORT_BREAK = 1;
    public static final int MODE_LONG_BREAK = 2;

    private final BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_TIMER_UPDATED.equals(intent.getAction())) {
                handleIntent(getIntent());
            }
        }
    };

    // Static state to persist across activity pauses/destroys when navigating home
    private static long targetEndTimeMillis = 0;
    private static boolean staticTimerRunning = false;
    private static int staticTimerMode = MODE_FOCUS;
    private static int staticWorkTimeMinutes = 25;
    private static int staticBreakTimeMinutes = 5;
    private static int staticLongBreakTimeMinutes = 15;
    private static int staticLongBreakInterval = 4;
    private static int staticCompletedPomodoros = 0;
    private static String staticTaskTitle = "Pomodoro Session";
    private static String staticTaskId = "";

    private TextView tvCountdown;
    private Chip chipTimerMode;
    private Chip chipSessionCounter;
    private CircularProgressView cpiTimer;
    private MaterialButton btnStartPause;
    private MaterialButton btnReset;
    private TextView tvWorkDuration;
    private TextView tvBreakDuration;
    private TextView tvLongBreakDuration;
    private TextView tvTimerTaskTitle;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private boolean isTimerRunning = false;
    private int timerMode = MODE_FOCUS;

    private int workTimeMinutes = 25;
    private int breakTimeMinutes = 5;
    private int longBreakTimeMinutes = 15;
    private int longBreakInterval = 4;
    private int completedPomodoros = 0;
    private String taskTitle = "Pomodoro Session";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_timer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

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

        tvCountdown = findViewById(R.id.tv_countdown);
        chipTimerMode = findViewById(R.id.chip_timer_mode);
        chipSessionCounter = findViewById(R.id.chip_session_counter);
        cpiTimer = findViewById(R.id.cpi_timer);
        tvTimerTaskTitle = findViewById(R.id.tv_timer_task_title);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnReset = findViewById(R.id.btn_reset);
        MaterialButton ibBack = findViewById(R.id.ib_back);
        tvWorkDuration = findViewById(R.id.tv_work_duration);
        tvBreakDuration = findViewById(R.id.tv_break_duration);
        tvLongBreakDuration = findViewById(R.id.tv_long_break_duration);

        ibBack.setOnClickListener(v -> finish());

        MaterialButton ibEditTask = findViewById(R.id.ib_edit_task);
        TextView tvEditLabel = findViewById(R.id.tv_edit_label);
        View.OnClickListener editClickListener = v -> {
            Intent editIntent = new Intent(TimerActivity.this, AddTaskActivity.class);
            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (userId != null) {
                editIntent.putExtra(AddTaskActivity.EXTRA_USER_ID, userId);
            }
            PomodoroTask taskToEdit = new PomodoroTask();
            taskToEdit.setId(staticTaskId);
            taskToEdit.setTitle(taskTitle);
            taskToEdit.setWorkTime(workTimeMinutes);
            taskToEdit.setBreakTime(breakTimeMinutes);
            taskToEdit.setLongBreakTime(longBreakTimeMinutes);
            taskToEdit.setLongBreakInterval(longBreakInterval);
            taskToEdit.setUserId(userId);
            editIntent.putExtra(AddTaskActivity.EXTRA_TASK, taskToEdit);
            startActivity(editIntent);
        };
        ibEditTask.setOnClickListener(editClickListener);
        tvEditLabel.setOnClickListener(editClickListener);

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
        btnReset.setOnClickListener(v -> resetTimer());

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_TASK_TITLE)) {
            taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
            workTimeMinutes = intent.getIntExtra(EXTRA_WORK_TIME, 25);
            breakTimeMinutes = intent.getIntExtra(EXTRA_BREAK_TIME, 5);
            longBreakTimeMinutes = intent.getIntExtra(EXTRA_LONG_BREAK_TIME, 15);
            longBreakInterval = intent.getIntExtra(EXTRA_LONG_BREAK_INTERVAL, 4);
            String incomingTaskId = intent.getStringExtra(EXTRA_TASK_ID);
            boolean openBreak = intent.getBooleanExtra(EXTRA_OPEN_BREAK, false);
            // Read completedPomodoros from intent (passed from alarm receiver) to avoid double-increment or reset
            int intentCompletedPomodoros = intent.getIntExtra(EXTRA_COMPLETED_POMODOROS, -1);
            boolean hasCompletedPomodorosInIntent = intentCompletedPomodoros >= 0;

            boolean isSameTask = staticTimerRunning && incomingTaskId != null && incomingTaskId.equals(staticTaskId);

            if (isSameTask) {
                // Restore the running timer state for the same task
                taskTitle = staticTaskTitle;
                workTimeMinutes = staticWorkTimeMinutes;
                breakTimeMinutes = staticBreakTimeMinutes;
                longBreakTimeMinutes = staticLongBreakTimeMinutes;
                longBreakInterval = staticLongBreakInterval;
                completedPomodoros = staticCompletedPomodoros;
                timerMode = staticTimerMode;
                long remaining = targetEndTimeMillis - System.currentTimeMillis();
                if (remaining > 0) {
                    timeLeftInMillis = remaining;
                    isTimerRunning = true;
                } else {
                    transitionToNextPhaseAuto();
                }
            } else if (openBreak) {
                // Focus session ended, transitioning to break.
                // Use completedPomodoros from intent (already incremented by handleTimerFinished before scheduling alarm)
                // DO NOT increment again here - that was the bug causing double-count.
                if (hasCompletedPomodorosInIntent) {
                    completedPomodoros = intentCompletedPomodoros;
                } else {
                    // Fallback: increment if not provided (should not happen with fixed alarm receiver)
                    completedPomodoros++;
                }
                boolean isLongBreak = (completedPomodoros > 0) && (completedPomodoros % longBreakInterval == 0);
                timerMode = isLongBreak ? MODE_LONG_BREAK : MODE_SHORT_BREAK;
                timeLeftInMillis = (isLongBreak ? longBreakTimeMinutes : breakTimeMinutes) * 60 * 1000L;
                isTimerRunning = false;
                staticTimerRunning = false;
                staticTimerMode = timerMode;
                staticTaskTitle = taskTitle;
                staticWorkTimeMinutes = workTimeMinutes;
                staticBreakTimeMinutes = breakTimeMinutes;
                staticLongBreakTimeMinutes = longBreakTimeMinutes;
                staticLongBreakInterval = longBreakInterval;
                staticCompletedPomodoros = completedPomodoros;
                staticTaskId = incomingTaskId != null ? incomingTaskId : "";
            } else {
                // Break ended, transitioning to focus (openBreak == false).
                // Use completedPomodoros from intent (should be the same count from completed focus sessions).
                // DO NOT reset to 0 - that was the bug losing the interval count.
                if (hasCompletedPomodorosInIntent) {
                    completedPomodoros = intentCompletedPomodoros;
                } else {
                    // Fallback: if no count provided, assume 0 (new task)
                    completedPomodoros = 0;
                }
                timerMode = MODE_FOCUS;
                timeLeftInMillis = workTimeMinutes * 60 * 1000L;
                isTimerRunning = false;
                staticTimerRunning = false;
                staticTimerMode = timerMode;
                staticTaskTitle = taskTitle;
                staticWorkTimeMinutes = workTimeMinutes;
                staticBreakTimeMinutes = breakTimeMinutes;
                staticLongBreakTimeMinutes = longBreakTimeMinutes;
                staticLongBreakInterval = longBreakInterval;
                staticCompletedPomodoros = completedPomodoros;
                staticTaskId = incomingTaskId != null ? incomingTaskId : "";
            }
        } else if (staticTimerRunning) {
            taskTitle = staticTaskTitle;
            workTimeMinutes = staticWorkTimeMinutes;
            breakTimeMinutes = staticBreakTimeMinutes;
            longBreakTimeMinutes = staticLongBreakTimeMinutes;
            longBreakInterval = staticLongBreakInterval;
            completedPomodoros = staticCompletedPomodoros;
            timerMode = staticTimerMode;
            long remaining = targetEndTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                timeLeftInMillis = remaining;
                isTimerRunning = true;
            } else {
                transitionToNextPhaseAuto();
            }
        } else {
            timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        }

        if (tvTimerTaskTitle != null) {
            tvTimerTaskTitle.setText(taskTitle);
            updateHeaderUI();
            if (tvWorkDuration != null) tvWorkDuration.setText(workTimeMinutes + " min");
            if (tvBreakDuration != null) tvBreakDuration.setText(breakTimeMinutes + " min");
            if (tvLongBreakDuration != null) tvLongBreakDuration.setText(longBreakTimeMinutes + " min");
            updateCountDownText();
            updateProgress();
            updateRingColor();
            updateThemeColors();

            if (isTimerRunning) {
                btnStartPause.setText(R.string.pause);
                startCountdown(timeLeftInMillis);
            } else {
                btnStartPause.setText(R.string.start);
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, timerUpdateReceiver, new IntentFilter(ACTION_TIMER_UPDATED), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (staticTimerRunning) {
            long remaining = targetEndTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                timeLeftInMillis = remaining;
                timerMode = staticTimerMode;
                taskTitle = staticTaskTitle;
                workTimeMinutes = staticWorkTimeMinutes;
                breakTimeMinutes = staticBreakTimeMinutes;
                longBreakTimeMinutes = staticLongBreakTimeMinutes;
                longBreakInterval = staticLongBreakInterval;
                completedPomodoros = staticCompletedPomodoros;
                if (tvTimerTaskTitle != null) {
                    tvTimerTaskTitle.setText(taskTitle);
                    updateHeaderUI();
                    if (tvWorkDuration != null) tvWorkDuration.setText(workTimeMinutes + " min");
                    if (tvBreakDuration != null) tvBreakDuration.setText(breakTimeMinutes + " min");
                    if (tvLongBreakDuration != null) tvLongBreakDuration.setText(longBreakTimeMinutes + " min");
                    updateCountDownText();
                    updateProgress();
                    updateRingColor();
                    updateThemeColors();
                }
                if (!isTimerRunning) {
                    isTimerRunning = true;
                    btnStartPause.setText(R.string.pause);
                }
                startCountdown(timeLeftInMillis);
            } else {
                transitionToNextPhaseAuto();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(timerUpdateReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void startTimer() {
        targetEndTimeMillis = System.currentTimeMillis() + timeLeftInMillis;
        staticTimerRunning = true;
        staticTimerMode = timerMode;
        staticTaskTitle = taskTitle;
        staticWorkTimeMinutes = workTimeMinutes;
        staticBreakTimeMinutes = breakTimeMinutes;
        staticLongBreakTimeMinutes = longBreakTimeMinutes;
        staticLongBreakInterval = longBreakInterval;
        staticCompletedPomodoros = completedPomodoros;

        startCountdown(timeLeftInMillis);
        isTimerRunning = true;
        btnStartPause.setText(R.string.pause);

        scheduleAlarm();
    }

    private void startCountdown(long millisInFuture) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                updateProgress();
            }

            @Override
            public void onFinish() {
                handleTimerFinished();
            }
        }.start();
    }

    private void handleTimerFinished() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        staticTimerRunning = false;
        btnStartPause.setText(R.string.start);
        playAlarmAndVibrate();
        cancelAlarm();

        if (timerMode == MODE_FOCUS) {
            completedPomodoros++;
            staticCompletedPomodoros = completedPomodoros;

            boolean isLongBreak = (completedPomodoros > 0) && (completedPomodoros % longBreakInterval == 0);
            int nextMode = isLongBreak ? MODE_LONG_BREAK : MODE_SHORT_BREAK;

            Intent alarmIntent = new Intent(this, PomodoroAlarmReceiver.class);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_TASK_TITLE, taskTitle);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_WORK_TIME, workTimeMinutes);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_BREAK_TIME, breakTimeMinutes);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_LONG_BREAK_TIME, longBreakTimeMinutes);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_LONG_BREAK_INTERVAL, longBreakInterval);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_COMPLETED_POMODOROS, completedPomodoros);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_TASK_ID, staticTaskId);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_TIMER_MODE, MODE_FOCUS);
            sendBroadcast(alarmIntent);

            if (isLongBreak) {
                Toast.makeText(TimerActivity.this, "Focus session finished! Time for a Long Break.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(TimerActivity.this, "Focus session finished! Time for a break.", Toast.LENGTH_LONG).show();
            }

            transitionToNextPhase(nextMode);
        } else {
            Intent alarmIntent = new Intent(this, PomodoroAlarmReceiver.class);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_TASK_TITLE, taskTitle);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_WORK_TIME, workTimeMinutes);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_BREAK_TIME, breakTimeMinutes);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_LONG_BREAK_TIME, longBreakTimeMinutes);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_LONG_BREAK_INTERVAL, longBreakInterval);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_COMPLETED_POMODOROS, completedPomodoros);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_TASK_ID, staticTaskId);
            alarmIntent.putExtra(PomodoroAlarmReceiver.EXTRA_TIMER_MODE, timerMode);
            sendBroadcast(alarmIntent);

            Toast.makeText(TimerActivity.this, "Break finished! Back to focus.", Toast.LENGTH_LONG).show();

            transitionToNextPhase(MODE_FOCUS);
        }
    }

    private void transitionToNextPhaseAuto() {
        if (timerMode == MODE_FOCUS) {
            completedPomodoros++;
            staticCompletedPomodoros = completedPomodoros;
            boolean isLongBreak = (completedPomodoros > 0) && (completedPomodoros % longBreakInterval == 0);
            transitionToNextPhase(isLongBreak ? MODE_LONG_BREAK : MODE_SHORT_BREAK);
        } else {
            transitionToNextPhase(MODE_FOCUS);
        }
    }

    private void transitionToNextPhase(int nextMode) {
        timerMode = nextMode;
        staticTimerMode = timerMode;

        if (timerMode == MODE_FOCUS) {
            timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        } else if (timerMode == MODE_SHORT_BREAK) {
            timeLeftInMillis = breakTimeMinutes * 60 * 1000L;
        } else {
            timeLeftInMillis = longBreakTimeMinutes * 60 * 1000L;
        }

        isTimerRunning = false;
        staticTimerRunning = false;
        btnStartPause.setText(R.string.start);
        updateHeaderUI();
        updateCountDownText();
        updateProgress();
        updateRingColor();
        updateThemeColors();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        staticTimerRunning = false;
        btnStartPause.setText(R.string.resume);
        cancelAlarm();
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        staticTimerRunning = false;
        timerMode = MODE_FOCUS;
        staticTimerMode = MODE_FOCUS;
        completedPomodoros = 0;
        staticCompletedPomodoros = 0;
        timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        updateHeaderUI();
        updateCountDownText();
        updateProgress();
        updateRingColor();
        updateThemeColors();
        btnStartPause.setText(R.string.start);
        cancelAlarm();
    }

    public static void startTimerFromBackground(Context context, String taskTitle, int workTime, int breakTime, int longBreakTime, int longBreakInterval, int completedPomodoros, String taskId, int nextTimerMode) {
        int durationMinutes;
        if (nextTimerMode == MODE_FOCUS) {
            durationMinutes = workTime;
        } else if (nextTimerMode == MODE_SHORT_BREAK) {
            durationMinutes = breakTime;
        } else {
            durationMinutes = longBreakTime;
        }
        long durationMillis = durationMinutes * 60 * 1000L;

        targetEndTimeMillis = System.currentTimeMillis() + durationMillis;
        staticTimerRunning = true;
        staticTimerMode = nextTimerMode;
        staticWorkTimeMinutes = workTime;
        staticBreakTimeMinutes = breakTime;
        staticLongBreakTimeMinutes = longBreakTime;
        staticLongBreakInterval = longBreakInterval;
        staticCompletedPomodoros = completedPomodoros;
        staticTaskTitle = taskTitle != null ? taskTitle : "Pomodoro Session";
        staticTaskId = taskId != null ? taskId : "";

        scheduleAlarmForTimer(context, targetEndTimeMillis, staticTaskTitle, workTime, breakTime, longBreakTime, longBreakInterval, completedPomodoros, staticTaskId, nextTimerMode);

        Intent updateIntent = new Intent(ACTION_TIMER_UPDATED);
        updateIntent.setPackage(context.getPackageName());
        context.sendBroadcast(updateIntent);
    }

    public static void scheduleAlarmForTimer(Context context, long targetEndMillis, String title, int workTime, int breakTime, int longBreakTime, int longBreakInterval, int completedPomodoros, String taskId, int timerMode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PomodoroAlarmReceiver.class);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_TASK_TITLE, title);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_WORK_TIME, workTime);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_BREAK_TIME, breakTime);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_LONG_BREAK_TIME, longBreakTime);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_LONG_BREAK_INTERVAL, longBreakInterval);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_COMPLETED_POMODOROS, completedPomodoros);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_TASK_ID, taskId);
        intent.putExtra(PomodoroAlarmReceiver.EXTRA_TIMER_MODE, timerMode);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(targetEndMillis, pendingIntent);
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, targetEndMillis, pendingIntent);
                    }
                } else {
                    AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(targetEndMillis, pendingIntent);
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                }
            } catch (SecurityException e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetEndMillis, pendingIntent);
            }
        }
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PomodoroAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void scheduleAlarm() {
        scheduleAlarmForTimer(this, targetEndTimeMillis, taskTitle, workTimeMinutes, breakTimeMinutes, longBreakTimeMinutes, longBreakInterval, completedPomodoros, staticTaskId, timerMode);
    }

    private void cancelAlarm() {
        cancelAlarm(this);
    }

    private void updateHeaderUI() {
        if (chipTimerMode != null) {
            if (timerMode == MODE_FOCUS) {
                chipTimerMode.setText(R.string.focus_time);
            } else if (timerMode == MODE_SHORT_BREAK) {
                chipTimerMode.setText(R.string.break_time);
            } else {
                chipTimerMode.setText(R.string.long_break_time);
            }
        }
        if (chipSessionCounter != null) {
            int currentInCycle = (completedPomodoros % longBreakInterval) + 1;
            if (timerMode != MODE_FOCUS) {
                int shownCount = (completedPomodoros % longBreakInterval == 0) ? longBreakInterval : (completedPomodoros % longBreakInterval);
                chipSessionCounter.setText(String.format(Locale.getDefault(), "Completed %d/%d", shownCount, longBreakInterval));
            } else {
                chipSessionCounter.setText(String.format(Locale.getDefault(), "Session %d of %d", currentInCycle, longBreakInterval));
            }
        }
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountdown.setText(timeLeftFormatted);
    }

    private void updateProgress() {
        long totalTime;
        if (timerMode == MODE_FOCUS) {
            totalTime = workTimeMinutes * 60 * 1000L;
        } else if (timerMode == MODE_SHORT_BREAK) {
            totalTime = breakTimeMinutes * 60 * 1000L;
        } else {
            totalTime = longBreakTimeMinutes * 60 * 1000L;
        }
        if (totalTime > 0) {
            int progress = (int) ((timeLeftInMillis * 100) / totalTime);
            cpiTimer.setProgress(progress);
        }
    }

    private void updateRingColor() {
        int colorRes;
        if (timerMode == MODE_FOCUS) {
            colorRes = R.color.focus_accent;
        } else if (timerMode == MODE_SHORT_BREAK) {
            colorRes = R.color.break_accent;
        } else {
            colorRes = R.color.long_break_accent;
        }
        cpiTimer.setColor(getResources().getColor(colorRes, getTheme()));
    }

    private void updateThemeColors() {
        if (timerMode == MODE_FOCUS) {
            chipTimerMode.setChipBackgroundColorResource(R.color.md_sys_color_primary_container);
            chipTimerMode.setTextColor(getResources().getColor(R.color.md_sys_color_on_primary_container, getTheme()));
            btnStartPause.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.focus_accent, getTheme())));
            btnStartPause.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.white, getTheme())));
            btnReset.setTextColor(getResources().getColor(R.color.focus_accent, getTheme()));
            btnReset.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.focus_accent, getTheme())));
        } else if (timerMode == MODE_SHORT_BREAK) {
            chipTimerMode.setChipBackgroundColorResource(R.color.break_accent_container);
            chipTimerMode.setTextColor(getResources().getColor(R.color.break_accent_on_container, getTheme()));
            btnStartPause.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.break_accent, getTheme())));
            btnStartPause.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.white, getTheme())));
            btnReset.setTextColor(getResources().getColor(R.color.break_accent, getTheme()));
            btnReset.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.break_accent, getTheme())));
        } else {
            // Long Break Mode
            chipTimerMode.setChipBackgroundColorResource(R.color.long_break_accent_container);
            chipTimerMode.setTextColor(getResources().getColor(R.color.long_break_accent_on_container, getTheme()));
            btnStartPause.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.long_break_accent, getTheme())));
            btnStartPause.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.white, getTheme())));
            btnReset.setTextColor(getResources().getColor(R.color.long_break_accent, getTheme()));
            btnReset.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.long_break_accent, getTheme())));
        }
    }

    private void playAlarmAndVibrate() {
    }
}
