package utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

/**
 * BroadcastReceiver to handle device boot and reschedule notifications
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "BootReceiver received action: " + action);

        if (action != null && (
                action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                        action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                        action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
                        action.equals("android.intent.action.QUICKBOOT_POWERON"))) {  // For some Xiaomi devices

            Log.d(TAG, "Device booted or app updated, rescheduling notifications");

            // Check if we have notification permissions
            if (!NotificationHelper.canShowNotifications(context)) {
                Log.e(TAG, "Cannot reschedule notifications - permissions not granted or channel disabled");
                return;
            }

            // Create notification channel
            NotificationHelper.createNotificationChannel(context);

            // Check if user is logged in
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                Log.d(TAG, "User is logged in, getting notification preferences");

                // Get notification preferences from Firebase
                FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
                firebaseHelper.getUserData(currentUser.getUid(), new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String preference = snapshot.child("notificationPreference").getValue(String.class);
                            String name = snapshot.child("name").getValue(String.class);

                            if (preference != null && !preference.equals("none")) {
                                Log.d(TAG, "Found notification preference: " + preference + ", name: " + name);

                                // Reschedule the notifications
                                NotificationScheduler.scheduleNotifications(context, preference, name);
                            } else {
                                Log.d(TAG, "User has notification preference set to none or null");
                            }
                        } else {
                            Log.d(TAG, "User data not found in Firebase");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error getting user data: " + error.getMessage());
                    }
                });
            } else {
                Log.d(TAG, "No user is logged in, skipping notification scheduling");
            }
        }
    }
}