package edu.northeastern.numad25sp_group4;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import utils.FirebaseHelper;
import utils.LoginManager;
import utils.NotificationHelper;
import utils.NotificationScheduler;

public class NotificationSettingsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationSettings";

    private MaterialCardView cardOnceDaily, cardTwiceDaily, cardThreeTimes, cardNoReminders;
    private ImageView ivOnceCheck, ivTwiceCheck, ivThreeCheck, ivNoCheck;
    private Button btnSaveSettings;
    private ImageButton btnBack;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String currentPreference = "none";
    private String selectedPreference = "none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Initialize views
        initViews();

        // Load current preferences
        loadCurrentPreference();

        // Set up listeners
        setupListeners();

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);
    }

    private void initViews() {
        cardOnceDaily = findViewById(R.id.card_once_daily);
        cardTwiceDaily = findViewById(R.id.card_twice_daily);
        cardThreeTimes = findViewById(R.id.card_three_times);
        cardNoReminders = findViewById(R.id.card_no_reminders);

        ivOnceCheck = findViewById(R.id.iv_once_check);
        ivTwiceCheck = findViewById(R.id.iv_twice_check);
        ivThreeCheck = findViewById(R.id.iv_three_check);
        ivNoCheck = findViewById(R.id.iv_no_check);

        btnSaveSettings = findViewById(R.id.btn_save_settings);
        btnBack = findViewById(R.id.btn_back);

//        // Test buttons
//        btnTestImmediate = findViewById(R.id.btn_test_immediate);
//        btnTestDelayed = findViewById(R.id.btn_test_delayed);
    }

    private void loadCurrentPreference() {
        if (firebaseHelper.getCurrentUser() != null) {
            String userId = firebaseHelper.getCurrentUser().getUid();

            firebaseHelper.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String preference = snapshot.child("notificationPreference").getValue(String.class);

                        if (preference != null) {
                            currentPreference = preference;
                            selectedPreference = preference;

                            // Update the UI to show selected option
                            updateSelectionUI(preference);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(NotificationSettingsActivity.this,
                            "Error loading preferences: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateSelectionUI(String preference) {
        // Hide all check marks first
        ivOnceCheck.setVisibility(View.INVISIBLE);
        ivTwiceCheck.setVisibility(View.INVISIBLE);
        ivThreeCheck.setVisibility(View.INVISIBLE);
        ivNoCheck.setVisibility(View.INVISIBLE);

        // Reset all card strokes
        cardOnceDaily.setStrokeWidth(0);
        cardTwiceDaily.setStrokeWidth(0);
        cardThreeTimes.setStrokeWidth(0);
        cardNoReminders.setStrokeWidth(0);

        // Set the appropriate checkmark visible and update card
        switch (preference) {
            case "once":
                ivOnceCheck.setVisibility(View.VISIBLE);
                cardOnceDaily.setStrokeWidth(4);
                cardOnceDaily.setStrokeColor(getResources().getColor(R.color.white));
                break;
            case "twice":
                ivTwiceCheck.setVisibility(View.VISIBLE);
                cardTwiceDaily.setStrokeWidth(4);
                cardTwiceDaily.setStrokeColor(getResources().getColor(R.color.white));
                break;
            case "thrice":
                ivThreeCheck.setVisibility(View.VISIBLE);
                cardThreeTimes.setStrokeWidth(4);
                cardThreeTimes.setStrokeColor(getResources().getColor(R.color.white));
                break;
            case "none":
            default:
                ivNoCheck.setVisibility(View.VISIBLE);
                cardNoReminders.setStrokeWidth(4);
                cardNoReminders.setStrokeColor(getResources().getColor(R.color.white));
                break;
        }
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Card click listeners
        cardOnceDaily.setOnClickListener(v -> {
            selectedPreference = "once";
            updateSelectionUI(selectedPreference);
        });

        cardTwiceDaily.setOnClickListener(v -> {
            selectedPreference = "twice";
            updateSelectionUI(selectedPreference);
        });

        cardThreeTimes.setOnClickListener(v -> {
            selectedPreference = "thrice";
            updateSelectionUI(selectedPreference);
        });

        cardNoReminders.setOnClickListener(v -> {
            selectedPreference = "none";
            updateSelectionUI(selectedPreference);
        });

        // Save button click listener
        btnSaveSettings.setOnClickListener(v -> savePreference());
    }

//        // Test buttons listeners
//        btnTestImmediate.setOnClickListener(v -> {
//            // Check for notification permission
//            checkNotificationPermission(true);
//        });
//
//        btnTestDelayed.setOnClickListener(v -> {
//            // Check for notification permission
//            checkNotificationPermission(false);
//        });


//
//    private void testImmediateNotification() {
//        Log.d(TAG, "Testing immediate notification");
//        String userName = loginManager.getUserName(this);
//        if (userName.isEmpty() && firebaseHelper.getCurrentUser() != null) {
//            // Try to get from Firebase
//            firebaseHelper.getUserData(firebaseHelper.getCurrentUser().getUid(),
//                    new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (snapshot.exists()) {
//                                String name = snapshot.child("name").getValue(String.class);
//                                NotificationTester.testNotificationImmediate(
//                                        NotificationSettingsActivity.this,
//                                        name != null ? name : ""
//                                );
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            NotificationTester.testNotificationImmediate(
//                                    NotificationSettingsActivity.this,
//                                    ""
//                            );
//                        }
//                    });
//        } else {
//            NotificationTester.testNotificationImmediate(this, userName);
//        }
//    }
//
//    private void testDelayedNotification() {
//        Log.d(TAG, "Testing delayed notification");
//        String userName = loginManager.getUserName(this);
//        if (userName.isEmpty() && firebaseHelper.getCurrentUser() != null) {
//            // Try to get from Firebase
//            firebaseHelper.getUserData(firebaseHelper.getCurrentUser().getUid(),
//                    new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (snapshot.exists()) {
//                                String name = snapshot.child("name").getValue(String.class);
//                                NotificationTester.testNotificationWithDelay(
//                                        NotificationSettingsActivity.this,
//                                        name != null ? name : ""
//                                );
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            NotificationTester.testNotificationWithDelay(
//                                    NotificationSettingsActivity.this,
//                                    ""
//                            );
//                        }
//                    });
//        } else {
//            NotificationTester.testNotificationWithDelay(this, userName);
//        }
//    }

    private void savePreference() {
        // If preference hasn't changed, just return
        if (selectedPreference.equals(currentPreference)) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Save to Firebase
        if (firebaseHelper.getCurrentUser() != null) {
            String userId = firebaseHelper.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("notificationPreference", selectedPreference);

            btnSaveSettings.setEnabled(false);

            firebaseHelper.updateUserData(userId, updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(NotificationSettingsActivity.this,
                                "Notification preferences updated",
                                Toast.LENGTH_SHORT).show();

                        // Update the notifications
                        updateNotifications(selectedPreference);

                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveSettings.setEnabled(true);
                        Toast.makeText(NotificationSettingsActivity.this,
                                "Failed to save preference: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateNotifications(String preference) {
        if (preference.equals("none")) {
            // Cancel all notifications
            NotificationScheduler.cancelAllNotifications(this);
        } else {
            // Get user name and schedule
            String userName = loginManager.getUserName(this);

            if (userName.isEmpty() && firebaseHelper.getCurrentUser() != null) {
                // Try to get from Firebase if not in LoginManager
                firebaseHelper.getUserData(firebaseHelper.getCurrentUser().getUid(),
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String name = snapshot.child("name").getValue(String.class);
                                    if (name != null) {
                                        NotificationScheduler.scheduleNotifications(
                                                NotificationSettingsActivity.this,
                                                preference,
                                                name
                                        );
                                    } else {
                                        NotificationScheduler.scheduleNotifications(
                                                NotificationSettingsActivity.this,
                                                preference,
                                                ""
                                        );
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                NotificationScheduler.scheduleNotifications(
                                        NotificationSettingsActivity.this,
                                        preference,
                                        ""
                                );
                            }
                        });
            } else {
                // Schedule with the name we have
                NotificationScheduler.scheduleNotifications(this, preference, userName);
            }
        }
    }
}