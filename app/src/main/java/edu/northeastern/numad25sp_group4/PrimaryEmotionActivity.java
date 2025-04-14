package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            finish(); // Return to previous screen
        });

        // High Energy Pleasant quadrant click listener
        cardHighEnergyPleasant.setOnClickListener(v -> {
            navigateToSpecificEmotions(Emotion.Category.HIGH_ENERGY_PLEASANT);
        });

        // High Energy Unpleasant quadrant click listener
        cardHighEnergyUnpleasant.setOnClickListener(v -> {
            navigateToSpecificEmotions(Emotion.Category.HIGH_ENERGY_UNPLEASANT);
        });

        // Low Energy Pleasant quadrant click listener
        cardLowEnergyPleasant.setOnClickListener(v -> {
            navigateToSpecificEmotions(Emotion.Category.LOW_ENERGY_PLEASANT);
        });

        // Low Energy Unpleasant quadrant click listener
        cardLowEnergyUnpleasant.setOnClickListener(v -> {
            navigateToSpecificEmotions(Emotion.Category.LOW_ENERGY_UNPLEASANT);
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

        // Return to home for now
        finish();
    }
}