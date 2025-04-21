package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "TutorialActivity";
    private ViewPager2 viewPager;
    private ImageView ivNextArrow, ivBackArrow;
    private Button btnGetStarted;
    private View progressDot1, progressDot2, progressDot3;
    private FirebaseHelper firebaseHelper;
    private TutorialPagerAdapter adapter;
    private ImageView ivCarouselPrev, ivCarouselNext;


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

        // Navigation
        ivNextArrow = findViewById(R.id.iv_next_arrow);
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        btnGetStarted = findViewById(R.id.btn_get_started);

        // Progress dots
        progressDot1 = findViewById(R.id.progress_dot_1);
        progressDot2 = findViewById(R.id.progress_dot_2);
        progressDot3 = findViewById(R.id.progress_dot_3);

        ivCarouselPrev = findViewById(R.id.iv_carousel_prev);
        ivCarouselNext = findViewById(R.id.iv_carousel_next);
    }

    private void setupCarousel() {
        try {
            // Create tutorial slides
            List<TutorialPagerAdapter.TutorialSlide> slides = new ArrayList<>();

            // Add slides
            slides.add(new TutorialPagerAdapter.TutorialSlide(R.drawable.carousel_one_log_emotion));
            slides.add(new TutorialPagerAdapter.TutorialSlide(R.drawable.carousel_two_primary_emotion));
            slides.add(new TutorialPagerAdapter.TutorialSlide(R.drawable.carousel_three_journal_summary));
            slides.add(new TutorialPagerAdapter.TutorialSlide(R.drawable.carousel_four_analytics_tab));


            // Set up adapter
            adapter = new TutorialPagerAdapter(this, slides);
            viewPager.setAdapter(adapter);

            // Important - set orientation to horizontal
            viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

            // Set up ViewPager callback
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);

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
        } catch (Exception e) {
            Log.e(TAG, "Error setting up carousel: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error loading tutorial slides", Toast.LENGTH_SHORT).show();
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

        // Back arrow - go to previous screen
        ivBackArrow.setOnClickListener(v -> {
            Intent intent = new Intent(TutorialActivity.this, NotificationsActivity.class);
            startActivity(intent);
            finish();
        });

        // Get Started button - complete onboarding
        btnGetStarted.setOnClickListener(v -> {
            // Mark tutorial as completed in Firebase
            markTutorialCompleted();

            // Navigate to the home screen
            navigateToHome();
        });

        ivCarouselPrev.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition > 0) {
                viewPager.setCurrentItem(currentPosition - 1);
            }
        });

        ivCarouselNext.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition < TOTAL_SLIDES - 1) {
                viewPager.setCurrentItem(currentPosition + 1);
            }
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