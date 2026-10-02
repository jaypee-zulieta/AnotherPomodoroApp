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
                // Does nothing for now as requested, but dismisses the notification
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIFICATION_ID);
                }
                return;
            }
        }

        String taskTitle = intent != null ? intent.getStringExtra(EXTRA_TASK_TITLE) : "Pomodoro Session";
        if (taskTitle == null || taskTitle.isEmpty()) {
            taskTitle = "Pomodoro Session";
        }

        createNotificationChannel(context);

        // Intent for clicking notification content (opens TimerActivity)
        Intent contentIntent = new Intent(context, TimerActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

        // Intent for Start Break action button
        Intent startBreakIntent = new Intent(context, PomodoroAlarmReceiver.class);
        startBreakIntent.setAction(ACTION_START_BREAK);
        PendingIntent startBreakPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                startBreakIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Work Timer Finished!")
                .setContentText("Focus session ended for: " + taskTitle)
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
                        "Start Break",
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
