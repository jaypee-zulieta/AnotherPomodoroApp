package dev.jaypee.anotherpomodoroapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.util.Locale;

public class PomodoroService extends Service {

    private static final String TAG = "PomodoroService";
    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "dev.jaypee.anotherpomodoroapp.action.START";
    public static final String ACTION_PAUSE = "dev.jaypee.anotherpomodoroapp.action.PAUSE";
    public static final String ACTION_RESUME = "dev.jaypee.anotherpomodoroapp.action.RESUME";
    public static final String ACTION_RESET = "dev.jaypee.anotherpomodoroapp.action.RESET";
    public static final String ACTION_STOP = "dev.jaypee.anotherpomodoroapp.action.STOP";
    public static final String ACTION_QUERY_STATE = "dev.jaypee.anotherpomodoroapp.action.QUERY_STATE";

    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_WORK_TIME = "extra_work_time";
    public static final String EXTRA_BREAK_TIME = "extra_break_time";

    public static final String BROADCAST_UPDATE = "dev.jaypee.anotherpomodoroapp.broadcast.UPDATE";
    public static final String EXTRA_TIME_LEFT = "extra_time_left";
    public static final String EXTRA_IS_WORK_MODE = "extra_is_work_mode";
    public static final String EXTRA_IS_RUNNING = "extra_is_running";
    public static final String EXTRA_TASK_NAME = "extra_task_name";

    // Static state for instant UI access across activities
    public static boolean staticIsRunning = false;
    public static boolean staticIsWorkMode = true;
    public static long staticTimeLeftInMillis = 25 * 60 * 1000L;
    public static String staticTaskTitle = "Pomodoro Session";

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 25 * 60 * 1000L;
    private long workTimeMillis = 25 * 60 * 1000L;
    private long breakTimeMillis = 5 * 60 * 1000L;
    private boolean isWorkMode = true;
    private boolean isRunning = false;
    private String taskTitle = "Pomodoro Session";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    if (intent.hasExtra(EXTRA_TASK_TITLE)) {
                        taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
                    }
                    int workMins = intent.getIntExtra(EXTRA_WORK_TIME, 25);
                    int breakMins = intent.getIntExtra(EXTRA_BREAK_TIME, 5);
                    workTimeMillis = workMins * 60 * 1000L;
                    breakTimeMillis = breakMins * 60 * 1000L;
                    timeLeftInMillis = workTimeMillis;
                    isWorkMode = true;
                    startTimer();
                    break;
                case ACTION_PAUSE:
                    pauseTimer();
                    break;
                case ACTION_RESUME:
                    startTimer();
                    break;
                case ACTION_RESET:
                    resetTimer();
                    break;
                case ACTION_STOP:
                    stopForegroundService();
                    break;
                case ACTION_QUERY_STATE:
                    broadcastUpdate();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro Timer Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active Pomodoro timer countdown");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, TimerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title = (isWorkMode ? "Focus: " : "Break: ") + taskTitle;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = true;
        staticIsRunning = true;
        staticIsWorkMode = isWorkMode;
        staticTaskTitle = taskTitle;
        staticTimeLeftInMillis = timeLeftInMillis;

        startForegroundServiceWithNotification(formatTime(timeLeftInMillis));

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                staticTimeLeftInMillis = timeLeftInMillis;
                String timeStr = formatTime(timeLeftInMillis);
                updateNotification(timeStr);
                broadcastUpdate();
            }

            @Override
            public void onFinish() {
                handleTimerFinished();
            }
        }.start();
        broadcastUpdate();
    }

    @SuppressLint("InlinedApi")
    private void startForegroundServiceWithNotification(String timeStr) {
        Notification notification = createNotification(timeStr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String timeStr) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(timeStr));
        }
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        staticIsRunning = false;
        staticTimeLeftInMillis = timeLeftInMillis;
        updateNotification(formatTime(timeLeftInMillis) + " (Paused)");
        broadcastUpdate();
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        staticIsRunning = false;
        isWorkMode = true;
        staticIsWorkMode = true;
        timeLeftInMillis = workTimeMillis;
        staticTimeLeftInMillis = timeLeftInMillis;
        updateNotification(formatTime(timeLeftInMillis));
        broadcastUpdate();
    }

    private void handleTimerFinished() {
        playAlarmAndVibrate();

        if (isWorkMode) {
            isWorkMode = false;
            timeLeftInMillis = breakTimeMillis;
        } else {
            isWorkMode = true;
            timeLeftInMillis = workTimeMillis;
        }
        staticIsWorkMode = isWorkMode;
        staticTimeLeftInMillis = timeLeftInMillis;
        startTimer();
    }

    private void stopForegroundService() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        staticIsRunning = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private String formatTime(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void broadcastUpdate() {
        Intent intent = new Intent(BROADCAST_UPDATE);
        intent.putExtra(EXTRA_TIME_LEFT, timeLeftInMillis);
        intent.putExtra(EXTRA_IS_WORK_MODE, isWorkMode);
        intent.putExtra(EXTRA_IS_RUNNING, isRunning);
        intent.putExtra(EXTRA_TASK_NAME, taskTitle);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        staticIsRunning = false;
        super.onDestroy();
    }

    private void playAlarmAndVibrate() {
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
