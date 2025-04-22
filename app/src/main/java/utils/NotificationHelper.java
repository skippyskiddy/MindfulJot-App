package utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.northeastern.numad25sp_group4.HomeActivity;
import edu.northeastern.numad25sp_group4.R;

/**
 * Helper class to manage notifications in the app
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "mindfuljot_channel";
    private static final String CHANNEL_NAME = "MindfulJot Reminders";
    private static final String CHANNEL_DESCRIPTION = "Reminders to log your emotions";

    public static final int NOTIFICATION_ID = 1001;
    private static final int NOTIFICATION_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;

    /**
     * Creates the notification channel for Android 8.0 and higher
     */
    public static void createNotificationChannel(Context context) {
        // Create the notification channel only on API 26+ (Android 8.0 and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NOTIFICATION_IMPORTANCE
            );

            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            channel.setBypassDnd(true);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    /**
     * Check if notification permission is granted for Android 13+ devices
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permission automatically granted on older Android versions
    }

    /**
     * Check if the notification channel is enabled by the user
     */
    public static boolean isNotificationChannelEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
                if (channel != null) {
                    return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
                }
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Shows a notification to the user
     */
    public static void showNotification(Context context, String userName) {
        // Log the attempt to show a notification
        Log.d(TAG, "Attempting to show notification for user: " +
                (userName != null ? userName : "unknown"));

        if (!hasNotificationPermission(context)) {
            Log.e(TAG, "Notification permission not granted");
            return;
        }

        // Create intent to open the app when notification is clicked
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Personalize the notification message
        String message = (userName != null && !userName.isEmpty())
                ? "Hi " + userName + ", remember to log how you feel."
                : "Hi there, remember to log how you feel.";

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("MindfulJot")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 250, 250, 250})
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification shown successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to show notification due to permission: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification: " + e.getMessage());
        }
    }

    /**
     * Check if the device has notification permission and notification channels enabled
     */
    public static boolean canShowNotifications(Context context) {
        boolean hasPermission = hasNotificationPermission(context);
        boolean channelEnabled = isNotificationChannelEnabled(context);
        Log.d(TAG, "Can show notifications: permission=" + hasPermission +
                ", channel enabled=" + channelEnabled);
        return hasPermission && channelEnabled;
    }
}