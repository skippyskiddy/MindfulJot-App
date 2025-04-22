package utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.PowerManager;

/**
 * BroadcastReceiver to handle the notification alarm
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final long WAKELOCK_TIMEOUT = 10000; // 10 seconds

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received alarm for notification with action: " + intent.getAction());

        // Acquire a wake lock to ensure the device doesn't go back to sleep before we finish
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;

        try {
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "MindfulJot:NotificationWakeLock");
                wakeLock.acquire(WAKELOCK_TIMEOUT);
                Log.d(TAG, "Wake lock acquired");
            }

            // Get user name from intent
            String userName = intent.getStringExtra("user_name");
            if (userName == null) {
                userName = ""; // Ensure userName is never null
            }
            Log.d(TAG, "Showing notification for user: " + userName);

            // Show the notification
            NotificationHelper.showNotification(context, userName);

            // Schedule the next notification if appropriate
            handleNextNotification(context, intent);

        } catch (Exception e) {
            Log.e(TAG, "Error in AlarmReceiver: " + e.getMessage());
        } finally {
            // Release wake lock if we acquired one
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Wake lock released");
            }
        }
    }

    /**
     * Handle scheduling the next notification
     */
    private void handleNextNotification(Context context, Intent intent) {
        // Check if we still have notification permissions
        if (!NotificationHelper.canShowNotifications(context)) {
            Log.e(TAG, "Cannot reschedule notifications - permissions not granted or channel disabled");
            return;
        }

        String notificationType = intent.getStringExtra("notification_type");
        String userName = intent.getStringExtra("user_name");

        if (notificationType == null) {
            Log.e(TAG, "No notification type specified in the intent");
            return;
        }

        // Schedule for tomorrow (same time)
        Log.d(TAG, "Rescheduling next notification for type: " + notificationType);
        NotificationScheduler.scheduleNotifications(context, notificationType, userName);
    }
}