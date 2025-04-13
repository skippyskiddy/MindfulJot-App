package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import utils.FirebaseHelper;
import utils.LoginManager;

/**
 * Welcome screen shown after login or signup
 * Directs users either to home screen or onboarding flow based on their status
 */
public class WelcomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvDescription;
    private Button btnBegin;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String userName;
    private boolean isNewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Get user information
        userName = loginManager.getUserName(this);

        // Check if this is a new user
        if (getIntent().hasExtra("isNewUser")) {
            isNewUser = getIntent().getBooleanExtra("isNewUser", false);
        } else {
            // Check Firebase user status for completion of tutorial
            FirebaseUser currentUser = firebaseHelper.getCurrentUser();
            if (currentUser != null) {
                checkUserTutorialStatus(currentUser.getUid());
            } else {
                // Default to returning user if we can't determine
                isNewUser = false;
            }
        }

        // Initialize views
        initViews();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvDescription = findViewById(R.id.tv_description);
        btnBegin = findViewById(R.id.btn_begin);

        // Set welcome message with user's name
        tvWelcome.setText("Welcome, " + userName);
    }

    private void setupListeners() {
        btnBegin.setOnClickListener(v -> {
            if (isNewUser) {
                // New user - navigate to first onboarding screen
                navigateToOnboarding();
            } else {
                // Returning user - navigate to home screen
                navigateToHome();
            }
        });
    }

    /**
     * Checks user's tutorial completion status in Firebase
     */
    private void checkUserTutorialStatus(String userId) {
        firebaseHelper.getUserData(userId, new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean completedTutorial = snapshot.child("completedTutorial").getValue(Boolean.class);
                    isNewUser = completedTutorial == null || !completedTutorial;
                } else {
                    isNewUser = true; // Default to new user if no data found
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                isNewUser = false; // Default to returning user on error
            }
        });
    }

    /**
     * Navigate to the onboarding goals screen
     */
    private void navigateToOnboarding() {
        Intent intent = new Intent(WelcomeActivity.this, GoalsActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to the home screen
     */
    private void navigateToHome() {
        Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}