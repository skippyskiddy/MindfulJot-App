package edu.northeastern.numad25sp_group4;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

import utils.FirebaseHelper;

/**
 * Second onboarding screen - asks for notification preferences
 */
public class NotificationsActivity extends AppCompatActivity {

    private MaterialCardView cardOnceDaily, cardTwiceDaily, cardThreeTimes, cardNoReminders;
    private ImageView ivOnceCheck, ivTwiceCheck, ivThreeCheck, ivNoCheck;
    private ImageView ivNextArrow, ivBackArrow;
    private View progressDot1, progressDot2, progressDot3;

    private FirebaseHelper firebaseHelper;
    private String selectedPreference = "";

    // Permission request launcher for notifications
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, continue with navigation
                    navigateToTutorial();
                } else {
                    // Permission denied, inform the user but still continue
                    Toast.makeText(this, "Notifications will be disabled", Toast.LENGTH_SHORT).show();

                    // Force set to none since permission was denied
                    selectedPreference = "none";
                    saveNotificationPreference();

                    navigateToTutorial();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_notifications);

        firebaseHelper = FirebaseHelper.getInstance();

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        // Cards
        cardOnceDaily = findViewById(R.id.card_once_daily);
        cardTwiceDaily = findViewById(R.id.card_twice_daily);
        cardThreeTimes = findViewById(R.id.card_three_times);
        cardNoReminders = findViewById(R.id.card_no_reminders);

        // Check marks
        ivOnceCheck = findViewById(R.id.iv_once_check);
        ivTwiceCheck = findViewById(R.id.iv_twice_check);
        ivThreeCheck = findViewById(R.id.iv_three_check);
        ivNoCheck = findViewById(R.id.iv_no_check);

        // Navigation
        ivNextArrow = findViewById(R.id.iv_next_arrow);
        ivBackArrow = findViewById(R.id.iv_back_arrow);

        // Progress dots
        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);

        // Initially hide check icons and set next arrow to partially transparent
        ivOnceCheck.setVisibility(View.INVISIBLE);
        ivTwiceCheck.setVisibility(View.INVISIBLE);
        ivThreeCheck.setVisibility(View.INVISIBLE);
        ivNoCheck.setVisibility(View.INVISIBLE);

        ivNextArrow.setAlpha(0.5f);
        ivNextArrow.setEnabled(false);
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

        // Set up navigation listeners
        ivNextArrow.setOnClickListener(v -> {
            if (!selectedPreference.isEmpty()) {
                // Save notification preference
                saveNotificationPreference();

                // Check if we need to request notification permissions
                if (!selectedPreference.equals("none")) {
                    requestNotificationPermission();
                } else {
                    // No need for permissions, navigate directly
                    navigateToTutorial();

                }
            }
        });

        ivBackArrow.setOnClickListener(v -> {
            // Navigate back to the goals screen
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

            firebaseHelper.updateUserData(userId, updates)
                    .addOnSuccessListener(aVoid -> {
                        // Preference saved successfully
                    })
                    .addOnFailureListener(e -> {
                        // Failed to save preference
                        Toast.makeText(NotificationsActivity.this,
                                "Failed to save preference: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void requestNotificationPermission() {
        // For Android 13 (API level 33) and above, use the POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    // Show explanation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission")
                            .setMessage("We need permission to send you check-in reminders based on your selected frequency.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                            })
                            .setNegativeButton("No Thanks", (dialog, which) -> {
                                // User declined, continue without notifications
                                navigateToTutorial();
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed, request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                // Permission already granted
                navigateToTutorial();
            }
        } else {
            // For Android 12 and below, notifications are enabled by default
            navigateToTutorial();
        }
    }


    private void navigateToTutorial() {
        // Log for debugging
        android.util.Log.d("NavigationFlow", "Navigating from Notifications to Tutorial");

        // Use NEW_TASK and CLEAR_TASK to clear the entire activity stack
        Intent intent = new Intent(NotificationsActivity.this, TutorialActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}