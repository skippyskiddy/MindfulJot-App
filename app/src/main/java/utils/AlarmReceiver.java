package utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver to handle the notification alarm
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received alarm for notification");

        // Get user name from intent
        String userName = intent.getStringExtra("user_name");

//        // Check if this is a test notification
//        boolean isTestNotification = intent.getBooleanExtra("test_notification", false);
//
//        if (isTestNotification) {
//            Log.d(TAG, "Handling TEST notification alarm");
//        } else {
//            Log.d(TAG, "Handling REGULAR notification alarm");
//        }
//
//        // Show the notification
//        NotificationHelper.showNotification(context, userName);
//
//        // If not a test notification, reschedule the next notification based on notification type
//        if (!isTestNotification) {
//            String notificationType = intent.getStringExtra("notification_type");
//            if (notificationType != null) {
//                // This will reschedule today's notifications if we haven't shown them all yet
//                NotificationScheduler.scheduleNotifications(context, notificationType, userName);
//            }
//        }
    }
}