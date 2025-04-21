package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Emotion;
import models.EmotionEntry;
import utils.FirebaseHelper;
import utils.LoginManager;
import utils.NotificationHelper;
import utils.NotificationScheduler;

public class HomeActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private TextView tvGreeting;
    private TextView tvLastCheckin;
    private CardView cardAddEntry;
    private Button btnAddEntry;
    private BottomNavigationView bottomNavigationView;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
        }

        // Initialize views
        initViews();

        // Set greeting based on time of day
        setGreeting();

        // Set last check-in info
        loadLastCheckinInfo();

        // Set up listeners
        setupListeners();

        // Initialize notifications
        initializeNotifications();
    }

    /**
     * Initializes notifications based on user preferences
     */
    private void initializeNotifications() {
        if (firebaseHelper.getCurrentUser() != null) {
            String userId = firebaseHelper.getCurrentUser().getUid();

            firebaseHelper.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String preference = snapshot.child("notificationPreference").getValue(String.class);
                        String name = snapshot.child("name").getValue(String.class);

                        if (preference != null && !preference.equals("none")) {
                            // Create notification channel first for Android 8.0+
                            NotificationHelper.createNotificationChannel(HomeActivity.this);

                            // Schedule notifications
                            NotificationScheduler.scheduleNotifications(
                                    HomeActivity.this,
                                    preference,
                                    name != null ? name : ""
                            );
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Failed to get user data
                }
            });
        }
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvLastCheckin = findViewById(R.id.tv_last_checkin);
        cardAddEntry = findViewById(R.id.card_add_entry);
        btnAddEntry = findViewById(R.id.btn_add_entry);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the Home tab as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupListeners() {
        // Set up button click listener for emotion entry
        btnAddEntry.setOnClickListener(v -> {
            // Add a small animation to the button when clicked
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        // Return to original size
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    // Navigate to primary emotion selection screen
                                    Intent intent = new Intent(HomeActivity.this, PrimaryEmotionActivity.class);
                                    startActivity(intent);
                                    // Apply custom transition animation
                                    overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                                });
                    })
                    .start();
        });

        // Also keep the card clickable for users who prefer clicking the card
        cardAddEntry.setOnClickListener(v -> {
            // Add a small animation to the card when clicked
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        // Return to original size
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    // Navigate to primary emotion selection screen
                                    Intent intent = new Intent(HomeActivity.this, PrimaryEmotionActivity.class);
                                    startActivity(intent);
                                    // Apply custom transition animation
                                    overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                                });
                    })
                    .start();
        });

        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            // Already on home screen
            return true;
        } else if (itemId == R.id.nav_entries) {
            Intent intent = new Intent(HomeActivity.this, EntriesActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_analytics) {
            Intent intent = new Intent(HomeActivity.this, AnalyticsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void setGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (timeOfDay >= 0 && timeOfDay < 12) {
            greeting = "Good morning";
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        // Get user name from LoginManager
        String userName = loginManager.getUserName(this);

        // If user name is empty, try to fetch it from Firebase
        if (userName.isEmpty() && userId != null) {
            fetchUserName();
        } else {
            tvGreeting.setText(greeting + ", " + userName);
        }
    }

    /**
     * Fetches the user's name from Firebase
     */
    private void fetchUserName() {
        if (userId != null) {
            firebaseHelper.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            // Update the greeting with the user's name
                            Calendar c = Calendar.getInstance();
                            int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
                            String greeting;
                            if (timeOfDay >= 0 && timeOfDay < 12) {
                                greeting = "Good morning";
                            } else if (timeOfDay >= 12 && timeOfDay < 16) {
                                greeting = "Good afternoon";
                            } else {
                                greeting = "Good evening";
                            }
                            tvGreeting.setText(greeting + ", " + name);

                            // Save name to LoginManager for future use
                            loginManager.saveLoginState(HomeActivity.this, name);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }
    }

    /**
     * Loads the last check-in information from Firebase
     */
    private void loadLastCheckinInfo() {
        if (userId != null) {
            firebaseHelper.getLatestEmotionEntry(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                        // There's at least one entry
                        for (DataSnapshot entrySnapshot : snapshot.getChildren()) {
                            EmotionEntry entry = entrySnapshot.getValue(EmotionEntry.class);
                            if (entry != null && entry.getTimestamp() != null) {
                                displayLastCheckinInfo(entry);
                                return;
                            }
                        }
                    } else {
                        // No entries yet
                        tvLastCheckin.setText("No entries - log how you feel with the check in button");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                    tvLastCheckin.setText("No entries - log how you feel with the check in button");
                }
            });
        } else {
            // If no userId, show default text
            tvLastCheckin.setText("No entries - log how you feel with the check in button");
        }
    }

    /**
     * Displays the last check-in information with colored emotions based on their categories
     */
    private void displayLastCheckinInfo(EmotionEntry entry) {
        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String date = dateFormat.format(entry.getTimestamp());

        // Get emotions
        List<Emotion> emotions = entry.getEmotions();

        if (emotions.isEmpty()) {
            tvLastCheckin.setText("You last checked in on " + date + ".\nNo emotion was logged.");
        } else if (emotions.size() == 1) {
            // Create spannable string to color just the emotion name
            String prefix = "You last checked in on " + date + ".\n";
            String emotionPart = "Your last logged emotion was ";
            String emotionName = emotions.get(0).getName();
            SpannableString spannableString = new SpannableString(prefix + emotionPart + emotionName);

            // Add size span for the emotion text part
            spannableString.setSpan(
                    new android.text.style.RelativeSizeSpan(1.2f),
                    prefix.length(),
                    prefix.length() + emotionPart.length() + emotionName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Add color span for the emotion name only
            int color = getColorForCategory(emotions.get(0).getCategory());
            spannableString.setSpan(
                    new ForegroundColorSpan(color),
                    prefix.length() + emotionPart.length(),
                    prefix.length() + emotionPart.length() + emotionName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Set the styled text
            tvLastCheckin.setText(spannableString);
        } else {
            // For two emotions, color each one separately
            String prefix = "You last checked in on " + date + ".\n";
            String emotionPart = "Your last logged emotions were ";
            String firstEmotionName = emotions.get(0).getName();
            String connector = " and ";
            String secondEmotionName = emotions.get(1).getName();

            SpannableString spannableString = new SpannableString(
                    prefix + emotionPart + firstEmotionName + connector + secondEmotionName
            );

            // Add size span for the emotion text part
            spannableString.setSpan(
                    new android.text.style.RelativeSizeSpan(1.2f),
                    prefix.length(),
                    prefix.length() + emotionPart.length() + firstEmotionName.length() + connector.length() + secondEmotionName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Color the first emotion
            int firstColor = getColorForCategory(emotions.get(0).getCategory());
            spannableString.setSpan(
                    new ForegroundColorSpan(firstColor),
                    prefix.length() + emotionPart.length(),
                    prefix.length() + emotionPart.length() + firstEmotionName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Color the second emotion
            int secondColor = getColorForCategory(emotions.get(1).getCategory());
            spannableString.setSpan(
                    new ForegroundColorSpan(secondColor),
                    prefix.length() + emotionPart.length() + firstEmotionName.length() + connector.length(),
                    prefix.length() + emotionPart.length() + firstEmotionName.length() + connector.length() + secondEmotionName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Set the styled text
            tvLastCheckin.setText(spannableString);
        }
    }

    /**
     * Get color for emotion category
     */
    private int getColorForCategory(Emotion.Category category) {
        switch (category) {
            case HIGH_ENERGY_PLEASANT:
                return Color.parseColor("#FFEE82"); // Bright yellow
            case HIGH_ENERGY_UNPLEASANT:
                return Color.parseColor("#FF6B6B"); // Red
            case LOW_ENERGY_PLEASANT:
                return Color.parseColor("#7FE57F"); // Green
            case LOW_ENERGY_UNPLEASANT:
                return Color.parseColor("#7FB8FF"); // Blue
            default:
                return Color.WHITE;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refreshes the last check-in info when returning to this screen
        loadLastCheckinInfo();
    }
}