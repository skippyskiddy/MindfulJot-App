package edu.northeastern.numad25sp_group4;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import utils.FirebaseHelper;
import utils.LoginManager;

public class SettingsActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private TextView tvUserGreeting;
    private TextView tvEditProfile;
    private TextView tvNotifications;
    private TextView tvAboutApp;
    private TextView tvLogout;
    private BottomNavigationView bottomNavigationView;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
        }

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        tvEditProfile = findViewById(R.id.tv_edit_profile);
        tvNotifications = findViewById(R.id.tv_notifications);
        tvAboutApp = findViewById(R.id.tv_about_app);
        tvLogout = findViewById(R.id.tv_logout);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the Settings tab as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
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
                            tvUserGreeting.setText("Hi, " + name);
                            // Save name to LoginManager for future use
                            loginManager.saveLoginState(SettingsActivity.this, name);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                    tvUserGreeting.setText("Hi there");
                }
            });
        }
    }

    private void setupListeners() {
        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(this);

        // Edit Profile click listener
        tvEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });

        // Notifications click listener
        tvNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, NotificationSettingsActivity.class);
                startActivity(intent);
            }
        });

        // About App click listener
        tvAboutApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AboutAppActivity.class);
                startActivity(intent);
            }
        });

        // Logout click listener
        tvLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmation();
            }
        });
    }

    /**
     * Shows confirmation dialog before logging out
     */
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performLogout();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Performs the logout operation
     */
    private void performLogout() {
        // Clear login state in preferences
        loginManager.clearLoginState(this);

        // Navigate back to the splash screen
        Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_entries) {
            Intent intent = new Intent(SettingsActivity.this, EntriesActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_analytics) {
            Intent intent = new Intent(SettingsActivity.this, AnalyticsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.nav_settings) {
            // Already on settings screen
            return true;
        }
        return false;
    }
}