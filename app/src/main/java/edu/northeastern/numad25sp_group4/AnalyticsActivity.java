package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import models.Emotion;
import models.EmotionEntry;
import utils.FirebaseHelper;
import utils.LoginManager;

public class AnalyticsActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private TextView tvAnalyticsTitle, tvStreak, tvLogFrequency, tvBreakdownTitle;
    private TextView tvHighEnergyPleasant, tvLowEnergyPleasant;
    private TextView tvHighEnergyUnpleasant, tvLowEnergyUnpleasant;
    private Spinner spinnerTimeframe;
    private BottomNavigationView bottomNavigationView;
    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String userId;
    private boolean isFirstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
        }

        // Initialize views
        initViews();

        // Set personalized title
        setTitle();

        loadStreak();

        // Set up spinner and load log frequency and emotion breakdown
        setupSpinner();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        tvAnalyticsTitle = findViewById(R.id.tv_analytics_title);
        tvStreak = findViewById(R.id.tv_streak);
        tvLogFrequency = findViewById(R.id.tv_log_frequency);
        tvBreakdownTitle = findViewById(R.id.tv_breakdown_title);
        tvHighEnergyPleasant = findViewById(R.id.tv_high_energy_pleasant);
        tvLowEnergyPleasant = findViewById(R.id.tv_low_energy_pleasant);
        tvHighEnergyUnpleasant = findViewById(R.id.tv_high_energy_unpleasant);
        tvLowEnergyUnpleasant = findViewById(R.id.tv_low_energy_unpleasant);
        spinnerTimeframe = findViewById(R.id.spinner_timeframe);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the Analytics tab as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_analytics);
    }

    private void setupListeners() {
        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(AnalyticsActivity.this, HomeActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_entries) {
            Intent intent = new Intent(AnalyticsActivity.this, EntriesActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_analytics) {
            return true;
        } else if (itemId == R.id.nav_settings) {
            // TODO: Navigate to settings screen
            // Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            // startActivity(intent);
            return true;
        }
        return false;
    }

    private void setTitle() {
        // Get user name from LoginManager
        String userName = loginManager.getUserName(this);

        if (!userName.isEmpty() && userId != null) {
            tvAnalyticsTitle.setText(userName + "'s Emotion Analytics");
        } else {
            // If not found locally, fetch from Firebase
            getUserData(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            tvAnalyticsTitle.setText(name + "'s Emotion Analytics");
                            loginManager.saveLoginState(AnalyticsActivity.this, name);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvAnalyticsTitle.setText("Your Emotion Analytics");
                }
            });
        }
    }

    private void getUserData(ValueEventListener listener) {
        if (userId != null) {
            firebaseHelper.getUserData(userId, listener);
        }
    }

    /**
     * Loads user's current streak and displays it in the streak TextView.
     *
     * This method uses a hybrid strategy for efficiency:
     * - It first checks if the user has entries for today and yesterday.
     * - If no entries are found for either day, the streak is 0.
     * - If there's only an entry yesterday, the streak is 1.
     * - If there is an entry today, it calls calculateFullStreak() to compute the full streak.
     *
     * This avoids querying all entries unless it's necessary to compute a longer streak.
     */
    private void loadStreak() {
        if (userId == null) {
            tvStreak.setText("You've been logging for 0 days.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        firebaseHelper.getEntriesForDate(userId, today, new FirebaseHelper.FilteredEntriesListener() {
            @Override
            public void onSuccess(List<EmotionEntry> entriesToday) {
                boolean hasToday = !entriesToday.isEmpty();

                firebaseHelper.getEntriesForDate(userId, yesterday, new FirebaseHelper.FilteredEntriesListener() {
                    @Override
                    public void onSuccess(List<EmotionEntry> entriesYesterday) {
                        boolean hasYesterday = !entriesYesterday.isEmpty();

                        // case 1: No entries for today or yesterday
                        if (!hasToday && !hasYesterday) {
                            tvStreak.setText("You've been logging for 0 days.");
                            return;
                        }

                        // case 2: Only yesterday has an entry
                        if (!hasToday) {
                            tvStreak.setText("You've been logging for 1 day - keep it up!");
                            return;
                        }

                        // case 3: Entry today → calculate full streak
                        calculateFullStreak();
                    }

                    @Override
                    public void onFailure(DatabaseError error) {

                    }
                });
            }

            @Override
            public void onFailure(DatabaseError error) {
                tvStreak.setText("Unable to load streak.");
            }
        });
    }


    private void calculateFullStreak() {
        LocalDate today = LocalDate.now();

        firebaseHelper.getAllEntries(userId, new FirebaseHelper.FilteredEntriesListener() {
            @Override
            public void onSuccess(List<EmotionEntry> entries) {
                Set<LocalDate> entryDates = new HashSet<>();

                for (EmotionEntry entry : entries) {
                    if (entry.getTimestamp() != null) {
                        LocalDate date = entry.getTimestamp()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        entryDates.add(date);
                    }
                }

                int streak = 0;
                LocalDate current = today;
                while (entryDates.contains(current)) {
                    streak++;
                    current = current.minusDays(1);
                }

                String msg = "You've been logging for "
                        + streak + " day" + (streak == 1 ? "" : "s")
                        + " – congratulations!";
                tvStreak.setText(msg);
            }

            @Override
            public void onFailure(DatabaseError error) {
                tvStreak.setText("Unable to load streak.");
            }
        });
    }

    public enum Timeframe {
        WEEK("This Week"),
        MONTH("This Month"),
        YEAR("This Year");

        private final String label;

        Timeframe(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }


    private void setupSpinner() {
        spinnerTimeframe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Timeframe selection = (Timeframe) parent.getItemAtPosition(position);
                loadLogFrequency(selection);
                loadEmotionBreakdown(selection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<Timeframe> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Timeframe.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeframe.setAdapter(adapter);
    }

    private LocalDate getStartDateForTimeframe(Timeframe timeframe) {
        LocalDate today = LocalDate.now();

        switch (timeframe) {
            case WEEK:
                DayOfWeek currentDayOfWeek = today.getDayOfWeek();
                int daysSinceSunday = currentDayOfWeek.getValue() % 7; // Sunday = 7 → 0
                return today.minusDays(daysSinceSunday);
            case MONTH:
                return today.withDayOfMonth(1);
            case YEAR:
                return today.withDayOfYear(1);
            default:
                return today.minusDays(7); // fallback
        }
    }


    private void loadLogFrequency(Timeframe timeframe) {
        if (userId == null) {
            tvLogFrequency.setText("Unable to load log frequency.");
            return;
        }

        LocalDate startDate = getStartDateForTimeframe(timeframe);
        LocalDate endDate = LocalDate.now();

        firebaseHelper.getEntriesInRange(userId, startDate, endDate, new FirebaseHelper.FilteredEntriesListener() {
            @Override
            public void onSuccess(List<EmotionEntry> entries) {
                int totalLogs = entries.size();
                int totalEmotions = countTotalEmotions(entries);

                String message = String.format(Locale.getDefault(),
                        "You’ve checked in %d time%s %s, logging %d emotion%s.",
                        totalLogs,
                        totalLogs == 1 ? "" : "s",
                        timeframe.toString().toLowerCase(),
                        totalEmotions,
                        totalEmotions == 1 ? "" : "s"
                );

                tvLogFrequency.setText(message);
            }

            @Override
            public void onFailure(DatabaseError error) {
                tvLogFrequency.setText("Unable to load log frequency.");
            }
        });
    }

    private int countTotalEmotions(List<EmotionEntry> entries) {
        int total = 0;
        for (EmotionEntry entry : entries) {
            if (entry.getEmotions() != null) {
                total += entry.getEmotions().size();
            }
        }
        return total;
    }

    private void loadEmotionBreakdown(Timeframe timeframe) {
        if (userId == null) {
            tvHighEnergyPleasant.setText("High energy pleasant: --");
            tvLowEnergyPleasant.setText("Low energy pleasant: --");
            tvHighEnergyUnpleasant.setText("High energy unpleasant: --");
            tvLowEnergyUnpleasant.setText("Low energy unpleasant: --");
            return;
        }

        LocalDate startDate = getStartDateForTimeframe(timeframe);
        LocalDate endDate = LocalDate.now();

        String custom_tf = timeframe.toString().toLowerCase();
        tvBreakdownTitle.setText("This is your emotion breakdown for " + custom_tf + ":");

        firebaseHelper.getEntriesInRange(userId, startDate, endDate, new FirebaseHelper.FilteredEntriesListener() {
            @Override
            public void onSuccess(List<EmotionEntry> entries) {
                int hep = 0, lep = 0, heu = 0, leu = 0;
                int total = 0;

                for (EmotionEntry entry : entries) {
                    if (entry.getEmotions() != null) {
                        for (Emotion emotion : entry.getEmotions()) {
                            switch (emotion.getCategory()) {
                                case HIGH_ENERGY_PLEASANT:
                                    hep++;
                                    break;
                                case LOW_ENERGY_PLEASANT:
                                    lep++;
                                    break;
                                case HIGH_ENERGY_UNPLEASANT:
                                    heu++;
                                    break;
                                case LOW_ENERGY_UNPLEASANT:
                                    leu++;
                                    break;
                            }
                            total++;
                        }
                    }
                }

                // Protect against divide by 0
                if (total == 0) {
                    tvHighEnergyPleasant.setText("High energy pleasant: 0%");
                    tvLowEnergyPleasant.setText("Low energy pleasant: 0%");
                    tvHighEnergyUnpleasant.setText("High energy unpleasant: 0%");
                    tvLowEnergyUnpleasant.setText("Low energy unpleasant: 0%");
                    return;
                }

                tvHighEnergyPleasant.setText(String.format(Locale.getDefault(), "High energy pleasant: %d%%", (hep * 100) / total));
                tvLowEnergyPleasant.setText(String.format(Locale.getDefault(), "Low energy pleasant: %d%%", (lep * 100) / total));
                tvHighEnergyUnpleasant.setText(String.format(Locale.getDefault(), "High energy unpleasant: %d%%", (heu * 100) / total));
                tvLowEnergyUnpleasant.setText(String.format(Locale.getDefault(), "Low energy unpleasant: %d%%", (leu * 100) / total));
            }

            @Override
            public void onFailure(DatabaseError error) {
                tvHighEnergyPleasant.setText("High energy pleasant: --");
                tvLowEnergyPleasant.setText("Low energy pleasant: --");
                tvHighEnergyUnpleasant.setText("High energy unpleasant: --");
                tvLowEnergyUnpleasant.setText("Low energy unpleasant: --");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFirstResume) {
            isFirstResume = false;
            return; // Skip refresh logic on first open
        }

        // Refresh everything after first open
        Timeframe selectedTimeframe = (Timeframe) spinnerTimeframe.getSelectedItem();
        loadStreak();
        loadLogFrequency(selectedTimeframe);
        loadEmotionBreakdown(selectedTimeframe);
    }

}