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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primary_emotion);

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

        // Apply animations to cards when activity starts
        Animation bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_bounce);

        // Apply animations with slight delays for a staggered effect
        cardHighEnergyUnpleasant.startAnimation(bounceAnimation);

        cardHighEnergyPleasant.postDelayed(() -> {
            cardHighEnergyPleasant.startAnimation(bounceAnimation);
        }, 100);

        cardLowEnergyUnpleasant.postDelayed(() -> {
            cardLowEnergyUnpleasant.startAnimation(bounceAnimation);
        }, 200);

        cardLowEnergyPleasant.postDelayed(() -> {
            cardLowEnergyPleasant.startAnimation(bounceAnimation);
        }, 300);
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            finish(); // Return to previous screen
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        });

        // Animation for card touch
        Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.anim_bounce);

        // High Energy Pleasant quadrant click listener
        cardHighEnergyPleasant.setOnClickListener(v -> {
            v.startAnimation(bounceAnim);
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
        });

        // High Energy Unpleasant quadrant click listener
        cardHighEnergyUnpleasant.setOnClickListener(v -> {
            v.startAnimation(bounceAnim);
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
        });

        // Low Energy Pleasant quadrant click listener
        cardLowEnergyPleasant.setOnClickListener(v -> {
            v.startAnimation(bounceAnim);
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
        });

        // Low Energy Unpleasant quadrant click listener
        cardLowEnergyUnpleasant.setOnClickListener(v -> {
            v.startAnimation(bounceAnim);
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
        });
    }

    /**
     * Navigate to specific emotions screen with the selected category
     */
    private void navigateToSpecificEmotions(Emotion.Category category) {
        // TODO: Create SpecificEmotionActivity and navigate to it
        // For now, we'll just return to HomeActivity
        // Intent intent = new Intent(PrimaryEmotionActivity.this, SpecificEmotionActivity.class);
        // intent.putExtra("CATEGORY", category.name());
        // startActivity(intent);
        // overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);

        // Return to home for now with transition animation
        finish();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
    }

}