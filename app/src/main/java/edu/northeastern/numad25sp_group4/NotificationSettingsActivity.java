package edu.northeastern.numad25sp_group4;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

public class NotificationSettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialCardView cardOnceDaily, cardTwiceDaily, cardThreeTimes, cardNoReminders;
    private ImageView ivOnceCheck, ivTwiceCheck, ivThreeCheck, ivNoCheck;
    private Button btnSaveSettings;

    private FirebaseHelper firebaseHelper;
    private String userId;
    private String selectedPreference = "";
    private String currentPreference = "";

    // Permission request launcher for notifications
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, continue with saving settings
                    saveNotificationPreference();
                } else {
                    // Permission denied, inform the user but still save preference
                    Toast.makeText(this, "Notifications will be disabled", Toast.LENGTH_SHORT).show();
                    // Force set to none since permission was denied
                    selectedPreference = "none";
                    saveNotificationPreference();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
        } else {
            // If not logged in, go back to settings
            Toast.makeText(this, "You must be logged in to change notification settings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Load current notification preference
        loadCurrentPreference();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

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

        // Save button
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        // Initially hide all check icons
        ivOnceCheck.setVisibility(View.INVISIBLE);
        ivTwiceCheck.setVisibility(View.INVISIBLE);
        ivThreeCheck.setVisibility(View.INVISIBLE);
        ivNoCheck.setVisibility(View.INVISIBLE);
    }

    private void loadCurrentPreference() {
        // Show loading/disable state
        setCardsEnabled(false);

        if (userId != null) {
            firebaseHelper.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentPreference = snapshot.child("notificationPreference").getValue(String.class);
                        if (currentPreference == null) {
                            currentPreference = "none"; // Default to none
                        }

                        // Set initial selection based on current preference
                        selectPreference(currentPreference);
                        updateCardSelection();

                        // Enable cards after loading
                        setCardsEnabled(true);
                    } else {
                        // If user data doesn't exist, default to none
                        currentPreference = "none";
                        selectPreference(currentPreference);
                        updateCardSelection();
                        setCardsEnabled(true);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(NotificationSettingsActivity.this,
                            "Failed to load notification preferences: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    setCardsEnabled(true);
                }
            });
        } else {
            // Default to none if no user ID
            currentPreference = "none";
            selectPreference(currentPreference);
            updateCardSelection();
            setCardsEnabled(true);
        }
    }

    private void setCardsEnabled(boolean enabled) {
        cardOnceDaily.setEnabled(enabled);
        cardTwiceDaily.setEnabled(enabled);
        cardThreeTimes.setEnabled(enabled);
        cardNoReminders.setEnabled(enabled);
        btnSaveSettings.setEnabled(enabled);
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Card click listeners
        cardOnceDaily.setOnClickListener(v -> {
            selectPreference("once");
            updateCardSelection();
        });

        cardTwiceDaily.setOnClickListener(v -> {
            selectPreference("twice");
            updateCardSelection();
        });

        cardThreeTimes.setOnClickListener(v -> {
            selectPreference("thrice");
            updateCardSelection();
        });

        cardNoReminders.setOnClickListener(v -> {
            selectPreference("none");
            updateCardSelection();
        });

        // Save button click listener
        btnSaveSettings.setOnClickListener(v -> {
            if (!selectedPreference.isEmpty()) {
                // Request notification permission if not set to "none"
                if (!selectedPreference.equals("none")) {
                    requestNotificationPermission();
                } else {
                    // No permission needed, just save preference
                    saveNotificationPreference();
                }
            } else {
                Toast.makeText(NotificationSettingsActivity.this,
                        "Please select a notification preference",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectPreference(String preference) {
        selectedPreference = preference;
    }

    private void updateCardSelection() {
        // Reset all card selections
        resetCardSelection();

        // Update card selection based on the selected preference
        if (selectedPreference.equals("once")) {
            setCardSelected(cardOnceDaily, ivOnceCheck);
        } else if (selectedPreference.equals("twice")) {
            setCardSelected(cardTwiceDaily, ivTwiceCheck);
        } else if (selectedPreference.equals("thrice")) {
            setCardSelected(cardThreeTimes, ivThreeCheck);
        } else if (selectedPreference.equals("none")) {
            setCardSelected(cardNoReminders, ivNoCheck);
        }
    }

    private void resetCardSelection() {
        resetCardState(cardOnceDaily, ivOnceCheck);
        resetCardState(cardTwiceDaily, ivTwiceCheck);
        resetCardState(cardThreeTimes, ivThreeCheck);
        resetCardState(cardNoReminders, ivNoCheck);
    }

    private void resetCardState(MaterialCardView card, ImageView checkIcon) {
        card.setStrokeWidth(0);
        checkIcon.setVisibility(View.INVISIBLE);
    }

    private void setCardSelected(MaterialCardView card, ImageView checkIcon) {
        card.setStrokeWidth(4);
        card.setStrokeColor(ContextCompat.getColor(this, R.color.white));
        checkIcon.setVisibility(View.VISIBLE);
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
                                selectedPreference = "none";
                                saveNotificationPreference();
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed, request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                // Permission already granted
                saveNotificationPreference();
            }
        } else {
            // For Android 12 and below, notifications are enabled by default
            saveNotificationPreference();
        }
    }

    private void saveNotificationPreference() {
        if (userId != null) {
            // Disable save button to prevent double-clicks
            btnSaveSettings.setEnabled(false);

            // Create updates map
            Map<String, Object> updates = new HashMap<>();
            updates.put("notificationPreference", selectedPreference);

            firebaseHelper.updateUserData(userId, updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(NotificationSettingsActivity.this,
                                "Notification settings saved",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(NotificationSettingsActivity.this,
                                "Failed to save preference: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnSaveSettings.setEnabled(true);
                    });
        } else {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            btnSaveSettings.setEnabled(true);
        }
    }
}