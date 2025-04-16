package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import models.Emotion;
import models.EmotionEntry;

/**
 * Activity for selecting primary emotion category
 * This is the first screen in the emotion logging flow
 */
public class PrimaryEmotionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private CardView cardHighEnergyPleasant;
    private CardView cardHighEnergyUnpleasant;
    private CardView cardLowEnergyPleasant;
    private CardView cardLowEnergyUnpleasant;

    private EmotionEntry currentEntry;
    private boolean isAddingSecondEmotion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primary_emotion);

        // Get entry from intent (if we're adding a second emotion)
        if (getIntent().hasExtra("CURRENT_ENTRY")) {
            try {
                currentEntry = (EmotionEntry) getIntent().getSerializableExtra("CURRENT_ENTRY");
                isAddingSecondEmotion = getIntent().getBooleanExtra("ADDING_SECOND_EMOTION", false);
            } catch (Exception e) {
                e.printStackTrace();
                // If there's an error, we'll create a new entry below
            }
        }

        // Create new entry if needed
        if (currentEntry == null) {
            currentEntry = new EmotionEntry();
            isAddingSecondEmotion = false;
        }

        // Initialize views
        initViews();

        // Set up click listeners
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        cardHighEnergyPleasant = findViewById(R.id.card_high_energy_pleasant);
        cardHighEnergyUnpleasant = findViewById(R.id.card_high_energy_unpleasant);
        cardLowEnergyPleasant = findViewById(R.id.card_low_energy_pleasant);
        cardLowEnergyUnpleasant = findViewById(R.id.card_low_energy_unpleasant);

        // Use a different animation for initial display
        Animation initialBounceAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_bounce_initial);

        // Clear any existing animations first
        cardHighEnergyUnpleasant.clearAnimation();
        cardHighEnergyPleasant.clearAnimation();
        cardLowEnergyUnpleasant.clearAnimation();
        cardLowEnergyPleasant.clearAnimation();

        // Apply animations with slight delays for a staggered effect
        cardHighEnergyUnpleasant.startAnimation(initialBounceAnimation);

        cardHighEnergyPleasant.postDelayed(() -> {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_bounce_initial);
            cardHighEnergyPleasant.startAnimation(animation);
        }, 100);

        cardLowEnergyUnpleasant.postDelayed(() -> {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_bounce_initial);
            cardLowEnergyUnpleasant.startAnimation(animation);
        }, 200);

        cardLowEnergyPleasant.postDelayed(() -> {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_bounce_initial);
            cardLowEnergyPleasant.startAnimation(animation);
        }, 300);
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            // If adding second emotion, return to journal summary
            if (isAddingSecondEmotion) {
                returnToJournalSummary();
            } else {
                // Otherwise, finish the activity to go back to HomeActivity
                finish();
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
            }
        });

        // High Energy Pleasant quadrant click listener
        cardHighEnergyPleasant.setOnClickListener(v -> {
            Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.anim_bounce);
            bounceAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    navigateToSpecificEmotions(Emotion.Category.HIGH_ENERGY_PLEASANT);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            v.startAnimation(bounceAnim);
        });

        // High Energy Unpleasant quadrant click listener
        cardHighEnergyUnpleasant.setOnClickListener(v -> {
            Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.anim_bounce);
            bounceAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    navigateToSpecificEmotions(Emotion.Category.HIGH_ENERGY_UNPLEASANT);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            v.startAnimation(bounceAnim);
        });

        // Low Energy Pleasant quadrant click listener
        cardLowEnergyPleasant.setOnClickListener(v -> {
            Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.anim_bounce);
            bounceAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    navigateToSpecificEmotions(Emotion.Category.LOW_ENERGY_PLEASANT);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            v.startAnimation(bounceAnim);
        });

        // Low Energy Unpleasant quadrant click listener
        cardLowEnergyUnpleasant.setOnClickListener(v -> {
            Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.anim_bounce);
            bounceAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    navigateToSpecificEmotions(Emotion.Category.LOW_ENERGY_UNPLEASANT);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            v.startAnimation(bounceAnim);
        });
    }

    /**
     * Navigate to specific emotions screen with the selected category
     */
    private void navigateToSpecificEmotions(Emotion.Category category) {
        // Navigate to SpecificEmotionActivity with selected category
        Intent intent = new Intent(PrimaryEmotionActivity.this, SpecificEmotionActivity.class);
        intent.putExtra("CATEGORY", category.name());

        // Pass the current entry and adding second emotion flag if relevant
        if (currentEntry != null) {
            intent.putExtra("CURRENT_ENTRY", currentEntry);
            intent.putExtra("ADDING_SECOND_EMOTION", isAddingSecondEmotion);
        }

        // Don't add any flags that would clear the back stack
        startActivity(intent);
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        // Don't call finish() here, so we remain in the back stack
    }

    /**
     * Return to JournalSummaryActivity when user cancels adding a second emotion
     */
    private void returnToJournalSummary() {
        Intent intent = new Intent(PrimaryEmotionActivity.this, JournalSummaryActivity.class);
        intent.putExtra("CURRENT_ENTRY", currentEntry);
        startActivity(intent);
        finish(); // Remove this activity from the stack
    }

    @Override
    public void onBackPressed() {
        // If adding second emotion, return to journal summary
        if (isAddingSecondEmotion) {
            returnToJournalSummary();
        } else {
            // Otherwise, do the default back behavior
            super.onBackPressed();
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        }
    }
}