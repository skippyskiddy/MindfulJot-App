package utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.northeastern.numad25sp_group4.HomeActivity;
import edu.northeastern.numad25sp_group4.R;

/**
 * Helper class to manage notifications in the app
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    private static final String CHANNEL_ID = "mindfuljot_channel";
    private static final String CHANNEL_NAME = "MindfulJot Reminders";
    private static final String CHANNEL_DESCRIPTION = "Reminders to log your emotions";

    public static final int NOTIFICATION_ID = 1001;

    /**
     * Creates the notification channel for Android 8.0 and higher
     */
    public static void createNotificationChannel(Context context) {
        Log.d(TAG, "Creating notification channel");

        // Create the notification channel only on API 26+ (Android 8.0 and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription(CHANNEL_DESCRIPTION);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created successfully");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } else {
            Log.d(TAG, "Notification channel not needed for this Android version");
        }
    }

    /**
     * Shows a notification to the user
     */
    public static void showNotification(Context context, String userName) {
        Log.d(TAG, "Showing notification for user: " + userName);

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
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification sent successfully");
        } catch (SecurityException e) {
            // This can happen if the user revoked notification permissions
            Log.e(TAG, "Security exception when showing notification: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Exception when showing notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}