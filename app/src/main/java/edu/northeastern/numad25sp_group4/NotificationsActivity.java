package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

import utils.FirebaseHelper;
import utils.NotificationHelper;
import utils.NotificationPermissionHandler;
import utils.NotificationScheduler;

/**
 * Second onboarding screen - asks about notification preferences
 */
public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private MaterialCardView cardOnceDaily;
    private MaterialCardView cardTwiceDaily;
    private MaterialCardView cardThreeTimes;
    private MaterialCardView cardNoReminders;
    private ImageView ivOnceCheck;
    private ImageView ivTwiceCheck;
    private ImageView ivThreeCheck;
    private ImageView ivNoCheck;
    private ImageView ivNextArrow;
    private ImageView ivBackArrow;
    private View progressDot1, progressDot2, progressDot3;
    private TextView tvDescription;

    private FirebaseHelper firebaseHelper;
    private String selectedPreference = "none";  // Default is no notifications

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_notifications);

        firebaseHelper = FirebaseHelper.getInstance();

        // Set up notification channel
        NotificationHelper.createNotificationChannel(this);

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted");
                        // Check exact alarm permission if needed
                        NotificationPermissionHandler.checkExactAlarmPermission(this);
                    } else {
                        Log.d(TAG, "Notification permission denied");
                        Toast.makeText(this,
                                "Notifications disabled. You can enable them later in settings.",
                                Toast.LENGTH_LONG).show();
                    }
                });

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();
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

        ivNextArrow = findViewById(R.id.iv_next_arrow);
        ivBackArrow = findViewById(R.id.iv_back_arrow);

        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);

        tvDescription = findViewById(R.id.tv_notification_description);

        // Initially hide check icons and set next arrow to partially transparent
        ivOnceCheck.setVisibility(View.INVISIBLE);
        ivTwiceCheck.setVisibility(View.INVISIBLE);
        ivThreeCheck.setVisibility(View.INVISIBLE);
        ivNoCheck.setVisibility(View.INVISIBLE);
        ivNextArrow.setAlpha(0.5f);
        ivNextArrow.setEnabled(false);

        // Set "None" as the default selected option
        selectedPreference = "none";
        updateCardSelection(cardNoReminders, ivNoCheck);
    }

    private void setupListeners() {
        // Set up card click listeners
        cardOnceDaily.setOnClickListener(v -> {
            selectPreference("once");
            updateCardSelection(cardOnceDaily, ivOnceCheck);
            resetOtherCards(cardOnceDaily);
        });

        cardTwiceDaily.setOnClickListener(v -> {
            selectPreference("twice");
            updateCardSelection(cardTwiceDaily, ivTwiceCheck);
            resetOtherCards(cardTwiceDaily);
        });

        cardThreeTimes.setOnClickListener(v -> {
            selectPreference("thrice");
            updateCardSelection(cardThreeTimes, ivThreeCheck);
            resetOtherCards(cardThreeTimes);
        });

        cardNoReminders.setOnClickListener(v -> {
            selectPreference("none");
            updateCardSelection(cardNoReminders, ivNoCheck);
            resetOtherCards(cardNoReminders);
        });

        // Next arrow click listener
        ivNextArrow.setOnClickListener(v -> {
            // Save notification preference
            saveNotificationPreference();

            // Request notification permission if needed (and if not "none")
            if (!selectedPreference.equals("none")) {
                requestNotificationPermission();
            }

            // Navigate to next onboarding screen (Tutorial)
            Intent intent = new Intent(NotificationsActivity.this, TutorialActivity.class);
            startActivity(intent);
        });

        ivBackArrow.setOnClickListener(v -> {
            // Navigate back to goals screen
            Intent intent = new Intent(NotificationsActivity.this, GoalsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void selectPreference(String preference) {
        selectedPreference = preference;

        // Activate next button
        ivNextArrow.setAlpha(1.0f);
        ivNextArrow.setEnabled(true);
    }

    private void updateCardSelection(MaterialCardView selectedCard, ImageView checkIcon) {
        // Add thicker border to selected card
        selectedCard.setStrokeWidth(4);
        selectedCard.setStrokeColor(ContextCompat.getColor(this, R.color.white));

        // Show check icon for selected card
        checkIcon.setVisibility(View.VISIBLE);
    }

    private void resetOtherCards(MaterialCardView selectedCard) {
        // Reset all cards except the selected one
        if (selectedCard != cardOnceDaily) {
            cardOnceDaily.setStrokeWidth(0);
            ivOnceCheck.setVisibility(View.INVISIBLE);
        }

        if (selectedCard != cardTwiceDaily) {
            cardTwiceDaily.setStrokeWidth(0);
            ivTwiceCheck.setVisibility(View.INVISIBLE);
        }

        if (selectedCard != cardThreeTimes) {
            cardThreeTimes.setStrokeWidth(0);
            ivThreeCheck.setVisibility(View.INVISIBLE);
        }

        if (selectedCard != cardNoReminders) {
            cardNoReminders.setStrokeWidth(0);
            ivNoCheck.setVisibility(View.INVISIBLE);
        }
    }

    private void saveNotificationPreference() {
        if (firebaseHelper.getCurrentUser() != null) {
            String userId = firebaseHelper.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("notificationPreference", selectedPreference);

            Log.d(TAG, "Saving notification preference: " + selectedPreference);

            firebaseHelper.updateUserData(userId, updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification preference saved successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save notification preference: " + e.getMessage());
                        Toast.makeText(NotificationsActivity.this,
                                "Failed to save notification preference: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void requestNotificationPermission() {
        // Request permission and schedule notifications if needed
        NotificationPermissionHandler.checkNotificationPermission(this);

        // Also check for exact alarm permission on Android 12+
        NotificationPermissionHandler.checkExactAlarmPermission(this);

        // Get user name
        String userName = "";
        if (firebaseHelper.getCurrentUser() != null) {
            // Try to use email until we get the actual name later
            userName = firebaseHelper.getCurrentUser().getEmail();
            if (userName != null && userName.contains("@")) {
                userName = userName.substring(0, userName.indexOf('@'));
            }
        }

        // Schedule notifications based on preference
        NotificationScheduler.scheduleNotifications(this, selectedPreference, userName);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the notification description based on current permissions
        updateNotificationDescription();
    }

    private void updateNotificationDescription() {
        boolean hasPermission = NotificationHelper.hasNotificationPermission(this);
        boolean canScheduleExact = NotificationScheduler.canScheduleExactAlarms(this);

        if (!hasPermission || !canScheduleExact) {
            tvDescription.setText("Note: For notifications to work properly, you'll need to grant permission when prompted. " +
                    "You can change this later in the app settings.");
        } else {
            tvDescription.setText("Enabling notifications will allow us to send you reminders based on your preferred frequency.");
        }
    }
}