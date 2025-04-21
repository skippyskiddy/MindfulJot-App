package utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Random;

/**
 * Utility class to schedule notifications based on user preferences
 */
public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";

    // Request code constants for different notification times
    public static final int MORNING_REQUEST_CODE = 1001;
    public static final int NOON_REQUEST_CODE = 1002;
    public static final int EVENING_REQUEST_CODE = 1003;
    public static final int ONCE_DAILY_REQUEST_CODE = 1004;

    // Time constants (24-hour format)
    private static final int MORNING_HOUR = 9;
    private static final int NOON_HOUR = 12;
    private static final int EVENING_HOUR = 19;
    private static final int ONCE_DAILY_HOUR = 14; // 2 PM for fixed once daily option

    // Random time range for once daily option (8 AM to 6 PM)
    private static final int RANDOM_START_HOUR = 8;
    private static final int RANDOM_END_HOUR = 18; // 6 PM

    /**
     * Schedule notifications based on user preference
     */
    public static void scheduleNotifications(Context context, String preference, String userName) {
        Log.d(TAG, "Scheduling notifications for preference: " + preference);

        // Create the notification channel first (will only take effect on Android 8.0+)
        NotificationHelper.createNotificationChannel(context);

        // Cancel any existing notifications first
        cancelAllNotifications(context);

        // Schedule based on preference
        switch (preference) {
            case "once":
                scheduleOnceDailyNotification(context, userName);
                break;
            case "twice":
                scheduleTwiceDailyNotifications(context, userName);
                break;
            case "thrice":
                scheduleThriceDailyNotifications(context, userName);
                break;
            case "none":
                // No notifications, so don't schedule any
                break;
            default:
                Log.e(TAG, "Unknown notification preference: " + preference);
                break;
        }
    }

    /**
     * Schedule a single notification per day (either at 2 PM or random time)
     */
    private static void scheduleOnceDailyNotification(Context context, String userName) {
        Calendar calendar = Calendar.getInstance();

        // Use a random time between 8 AM and 6 PM
        Random random = new Random();
        int hour = random.nextInt(RANDOM_END_HOUR - RANDOM_START_HOUR + 1) + RANDOM_START_HOUR;
        int minute = random.nextInt(60);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Scheduling once daily notification for: " + calendar.getTime());

        // Create intent for the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("notification_type", "once");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ONCE_DAILY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * Schedule two notifications per day (morning and evening)
     */
    private static void scheduleTwiceDailyNotifications(Context context, String userName) {
        // Schedule morning notification
        scheduleNotificationAt(context, MORNING_HOUR, 0, MORNING_REQUEST_CODE, "twice", userName);

        // Schedule evening notification
        scheduleNotificationAt(context, EVENING_HOUR, 0, EVENING_REQUEST_CODE, "twice", userName);
    }

    /**
     * Schedule three notifications per day (morning, noon, and evening)
     */
    private static void scheduleThriceDailyNotifications(Context context, String userName) {
        // Schedule morning notification
        scheduleNotificationAt(context, MORNING_HOUR, 0, MORNING_REQUEST_CODE, "thrice", userName);

        // Schedule noon notification
        scheduleNotificationAt(context, NOON_HOUR, 0, NOON_REQUEST_CODE, "thrice", userName);

        // Schedule evening notification
        scheduleNotificationAt(context, EVENING_HOUR, 0, EVENING_REQUEST_CODE, "thrice", userName);
    }

    /**
     * Helper method to schedule a notification at a specific time
     */
    private static void scheduleNotificationAt(Context context, int hour, int minute,
                                               int requestCode, String notificationType, String userName) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Scheduling notification for " + notificationType + " at: " + calendar.getTime());

        // Create intent for the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("notification_type", notificationType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * Cancel all scheduled notifications
     */
    public static void cancelAllNotifications(Context context) {
        cancelNotification(context, MORNING_REQUEST_CODE);
        cancelNotification(context, NOON_REQUEST_CODE);
        cancelNotification(context, EVENING_REQUEST_CODE);
        cancelNotification(context, ONCE_DAILY_REQUEST_CODE);
    }

    /**
     * Cancel a specific notification by request code
     */
    private static void cancelNotification(Context context, int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}