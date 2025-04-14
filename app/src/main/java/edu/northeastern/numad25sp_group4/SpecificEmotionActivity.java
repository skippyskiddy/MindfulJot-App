package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import adapters.EmotionAdapter;
import models.Emotion;
import models.EmotionEntry;
import utils.FirebaseHelper;

/**
 * Activity for selecting a specific emotion from the selected category
 * This is the second screen in the emotion logging flow
 */
public class SpecificEmotionActivity extends AppCompatActivity implements EmotionAdapter.OnEmotionSelectedListener {

    private ImageButton btnBack;
    private TextView tvCategoryTitle;
    private RecyclerView recyclerEmotions;
    private ImageView ivBackArrow;
    private ImageView ivNextArrow;

    private EmotionAdapter adapter;
    private Emotion.Category selectedCategory;
    private EmotionEntry currentEntry;
    private FirebaseHelper firebaseHelper;
    private Emotion selectedEmotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_emotion);

        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();

        // Get selected category from intent
        if (getIntent().hasExtra("CATEGORY")) {
            String categoryStr = getIntent().getStringExtra("CATEGORY");
            selectedCategory = Emotion.Category.valueOf(categoryStr);
        } else {
            // Default to HIGH_ENERGY_PLEASANT if somehow no category was passed
            selectedCategory = Emotion.Category.HIGH_ENERGY_PLEASANT;
        }

        // Initialize views
        initViews();

        // Create new entry or get existing entry from intent
        initEmotionEntry();

        // Load emotions for the selected category
        loadEmotions();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvCategoryTitle = findViewById(R.id.tv_category_title);
        recyclerEmotions = findViewById(R.id.recycler_emotions);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        ivNextArrow = findViewById(R.id.iv_next_arrow);

        // Set category title text
        String categoryTitle = getCategoryDisplayName(selectedCategory);
        tvCategoryTitle.setText(categoryTitle);

        // Set category title color based on the category
        int textColor;
        switch (selectedCategory) {
            case HIGH_ENERGY_PLEASANT:
                textColor = 0xFF5F5000;
                break;
            case HIGH_ENERGY_UNPLEASANT:
                textColor = 0xFF8E2020;
                break;
            case LOW_ENERGY_PLEASANT:
                textColor = 0xFF0F5B0F;
                break;
            case LOW_ENERGY_UNPLEASANT:
                textColor = 0xFF004975;
                break;
            default:
                textColor = 0xFFFFFFFF;
        }
        tvCategoryTitle.setTextColor(textColor);

        // Set up RecyclerView
        recyclerEmotions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmotionAdapter(this, selectedCategory, this);
        recyclerEmotions.setAdapter(adapter);

        // Initially disable next button
        ivNextArrow.setAlpha(0.5f);
        ivNextArrow.setEnabled(false);
    }

    private String getCategoryDisplayName(Emotion.Category category) {
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                return "High Energy, Pleasant";
            case HIGH_ENERGY_UNPLEASANT:
                return "High Energy, Unpleasant";
            case LOW_ENERGY_PLEASANT:
                return "Low Energy, Pleasant";
            case LOW_ENERGY_UNPLEASANT:
                return "Low Energy, Unpleasant";
            default:
                return "Select Emotion";
        }
    }

    private void initEmotionEntry() {
        // Check if we're editing an existing entry
        if (getIntent().hasExtra("ENTRY_ID")) {
            String entryId = getIntent().getStringExtra("ENTRY_ID");
            // TODO: Load the entry from Firebase
            // For now, create a new entry
            currentEntry = new EmotionEntry();
            currentEntry.setEntryId(entryId);
        } else {
            // Create a new entry
            currentEntry = new EmotionEntry();
            // Set current user ID and timestamp
            if (firebaseHelper.getCurrentUser() != null) {
                currentEntry.setUserId(firebaseHelper.getCurrentUser().getUid());
            }
            currentEntry.setTimestamp(new java.util.Date());
        }
    }

    private void loadEmotions() {
        firebaseHelper.getEmotionsByCategory(selectedCategory, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Emotion> emotions = new ArrayList<>();

                for (DataSnapshot emotionSnapshot : snapshot.getChildren()) {
                    Emotion emotion = emotionSnapshot.getValue(Emotion.class);
                    if (emotion != null) {
                        emotions.add(emotion);
                    }
                }

                // Sort emotions by energy level (highest to lowest)
                Collections.sort(emotions, new Comparator<Emotion>() {
                    @Override
                    public int compare(Emotion e1, Emotion e2) {
                        return Integer.compare(e2.getEnergyLevel(), e1.getEnergyLevel());
                    }
                });

                // Update adapter
                adapter.setEmotions(emotions);

                // Check if we need to select an emotion (editing existing entry)
                if (currentEntry != null && currentEntry.getEmotions() != null && !currentEntry.getEmotions().isEmpty()) {
                    for (Emotion entryEmotion : currentEntry.getEmotions()) {
                        if (entryEmotion.getCategory() == selectedCategory) {
                            selectedEmotion = entryEmotion;
                            onEmotionSelected(selectedEmotion);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SpecificEmotionActivity.this,
                        "Failed to load emotions: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            finish(); // Return to primary emotion selection
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        });

        // Back arrow click listener
        ivBackArrow.setOnClickListener(v -> {
            finish(); // Return to primary emotion selection
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        });

        // Next arrow click listener
        ivNextArrow.setOnClickListener(v -> {
            if (selectedEmotion != null) {
                // Add selected emotion to entry
                addEmotionToEntry();

                // Navigate to journal summary screen
                navigateToJournalSummary();
            }
        });
    }

    @Override
    public void onEmotionSelected(Emotion emotion) {
        selectedEmotion = emotion;

        // Enable next button
        ivNextArrow.setAlpha(1.0f);
        ivNextArrow.setEnabled(true);

        // Apply quick pulse animation to next button
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.anim_pulse);
        ivNextArrow.startAnimation(pulseAnim);
    }

    private void addEmotionToEntry() {
        if (currentEntry == null) {
            currentEntry = new EmotionEntry();
            // Set current user ID and timestamp
            if (firebaseHelper.getCurrentUser() != null) {
                currentEntry.setUserId(firebaseHelper.getCurrentUser().getUid());
            }
            currentEntry.setTimestamp(new java.util.Date());
        }

        // Check if entry already has this emotion category
        boolean hasCategory = false;
        if (currentEntry.getEmotions() != null) {
            for (int i = 0; i < currentEntry.getEmotions().size(); i++) {
                Emotion existing = currentEntry.getEmotions().get(i);
                if (existing.getCategory() == selectedEmotion.getCategory()) {
                    // Replace existing emotion of this category
                    currentEntry.getEmotions().set(i, selectedEmotion);
                    hasCategory = true;
                    break;
                }
            }
        }

        // Add emotion if not replacing
        if (!hasCategory) {
            currentEntry.addEmotion(selectedEmotion);
        }
    }

    private void navigateToJournalSummary() {
        // TODO: Create JournalSummaryActivity and navigate to it
        // For now, we'll just return to HomeActivity
        Toast.makeText(this, "Selected: " + selectedEmotion.getName(), Toast.LENGTH_SHORT).show();

        // Temporary: return to home after selection
        // In the final app, this should navigate to JournalSummaryActivity
        Intent intent = new Intent(SpecificEmotionActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();

        // TODO: Replace with this when JournalSummaryActivity is implemented:
        // Intent intent = new Intent(SpecificEmotionActivity.this, JournalSummaryActivity.class);
        // intent.putExtra("ENTRY_ID", currentEntry.getEntryId());
        // startActivity(intent);
        // overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
    }
}