package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.exceptions.OutOfDateRangeException;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.EmotionEntry;
import utils.FirebaseHelper;
import utils.LoginManager;

public class EntriesActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private CalendarView calendarView;
    private TextView tvEntryLogTitle;
    private BottomNavigationView bottomNavigationView;
    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String userId;
    private boolean isFirstResume = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entries);

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
        }

        // Initialize views
        initViews();

        try {
            calendarView.setDate(Calendar.getInstance());
        } catch (OutOfDateRangeException e) {
            Log.w("EntriesActivity", "Date out of range on create: " + e.getMessage());
        }

        calendarView.setMaximumDate(Calendar.getInstance());
        loadCalendarDots();
        disableFutureDates();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        tvEntryLogTitle = findViewById(R.id.tv_entry_log_title);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the Entries tab as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_entries);
    }

    private void setupListeners() {
        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(this);

        calendarView.setOnCalendarDayClickListener(new OnCalendarDayClickListener() {
            @Override
            public void onClick(@NonNull CalendarDay calendarDay) {
                Calendar clickedDay = calendarDay.getCalendar();

                // Pass the date into the EntryListActivity
                Intent intent = new Intent(EntriesActivity.this, EntryListActivity.class);
                intent.putExtra("selectedDate", clickedDay.getTimeInMillis());
                startActivity(intent);
            }
        });
    }

    private void loadCalendarDots() {
        firebaseHelper.getAllEntries(userId, new FirebaseHelper.FilteredEntriesListener() {
            @Override
            public void onSuccess(List<EmotionEntry> entries) {
                Set<LocalDate> seenDates = new HashSet<>();
                List<CalendarDay> calendarDays = new ArrayList<>();

                for (EmotionEntry entry : entries) {
                    if (entry.getTimestamp() != null) {
                        // Convert Timestamp to LocalDate
                        LocalDate localDate = entry.getTimestamp()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        if (seenDates.add(localDate)) {
                            // Convert LocalDate back to Calendar for CalendarDay
                            Calendar cal = Calendar.getInstance();
                            cal.set(localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth());

                            CalendarDay calendarDay = new CalendarDay(cal);
                            calendarDay.setImageResource(R.drawable.ic_dot);
                            calendarDays.add(calendarDay);
                        }
                    }
                }
                calendarView.setCalendarDays(calendarDays);
            }

            @Override
            public void onFailure(com.google.firebase.database.DatabaseError error) {
                // Handle error
                Log.e("EntriesActivity", "Failed to load entries", error.toException());
            }
        });
    }

    private void disableFutureDates() {
        Calendar today = Calendar.getInstance();
        Calendar cursor = (Calendar) today.clone();
        cursor.add(Calendar.DAY_OF_MONTH, 1); // Start from tomorrow

        List<Calendar> disabledDays = new ArrayList<>();

        // Disable rest of month's worth of days
        for (int i = 0; i < 30; i++) {
            disabledDays.add((Calendar) cursor.clone());
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendarView.setDisabledDays(disabledDays);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(EntriesActivity.this, HomeActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_entries) {
            return true;
        } else if (itemId == R.id.nav_analytics) {
            Intent intent = new Intent(EntriesActivity.this, AnalyticsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_settings) {
            // TODO: Navigate to settings screen
            // Intent intent = new Intent(EntriesActivity.this, SettingsActivity.class);
            // startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFirstResume) {
            isFirstResume = false;
            return; // Skip refresh logic on first open
        }

        // Refresh everything after first open
        try {
            calendarView.setDate(Calendar.getInstance());
        } catch (OutOfDateRangeException e) {
            Log.w("EntriesActivity", "Date out of range: " + e.getMessage());
        }
        loadCalendarDots();
    }

}