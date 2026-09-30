package dev.jaypee.anotherpomodoroapp;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;


import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_WORK_TIME = "extra_work_time";
    public static final String EXTRA_BREAK_TIME = "extra_break_time";
    private static final String TAG = "TimerActivity";

    // Static state to persist across activity pauses/destroys when navigating home
    private static long targetEndTimeMillis = 0;
    private static boolean staticTimerRunning = false;
    private static boolean staticWorkMode = true;
    private static int staticWorkTimeMinutes = 25;
    private static int staticBreakTimeMinutes = 5;
    private static String staticTaskTitle = "Pomodoro Session";

    private TextView tvCountdown;
    private Chip chipTimerMode;
    private CircularProgressView cpiTimer;
    private Button btnStartPause;
    private TextView tvWorkDuration;
    private TextView tvBreakDuration;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private boolean isTimerRunning = false;
    private boolean isWorkMode = true;

    private int workTimeMinutes = 25;
    private int breakTimeMinutes = 5;
    private String taskTitle = "Pomodoro Session";

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

            staticTaskTitle = taskTitle;
            staticWorkTimeMinutes = workTimeMinutes;
            staticBreakTimeMinutes = breakTimeMinutes;
            staticTimerRunning = false;
            staticWorkMode = true;
            isWorkMode = true;
            timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        } else if (staticTimerRunning) {
            taskTitle = staticTaskTitle;
            workTimeMinutes = staticWorkTimeMinutes;
            breakTimeMinutes = staticBreakTimeMinutes;
            isWorkMode = staticWorkMode;
            long remaining = targetEndTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                timeLeftInMillis = remaining;
                isTimerRunning = true;
            } else {
                timeLeftInMillis = 0;
                isTimerRunning = false;
                staticTimerRunning = false;
            }
        } else {
            timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        }

        tvCountdown = findViewById(R.id.tv_countdown);
        chipTimerMode = findViewById(R.id.chip_timer_mode);
        cpiTimer = findViewById(R.id.cpi_timer);
        TextView tvTimerTaskTitle = findViewById(R.id.tv_timer_task_title);
        btnStartPause = findViewById(R.id.btn_start_pause);
        Button btnReset = findViewById(R.id.btn_reset);
        MaterialButton ibBack = findViewById(R.id.ib_back);
        tvWorkDuration = findViewById(R.id.tv_work_duration);
        tvBreakDuration = findViewById(R.id.tv_break_duration);

        ibBack.setOnClickListener(v -> finish());

        tvTimerTaskTitle.setText(taskTitle);
        chipTimerMode.setText(isWorkMode ? R.string.focus_time : R.string.break_time);
        tvWorkDuration.setText(workTimeMinutes + " min");
        tvBreakDuration.setText(breakTimeMinutes + " min");
        updateCountDownText();
        updateProgress();

        if (isTimerRunning) {
            btnStartPause.setText(R.string.pause);
        } else {
            btnStartPause.setText(R.string.start);
        }

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (staticTimerRunning) {
            long remaining = targetEndTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                timeLeftInMillis = remaining;
                if (!isTimerRunning) {
                    isTimerRunning = true;
                    btnStartPause.setText(R.string.pause);
                }
                startCountdown(timeLeftInMillis);
            } else {
                handleTimerFinished();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void startTimer() {
        targetEndTimeMillis = System.currentTimeMillis() + timeLeftInMillis;
        staticTimerRunning = true;
        staticWorkMode = isWorkMode;
        staticTaskTitle = taskTitle;
        staticWorkTimeMinutes = workTimeMinutes;
        staticBreakTimeMinutes = breakTimeMinutes;

        startCountdown(timeLeftInMillis);
        isTimerRunning = true;
        btnStartPause.setText(R.string.pause);
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

        if (isWorkMode) {
            Toast.makeText(TimerActivity.this, "Focus session finished! Time for a break.", Toast.LENGTH_LONG).show();
            isWorkMode = false;
            chipTimerMode.setText(R.string.break_time);
            timeLeftInMillis = breakTimeMinutes * 60 * 1000L;
        } else {
            Toast.makeText(TimerActivity.this, "Break finished! Back to focus.", Toast.LENGTH_LONG).show();
            isWorkMode = true;
            chipTimerMode.setText(R.string.focus_time);
            timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        }
        updateCountDownText();
        updateProgress();
        startTimer();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        staticTimerRunning = false;
        btnStartPause.setText(R.string.resume);
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        staticTimerRunning = false;
        isWorkMode = true;
        chipTimerMode.setText(R.string.focus_time);
        timeLeftInMillis = workTimeMinutes * 60 * 1000L;
        updateCountDownText();
        updateProgress();
        btnStartPause.setText(R.string.start);
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountdown.setText(timeLeftFormatted);
    }

    private void updateProgress() {
        long totalTime = isWorkMode ? workTimeMinutes * 60 * 1000L : breakTimeMinutes * 60 * 1000L;
        if (totalTime > 0) {
            int progress = (int) ((timeLeftInMillis * 100) / totalTime);
            cpiTimer.setProgress(progress);
        }
    }

    private void playAlarmAndVibrate() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            if (r != null) {
                r.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
        }

        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }

            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
                } else {
                    @SuppressWarnings("deprecation")
                    long[] pattern = {0, 500, 200, 500};
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating device", e);
        }
    }
}