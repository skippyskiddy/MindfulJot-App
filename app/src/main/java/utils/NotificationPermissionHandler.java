package utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Utility class to handle notification permissions and exact alarm permissions
 */
public class NotificationPermissionHandler {

    private static final String TAG = "NotifPermissionHandler";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    /**
     * Check and request notification permission if needed for Android 13+
     */
    public static boolean checkNotificationPermission(Activity activity) {
        // For Android 13+ we need to check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission for Android 13+");

                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    // Show rationale then request
                    showNotificationPermissionRationale(activity);
                } else {
                    // Request directly
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_NOTIFICATION_PERMISSION);
                }
                return false;
            }
        }

        // Check if notification channel is enabled
        return NotificationHelper.isNotificationChannelEnabled(activity);
    }

    /**
     * Show a dialog explaining why we need notification permission
     */
    private static void showNotificationPermissionRationale(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Notification Permission")
                .setMessage("MindfulJot needs notification permission to send you reminders to log your emotions. " +
                        "Without this permission, you won't receive any notifications.")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                REQUEST_NOTIFICATION_PERMISSION);
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * Check for exact alarm permission on Android 12+
     */
    public static boolean checkExactAlarmPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!NotificationScheduler.canScheduleExactAlarms(activity)) {
                Log.d(TAG, "Exact alarm permission not granted for Android 12+");
                showExactAlarmPermissionDialog(activity);
                return false;
            }
        }
        return true;
    }

    /**
     * Show a dialog explaining why we need exact alarm permission and take user to settings
     */
    private static void showExactAlarmPermissionDialog(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Exact Alarm Permission")
                .setMessage("MindfulJot needs permission to schedule exact alarms to send notifications at specific times. " +
                        "Please enable this in your device settings.")
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }
}