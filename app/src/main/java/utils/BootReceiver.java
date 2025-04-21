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
        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            Log.d(TAG, "Device booted, rescheduling notifications");

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
                                Log.d(TAG, "Found notification preference: " + preference);
                                NotificationScheduler.scheduleNotifications(context, preference, name);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error getting user data: " + error.getMessage());
                    }
                });
            }
        }
    }
}