package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapters.TutorialPagerAdapter;
import utils.FirebaseHelper;

/**
 * Third onboarding screen - shows a tutorial carousel
 */
public class TutorialActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View[] carouselIndicators;
    private ImageView ivNextArrow, ivBackArrow;
    private Button btnGetStarted;
    private View progressDot1, progressDot2, progressDot3;
    private FirebaseHelper firebaseHelper;
    private TutorialPagerAdapter adapter;

    private final int TOTAL_SLIDES = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        firebaseHelper = FirebaseHelper.getInstance();

        // Initialize views
        initViews();

        // Setup carousel
        setupCarousel();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewpager_tutorial);

        // Carousel indicators
        carouselIndicators = new View[TOTAL_SLIDES];
        carouselIndicators[0] = findViewById(R.id.indicator_1);
        carouselIndicators[1] = findViewById(R.id.indicator_2);
        carouselIndicators[2] = findViewById(R.id.indicator_3);
        carouselIndicators[3] = findViewById(R.id.indicator_4);

        // Navigation
        ivNextArrow = findViewById(R.id.iv_next_arrow);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        btnGetStarted = findViewById(R.id.btn_get_started);

        // Progress dots
        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);
    }

    private void setupCarousel() {
        // Create tutorial slides
        List<TutorialPagerAdapter.TutorialSlide> slides = new ArrayList<>();

       // TODO
        slides.add(new TutorialPagerAdapter.TutorialSlide(
                R.drawable.carousel_one_log_emotion,
                "1.",
                "Tap log emotion button on the home page"));

        slides.add(new TutorialPagerAdapter.TutorialSlide(
                R.drawable.carousel_one_log_emotion,
                //R.drawable.carousel_two_select_emotion,
                "2.",
                "Select how you're feeling currently"));

        slides.add(new TutorialPagerAdapter.TutorialSlide(
                R.drawable.carousel_one_log_emotion,
                //R.drawable.carousel_three_journal_entry,
                "3.",
                "Optionally, attach a journal entry, images, text, or tags to your MindfulJot entry"));

        slides.add(new TutorialPagerAdapter.TutorialSlide(
                R.drawable.carousel_one_log_emotion,
                //R.drawable.carousel_four_view_entries,
                "4.",
                "You're done! Look back on your old entries in the Entries tab, or your emotion trends in the Analytics tab"));

        // Set up adapter
        adapter = new adapters.TutorialPagerAdapter(this, slides);
        viewPager.setAdapter(adapter);

        // Set initial indicator
        updateCarouselIndicators(0);

        // Set up ViewPager callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCarouselIndicators(position);

                // Show Get Started button on last slide
                if (position == TOTAL_SLIDES - 1) {
                    ivNextArrow.setVisibility(View.INVISIBLE);
                    btnGetStarted.setVisibility(View.VISIBLE);
                } else {
                    ivNextArrow.setVisibility(View.VISIBLE);
                    btnGetStarted.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void updateCarouselIndicators(int position) {
        // Update carousel indicators
        for (int i = 0; i < TOTAL_SLIDES; i++) {
            carouselIndicators[i].setBackgroundResource(
                    i == position ? R.drawable.carousel_indicator_active : R.drawable.carousel_indicator_inactive);
        }
    }

    private void setupListeners() {
        // Next arrow - advance carousel
        ivNextArrow.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition < TOTAL_SLIDES - 1) {
                viewPager.setCurrentItem(currentPosition + 1);
            }
        });

        // Back arrow - go back in carousel or to previous screen
        ivBackArrow.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition > 0) {
                viewPager.setCurrentItem(currentPosition - 1);
            } else {
                // Go back to notifications screen
                finish();
            }
        });

        // Get Started button - complete onboarding
        btnGetStarted.setOnClickListener(v -> {
            // Mark tutorial as completed in Firebase
            markTutorialCompleted();

            // Navigate to the home screen
            navigateToHome();
        });
    }

    private void markTutorialCompleted() {
        if (firebaseHelper.getCurrentUser() != null) {
            String userId = firebaseHelper.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("completedTutorial", true);

            firebaseHelper.updateUserData(userId, updates)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully marked tutorial as completed
                    })
                    .addOnFailureListener(e -> {
                        // Failed to update
                        Toast.makeText(TutorialActivity.this,
                                "Failed to save tutorial status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(TutorialActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}