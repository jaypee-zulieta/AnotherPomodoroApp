package dev.jaypee.anotherpomodoroapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PomodoroAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_DISMISS = "dev.jaypee.anotherpomodoroapp.action.DISMISS_ALARM";
    public static final String ACTION_START_BREAK = "dev.jaypee.anotherpomodoroapp.action.START_BREAK";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_WORK_TIME = "extra_work_time";
    public static final String EXTRA_BREAK_TIME = "extra_break_time";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_IS_WORK_MODE = "extra_is_work_mode";
    private static final String CHANNEL_ID = "pomodoro_alarm_channel";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_DISMISS.equals(action)) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIFICATION_ID);
                }
                return;
            } else if (ACTION_START_BREAK.equals(action)) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIFICATION_ID);
                }
                String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
                int workTime = intent.getIntExtra(EXTRA_WORK_TIME, 25);
                int breakTime = intent.getIntExtra(EXTRA_BREAK_TIME, 5);
                String taskId = intent.getStringExtra(EXTRA_TASK_ID);
                boolean wasWorkMode = intent.getBooleanExtra(EXTRA_IS_WORK_MODE, true);

                boolean nextWorkMode = !wasWorkMode;
                TimerActivity.startTimerFromBackground(context, taskTitle, workTime, breakTime, taskId, nextWorkMode);

                String toastText = nextWorkMode ? "Focus session started!" : "Break started!";
                android.widget.Toast.makeText(context, toastText, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String taskTitle = intent != null ? intent.getStringExtra(EXTRA_TASK_TITLE) : "Pomodoro Session";
        if (taskTitle == null || taskTitle.isEmpty()) {
            taskTitle = "Pomodoro Session";
        }
        int workTime = intent != null ? intent.getIntExtra(EXTRA_WORK_TIME, 25) : 25;
        int breakTime = intent != null ? intent.getIntExtra(EXTRA_BREAK_TIME, 5) : 5;
        String taskId = intent != null ? intent.getStringExtra(EXTRA_TASK_ID) : "";
        boolean isWorkMode = intent != null ? intent.getBooleanExtra(EXTRA_IS_WORK_MODE, true) : true;

        createNotificationChannel(context);

        String notificationTitle;
        String notificationText;
        String actionText;
        boolean openBreak;

        if (isWorkMode) {
            notificationTitle = "Work Timer Finished!";
            notificationText = "Focus session ended for: " + taskTitle;
            actionText = "Start Break";
            openBreak = true;
        } else {
            notificationTitle = "Break Finished!";
            notificationText = "Break ended for: " + taskTitle;
            actionText = "Start Focus";
            openBreak = false;
        }

        // Intent for clicking notification content (opens TimerActivity)
        Intent contentIntent = new Intent(context, TimerActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        contentIntent.putExtra(TimerActivity.EXTRA_TASK_TITLE, taskTitle);
        contentIntent.putExtra(TimerActivity.EXTRA_WORK_TIME, workTime);
        contentIntent.putExtra(TimerActivity.EXTRA_BREAK_TIME, breakTime);
        contentIntent.putExtra(TimerActivity.EXTRA_TASK_ID, taskId);
        contentIntent.putExtra(TimerActivity.EXTRA_OPEN_BREAK, openBreak);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Intent for Dismiss action button
        Intent dismissIntent = new Intent(context, PomodoroAlarmReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Intent for Start Break/Focus action button
        Intent startBreakIntent = new Intent(context, PomodoroAlarmReceiver.class);
        startBreakIntent.setAction(ACTION_START_BREAK);
        startBreakIntent.putExtra(EXTRA_TASK_TITLE, taskTitle);
        startBreakIntent.putExtra(EXTRA_WORK_TIME, workTime);
        startBreakIntent.putExtra(EXTRA_BREAK_TIME, breakTime);
        startBreakIntent.putExtra(EXTRA_TASK_ID, taskId);
        startBreakIntent.putExtra(EXTRA_IS_WORK_MODE, isWorkMode);
        PendingIntent startBreakPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                startBreakIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_close,
                        "Dismiss",
                        dismissPendingIntent
                ).build())
                .addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_play,
                        actionText,
                        startBreakPendingIntent
                ).build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setFullScreenIntent(contentPendingIntent, true);
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro Alarm Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Heads-up notifications when work timer ends");
            channel.enableVibration(true);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
