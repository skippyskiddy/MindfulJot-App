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
    private static final int NOON_HOUR = 13;
    private static final int EVENING_HOUR = 19;
    private static final int ONCE_DAILY_HOUR = 14; // 2 PM for fixed once daily option

    // Random time range for once daily option (8 AM to 6 PM)
    private static final int RANDOM_START_HOUR = 8;
    private static final int RANDOM_END_HOUR = 18; // 6 PM

    /**
     * Schedule notifications based on user preference
     */
    public static void scheduleNotifications(Context context, String preference, String userName) {
        Log.d(TAG, "Scheduling notifications for preference: " + preference + ", user: " + userName);

        // Create the notification channel first (will only take effect on Android 8.0+)
        NotificationHelper.createNotificationChannel(context);

        // Check notification permissions - if not granted, we can't schedule
        if (!NotificationHelper.canShowNotifications(context)) {
            Log.e(TAG, "Cannot schedule notifications - permissions not granted or channel disabled");
            return;
        }

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
                Log.d(TAG, "User preference is 'none', not scheduling notifications");
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
        calendar.set(Calendar.MILLISECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Scheduling once daily notification for: " + calendar.getTime());

        // Create intent for the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("notification_type", "once");
        // Add unique action to ensure the intent is unique
        intent.setAction("edu.northeastern.numad25sp_group4.ONCE_DAILY_NOTIFICATION");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ONCE_DAILY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        scheduleAlarm(context, calendar.getTimeInMillis(), pendingIntent);
    }

    /**
     * Schedule two notifications per day (morning and evening)
     */
    private static void scheduleTwiceDailyNotifications(Context context, String userName) {
        // Schedule morning notification
        scheduleNotificationAt(context, MORNING_HOUR, 0, MORNING_REQUEST_CODE, "twice", userName,
                "edu.northeastern.numad25sp_group4.MORNING_NOTIFICATION");

        // Schedule evening notification
        scheduleNotificationAt(context, EVENING_HOUR, 0, EVENING_REQUEST_CODE, "twice", userName,
                "edu.northeastern.numad25sp_group4.EVENING_NOTIFICATION");
    }

    /**
     * Schedule three notifications per day (morning, noon, and evening)
     */
    private static void scheduleThriceDailyNotifications(Context context, String userName) {
        // Schedule morning notification
        scheduleNotificationAt(context, MORNING_HOUR, 0, MORNING_REQUEST_CODE, "thrice", userName,
                "edu.northeastern.numad25sp_group4.MORNING_NOTIFICATION");

        // Schedule noon notification
        scheduleNotificationAt(context, NOON_HOUR, 0, NOON_REQUEST_CODE, "thrice", userName,
                "edu.northeastern.numad25sp_group4.NOON_NOTIFICATION");

        // Schedule evening notification
        scheduleNotificationAt(context, EVENING_HOUR, 0, EVENING_REQUEST_CODE, "thrice", userName,
                "edu.northeastern.numad25sp_group4.EVENING_NOTIFICATION");
    }

    /**
     * Helper method to schedule a notification at a specific time
     */
    private static void scheduleNotificationAt(Context context, int hour, int minute,
                                               int requestCode, String notificationType,
                                               String userName, String intentAction) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Scheduling notification for " + notificationType + " at: " + calendar.getTime());

        // Create intent for the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("user_name", userName);
        intent.putExtra("notification_type", notificationType);
        intent.setAction(intentAction);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        scheduleAlarm(context, calendar.getTimeInMillis(), pendingIntent);
    }

    /**
     * Helper method to schedule an alarm with the appropriate method for the Android version
     */
    private static void scheduleAlarm(Context context, long triggerTimeMillis, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "Could not get AlarmManager service");
            return;
        }

        try {
            // Pick the best method to schedule the alarm based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()) {
                // For Android 12+, we need to check the exact alarm permission
                Log.w(TAG, "Cannot schedule exact alarms, using inexact method");

                // Fallback to a reasonable alternative
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0+
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm with setExactAndAllowWhileIdle");
            } else {
                // For older versions
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm with setExact");
            }

            // Log successful scheduling
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(triggerTimeMillis);
            Log.d(TAG, "Successfully scheduled alarm for: " + cal.getTime());

        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
        }
    }

    /**
     * Cancel all scheduled notifications
     */
    public static void cancelAllNotifications(Context context) {
        cancelNotification(context, MORNING_REQUEST_CODE, "edu.northeastern.numad25sp_group4.MORNING_NOTIFICATION");
        cancelNotification(context, NOON_REQUEST_CODE, "edu.northeastern.numad25sp_group4.NOON_NOTIFICATION");
        cancelNotification(context, EVENING_REQUEST_CODE, "edu.northeastern.numad25sp_group4.EVENING_NOTIFICATION");
        cancelNotification(context, ONCE_DAILY_REQUEST_CODE, "edu.northeastern.numad25sp_group4.ONCE_DAILY_NOTIFICATION");
        Log.d(TAG, "Cancelled all scheduled notifications");
    }

    /**
     * Cancel a specific notification by request code
     */
    private static void cancelNotification(Context context, int requestCode, String intentAction) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(intentAction);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Cancelled notification with request code: " + requestCode);
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling notification: " + e.getMessage());
            }
        }
    }

    /**
     * Check if we have permission to schedule exact alarms (Android 12+)
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Permission automatically granted on older Android versions
    }
}