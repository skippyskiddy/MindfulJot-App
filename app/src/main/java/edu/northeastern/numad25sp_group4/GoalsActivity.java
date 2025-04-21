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
 * First onboarding screen - asks why user is using MindfulJot
 */
public class GoalsActivity extends AppCompatActivity {

    private MaterialCardView cardImproveAwareness;
    private MaterialCardView cardTrackPatterns;
    private MaterialCardView cardReduceStress;
    private ImageView ivAwarenessCheck;
    private ImageView ivPatternsCheck;
    private ImageView ivStressCheck;
    private ImageView ivNextArrow;
    private ImageView ivBackArrow;
    private View progressDot1, progressDot2, progressDot3;

    private FirebaseHelper firebaseHelper;
    private String selectedGoal = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_goals);

        firebaseHelper = FirebaseHelper.getInstance();

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        cardImproveAwareness = findViewById(R.id.card_improve_awareness);
        cardTrackPatterns = findViewById(R.id.card_track_patterns);
        cardReduceStress = findViewById(R.id.card_reduce_stress);

        ivAwarenessCheck = findViewById(R.id.iv_awareness_check);
        ivPatternsCheck = findViewById(R.id.iv_patterns_check);
        ivStressCheck = findViewById(R.id.iv_stress_check);

        ivNextArrow = findViewById(R.id.iv_next_arrow);
        ivBackArrow = findViewById(R.id.iv_back_arrow);


        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);

        // Initially hide check icons and set next arrow to partially transparent
        ivAwarenessCheck.setVisibility(View.INVISIBLE);
        ivPatternsCheck.setVisibility(View.INVISIBLE);
        ivStressCheck.setVisibility(View.INVISIBLE);
        ivNextArrow.setAlpha(0.5f);
        ivNextArrow.setEnabled(false);
    }

    private void setupListeners() {
        // Set up card click listeners
        cardImproveAwareness.setOnClickListener(v -> {
            selectGoal("EmotionalAwareness");
            updateCardSelection(cardImproveAwareness, ivAwarenessCheck);
            resetOtherCards(cardImproveAwareness);
        });

        cardTrackPatterns.setOnClickListener(v -> {
            selectGoal("MoodPattern");
            updateCardSelection(cardTrackPatterns, ivPatternsCheck);
            resetOtherCards(cardTrackPatterns);
        });

        cardReduceStress.setOnClickListener(v -> {
            selectGoal("StressReduction");
            updateCardSelection(cardReduceStress, ivStressCheck);
            resetOtherCards(cardReduceStress);
        });

        // Next arrow click listener
        ivNextArrow.setOnClickListener(v -> {
            if (!selectedGoal.isEmpty()) {
                // Save user's goal
                saveUserGoal();

                // Navigate to next onboarding screen (Notifications)
                Intent intent = new Intent(GoalsActivity.this, NotificationsActivity.class);
                startActivity(intent);
            }
        });

        ivBackArrow.setOnClickListener(v -> {
            // Navigate back to welcome screen
            Intent intent = new Intent(GoalsActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void selectGoal(String goal) {
        selectedGoal = goal;

        // Activate next button
        ivNextArrow.setAlpha(1.0f);
        ivNextArrow.setEnabled(true);
    }

    private void updateCardSelection(MaterialCardView selectedCard, ImageView checkIcon) {        // Add thicker border to selected card
        selectedCard.setStrokeWidth(4);
        selectedCard.setStrokeColor(ContextCompat.getColor(this, R.color.white));

        // Show check icon for selected card
        checkIcon.setVisibility(View.VISIBLE);
    }

    private void resetOtherCards(MaterialCardView selectedCard) {
        // Reset all cards except the selected one
        if (selectedCard != cardImproveAwareness) {
            cardImproveAwareness.setStrokeWidth(0);
            ivAwarenessCheck.setVisibility(View.INVISIBLE);
        }

        if (selectedCard != cardTrackPatterns) {
            cardTrackPatterns.setStrokeWidth(0);
            ivPatternsCheck.setVisibility(View.INVISIBLE);
        }

        if (selectedCard != cardReduceStress) {
            cardReduceStress.setStrokeWidth(0);
            ivStressCheck.setVisibility(View.INVISIBLE);
        }
    }

    private void saveUserGoal() {
        if (firebaseHelper.getCurrentUser() != null) {
            String userId = firebaseHelper.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("goal", selectedGoal);

            firebaseHelper.updateUserData(userId, updates)
                    .addOnSuccessListener(aVoid -> {
                        // Goal saved successfully
                    })
                    .addOnFailureListener(e -> {
                        // Failed to save goal
                        Toast.makeText(GoalsActivity.this, "Failed to save goal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}