package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;

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
        // Initialize all views
        // TODO we need to add all the view bindings

        // For now, we'll just set up a placeholder behavior to continue to the next screen
        ivNextArrow = findViewById(R.id.iv_next_arrow);
        //TODO ivBackArrow = findViewById(R.id.iv_back_arrow);

        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);
    }

    private void setupListeners() {
        // Set up next arrow listener
        ivNextArrow.setOnClickListener(v -> {
            // TODO
           // navigateToTutorial();
        });

        // Set up back arrow listener
        ivBackArrow.setOnClickListener(v -> {
            // Navigate back to the goals screen
            finish();
        });
    }
//TODO
//    private void navigateToTutorial() {
//        Intent intent = new Intent(NotificationsActivity.this, TutorialActivity.class);
//        startActivity(intent);
//    }

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
}