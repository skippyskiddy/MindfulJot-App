package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import adapters.EmotionAdapter;
import models.Emotion;
import models.EmotionEntry;
import utils.EmotionInitializer;

/**
 * Activity for selecting a specific emotion from the selected category
 * This is the second screen in the emotion logging flow
 */
public class SpecificEmotionActivity extends AppCompatActivity implements EmotionAdapter.OnEmotionSelectedListener {

    private static final String TAG = "SpecificEmotionActivity";
    private static final boolean FORCE_EMOTION_RESET = false; // Set to true to force a database reset

    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvCategoryTitle;
    private RecyclerView recyclerEmotions;
    private ImageView ivBackArrow;
    private ImageView ivNextArrow;
    private ProgressBar progressBar;

    private EmotionAdapter adapter;
    private Emotion.Category selectedCategory;
    private EmotionEntry currentEntry;
    private boolean isAddingSecondEmotion = false;
    private Emotion selectedEmotion;
    private List<Emotion> emotionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_emotion);

        Log.d(TAG, "onCreate called");

        // Get selected category from intent
        if (getIntent().hasExtra("CATEGORY")) {
            String categoryStr = getIntent().getStringExtra("CATEGORY");
            selectedCategory = Emotion.Category.valueOf(categoryStr);
            Log.d(TAG, "Selected category: " + selectedCategory.name());
        } else {
            // Default to HIGH_ENERGY_PLEASANT if somehow no category was passed
            selectedCategory = Emotion.Category.HIGH_ENERGY_PLEASANT;
            Log.d(TAG, "No category passed, defaulting to HIGH_ENERGY_PLEASANT");
        }

        // Initialize views
        initViews();

        // Create new entry or get existing entry from intent
        initEmotionEntry();

        // Set up RecyclerView and adapter
        setupRecyclerView();

        // Set up listeners
        setupListeners();

        // Show progress bar
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Use the EmotionInitializer to load emotions
        loadEmotions();
    }

    private void loadEmotions() {
        EmotionInitializer initializer = new EmotionInitializer(this);

        if (FORCE_EMOTION_RESET) {
            // Force a complete reset of the emotions database
            initializer.forceResetEmotions(new EmotionInitializer.EmotionInitCallback() {
                @Override
                public void onInitComplete(boolean success, List<Emotion> allEmotions) {
                    handleEmotionsLoaded(success, allEmotions);
                }
            });
        } else {
            // Normal initialization - will add any missing emotions
            initializer.verifyAndInitEmotions(new EmotionInitializer.EmotionInitCallback() {
                @Override
                public void onInitComplete(boolean success, List<Emotion> allEmotions) {
                    handleEmotionsLoaded(success, allEmotions);
                }
            });
        }
    }

    private void handleEmotionsLoaded(boolean success, List<Emotion> allEmotions) {
        if (success) {
            // Filter for the current category
            List<Emotion> filteredEmotions = new ArrayList<>();
            for (Emotion emotion : allEmotions) {
                if (emotion.getCategory() == selectedCategory) {
                    filteredEmotions.add(emotion);
                }
            }

            Log.d(TAG, "Filtered " + filteredEmotions.size() + " emotions for category " + selectedCategory.name());

            // Sort emotions by energy level (highest to lowest)
            Collections.sort(filteredEmotions, new Comparator<Emotion>() {
                @Override
                public int compare(Emotion e1, Emotion e2) {
                    return Integer.compare(e2.getEnergyLevel(), e1.getEnergyLevel());
                }
            });

            // Update the adapter with emotions
            runOnUiThread(() -> {
                emotionsList.clear();
                emotionsList.addAll(filteredEmotions);
                adapter.setEmotions(filteredEmotions);
                adapter.notifyDataSetChanged();

                // Hide progress bar
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                Log.d(TAG, "Updated adapter with " + filteredEmotions.size() +
                        " emotions for category " + selectedCategory.name());
            });
        } else {
            // If initialization failed, use default emotions
            runOnUiThread(() -> {
                Toast.makeText(SpecificEmotionActivity.this,
                        "Error loading emotions, using defaults", Toast.LENGTH_SHORT).show();
                addDefaultEmotions();

                // Hide progress bar
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvCategoryTitle = findViewById(R.id.tv_category_title);
        recyclerEmotions = findViewById(R.id.recycler_emotions);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        ivNextArrow = findViewById(R.id.iv_next_arrow);
        progressBar = findViewById(R.id.progress_bar);

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

        // Initially disable next button
        ivNextArrow.setAlpha(0.5f);
        ivNextArrow.setEnabled(false);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");

        // Create layout manager with explicit settings
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        // Configure RecyclerView with more explicit settings
        recyclerEmotions.setLayoutManager(layoutManager);
        recyclerEmotions.setHasFixedSize(false);
        recyclerEmotions.setItemAnimator(new DefaultItemAnimator());

        // Add divider decoration for visual separation
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerEmotions.getContext(), layoutManager.getOrientation());
        recyclerEmotions.addItemDecoration(dividerItemDecoration);

        // Enable scrolling features
        recyclerEmotions.setNestedScrollingEnabled(true);
        recyclerEmotions.setVerticalScrollBarEnabled(true);

        // Create and set adapter
        adapter = new EmotionAdapter(this, selectedCategory, this);
        recyclerEmotions.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup complete");
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
        Log.d(TAG, "Initializing emotion entry");

        // Check if we're getting an existing entry from the intent
        if (getIntent().hasExtra("CURRENT_ENTRY")) {
            try {
                currentEntry = (EmotionEntry) getIntent().getSerializableExtra("CURRENT_ENTRY");
                isAddingSecondEmotion = getIntent().getBooleanExtra("ADDING_SECOND_EMOTION", false);
                Log.d(TAG, "Retrieved current entry from intent. Adding second emotion: " + isAddingSecondEmotion);
            } catch (Exception e) {
                Log.e(TAG, "Error getting current entry from intent", e);
                // If there's an error, we'll create a new entry below
            }
        }

        // Check if we're editing an existing entry
        if (currentEntry == null && getIntent().hasExtra("ENTRY_ID")) {
            String entryId = getIntent().getStringExtra("ENTRY_ID");
            Log.d(TAG, "Creating new entry with ID: " + entryId);
            // TODO: Load the entry from Firebase
            // For now, create a new entry with the ID
            currentEntry = new EmotionEntry();
            currentEntry.setEntryId(entryId);
        }

        // If still null, create a new entry
        if (currentEntry == null) {
            Log.d(TAG, "Creating brand new entry");
            currentEntry = new EmotionEntry();
            // Set current user ID and timestamp
            isAddingSecondEmotion = false;
            currentEntry.setTimestamp(new java.util.Date());
        }
    }

    private void addDefaultEmotions() {
        Log.d(TAG, "Adding default emotions as fallback");
        Toast.makeText(this, "Using default emotions", Toast.LENGTH_SHORT).show();

        List<Emotion> defaultEmotions = new ArrayList<>();

        // Add default emotions based on selected category
        switch (selectedCategory) {
            case HIGH_ENERGY_PLEASANT:
                defaultEmotions.add(new Emotion("Excited", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling very enthusiastic and eager", 10));
                defaultEmotions.add(new Emotion("Joyful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling happiness and delight", 9));
                defaultEmotions.add(new Emotion("Proud", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling deep satisfaction with achievements", 8));
                defaultEmotions.add(new Emotion("Optimistic", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling hopeful about the future", 7));
                defaultEmotions.add(new Emotion("Cheerful", Emotion.Category.HIGH_ENERGY_PLEASANT, "Feeling noticeably happy and positive", 6));
                break;

            case HIGH_ENERGY_UNPLEASANT:
                defaultEmotions.add(new Emotion("Angry", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling strong displeasure or hostility", 10));
                defaultEmotions.add(new Emotion("Anxious", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling worried or nervous", 9));
                defaultEmotions.add(new Emotion("Frustrated", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling upset and annoyed at unresolved problems", 8));
                defaultEmotions.add(new Emotion("Stressed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling mental or emotional strain", 7));
                defaultEmotions.add(new Emotion("Overwhelmed", Emotion.Category.HIGH_ENERGY_UNPLEASANT, "Feeling buried under too many tasks or emotions", 6));
                break;

            case LOW_ENERGY_PLEASANT:
                defaultEmotions.add(new Emotion("Calm", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling tranquil and peaceful", 4));
                defaultEmotions.add(new Emotion("Content", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling satisfied with current state", 3));
                defaultEmotions.add(new Emotion("Relaxed", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling free from tension", 2));
                defaultEmotions.add(new Emotion("Grateful", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling thankful and appreciative", 4));
                defaultEmotions.add(new Emotion("Serene", Emotion.Category.LOW_ENERGY_PLEASANT, "Feeling clear and calm", 1));
                break;

            case LOW_ENERGY_UNPLEASANT:
                defaultEmotions.add(new Emotion("Sad", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling sorrow or unhappiness", 4));
                defaultEmotions.add(new Emotion("Tired", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling in need of rest or sleep", 3));
                defaultEmotions.add(new Emotion("Bored", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling weary from lack of interest", 2));
                defaultEmotions.add(new Emotion("Disappointed", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling let down or discouraged", 4));
                defaultEmotions.add(new Emotion("Lonely", Emotion.Category.LOW_ENERGY_UNPLEASANT, "Feeling isolated or without companionship", 3));
                break;
        }

        // Update adapter with default emotions
        emotionsList.clear();
        emotionsList.addAll(defaultEmotions);
        adapter.setEmotions(defaultEmotions);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Added " + defaultEmotions.size() + " default emotions");
    }

    private void setupListeners() {
        Log.d(TAG, "Setting up listeners");

        // Back button click listener
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish(); // Return to primary emotion selection
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        });

        // Back arrow click listener
        ivBackArrow.setOnClickListener(v -> {
            Log.d(TAG, "Back arrow clicked");
            finish(); // Return to primary emotion selection
            overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
        });

        // Next arrow click listener
        ivNextArrow.setOnClickListener(v -> {
            Log.d(TAG, "Next arrow clicked with selected emotion: " +
                    (selectedEmotion != null ? selectedEmotion.getName() : "none"));

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
        Log.d(TAG, "Emotion selected: " + emotion.getName());
        selectedEmotion = emotion;

        // Enable next button
        ivNextArrow.setAlpha(1.0f);
        ivNextArrow.setEnabled(true);

        // Apply quick pulse animation to next button
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.anim_pulse);
        ivNextArrow.startAnimation(pulseAnim);
    }

    private void addEmotionToEntry() {
        Log.d(TAG, "Adding emotion to entry: " + selectedEmotion.getName());

        if (currentEntry == null) {
            currentEntry = new EmotionEntry();
            // Set timestamp
            currentEntry.setTimestamp(new java.util.Date());
        }

        // Check if this is a second emotion (adding second emotion flow)
        if (isAddingSecondEmotion) {
            Log.d(TAG, "Adding as second emotion");

            // This is a second emotion, just add it to the existing list
            // First check if we already have 2 emotions (max limit)
            if (currentEntry.getEmotions() != null && currentEntry.getEmotions().size() >= 2) {
                // We already have 2 emotions, replace the second one
                if (currentEntry.getEmotions().size() > 1) {
                    currentEntry.getEmotions().set(1, selectedEmotion);
                    Log.d(TAG, "Replaced second emotion");
                } else {
                    // Or add as second emotion
                    currentEntry.addEmotion(selectedEmotion);
                    Log.d(TAG, "Added as second emotion");
                }
            } else {
                // Just add the emotion
                currentEntry.addEmotion(selectedEmotion);
                Log.d(TAG, "Added emotion to list");
            }
        } else {
            Log.d(TAG, "Adding as primary emotion");

            // This is the first emotion or we're replacing all emotions
            // Clear existing emotions first
            if (currentEntry.getEmotions() != null) {
                currentEntry.getEmotions().clear();
            }
            // Add the selected emotion
            currentEntry.addEmotion(selectedEmotion);
        }

        // Log the current state of emotions in the entry
        Log.d(TAG, "Current entry now has " +
                (currentEntry.getEmotions() != null ? currentEntry.getEmotions().size() : 0) +
                " emotions");
    }

    private void navigateToJournalSummary() {
        Log.d(TAG, "Navigating to journal summary");

        // Navigate to JournalSummaryActivity with the updated emotion entry
        Intent intent = new Intent(SpecificEmotionActivity.this, JournalSummaryActivity.class);
        intent.putExtra("CURRENT_ENTRY", currentEntry);
        startActivity(intent);

        // Use the slide animation
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);

        // If we're adding a second emotion, we should finish this and the PrimaryEmotionActivity
        // so the back button from JournalSummary doesn't go through the emotion selection again
        if (isAddingSecondEmotion) {
            Log.d(TAG, "Finishing activity after adding second emotion");
            finish();
        }
    }
}