package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import utils.LoginManager;

/**
 * Initial screen shown when the app launches.
 * Handles automatic login for users with existing sessions
 * and provides options to log in or create a new account.
 */
public class SplashActivity extends AppCompatActivity {

    private TextView tvAppName;
    private TextView tvAppTagline;
    private Button btnLogin;
    private Button btnCreateAccount;
    private FirebaseAuth mAuth;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth and LoginManager
        mAuth = FirebaseAuth.getInstance();
        loginManager = LoginManager.getInstance();

        // Find views
        ImageView ivAppLogo = findViewById(R.id.iv_app_logo);
        tvAppTagline = findViewById(R.id.tv_app_tagline);
        btnLogin = findViewById(R.id.btn_login);
        btnCreateAccount = findViewById(R.id.btn_create_account);

        // Apply animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.logo_fade_in);
        ivAppLogo.startAnimation(fadeIn);
        tvAppTagline.startAnimation(fadeIn);
        tvAppTagline.startAnimation(fadeIn);

        // Set up click listeners
        btnLogin.setOnClickListener(view -> {
            // Navigate to the LoginActivity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnCreateAccount.setOnClickListener(view -> {
            // Navigate to the SignupActivity
            Intent intent = new Intent(SplashActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // Check if user is already logged in
        checkLoginState();
    }

    /**
     * Checks if the user is already logged in and navigates accordingly
     */
    private void checkLoginState() {
        // Use LoginManager to check if user is logged in
        if (loginManager.isLoggedIn(this)) {
            // User is already logged in, navigate to HomeActivity
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(intent);
            finish(); // Close this activity so user can't navigate back
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Each time activity starts, check login state
        checkLoginState();
    }
}