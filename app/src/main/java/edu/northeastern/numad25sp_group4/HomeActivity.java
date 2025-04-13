package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private TextView tvLastCheckin;
    private CardView cardAddEntry;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        initViews();

        // Set greeting based on time of day
        setGreeting();

        // Set last check-in info
        setLastCheckinInfo();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvLastCheckin = findViewById(R.id.tv_last_checkin);
        cardAddEntry = findViewById(R.id.card_add_entry);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the Home tab as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void setupListeners() {
        // Set up card click listener for emotion entry
        cardAddEntry.setOnClickListener(v -> {
            //TODO: Navigate to primary emotion selection screen
            //Intent intent = new Intent(HomeActivity.this, PrimaryEmotionActivity.class);
            //startActivity(intent);
        });

        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home screen
                return true;
            } else if (itemId == R.id.nav_entries) {
                // Navigate to entries screen
                //startActivity(new Intent(HomeActivity.this, EntriesActivity.class));
                return true;
            } else if (itemId == R.id.nav_analytics) {
                // Navigate to analytics screen
                //startActivity(new Intent(HomeActivity.this, AnalyticsActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                // Navigate to settings screen
                //startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });
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

        // Get user name from preferences or database
        String userName = getUserName();

        tvGreeting.setText(greeting + ", " + userName);
    }

    private String getUserName() {
        // TODO: et this from SharedPreferences or Firebase
        // For now, returning a placeholder
        return "Name";

        // Example with SharedPreferences:
        // SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        // return prefs.getString("userName", "Friend");
    }

    private void setLastCheckinInfo() {
        // TODO: get this info from Firebase database
        // For now, showing example placeholder text

        // Determine if user has any entries (from Firebase)
        boolean hasEntries = false; // This should be determined from database

        if (hasEntries) {
            // Example entry data - this would come from Firebase
            String date = "March 7, 2024";
            String emotion1 = "Anger";
            String emotion2 = null; // Set to actual emotion or null if only one emotion

            if (emotion2 != null) {
                tvLastCheckin.setText("You last checked in on " + date +
                        ". Your last logged emotions were " + emotion1 + " and " + emotion2);
            } else {
                tvLastCheckin.setText("You last checked in on " + date +
                        ". Your last logged emotion was " + emotion1);
            }
        } else {
            tvLastCheckin.setText("No entries - log how you feel with the check in button");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refreshes the last check-in info when returning to this screen
        setLastCheckinInfo();
    }
}