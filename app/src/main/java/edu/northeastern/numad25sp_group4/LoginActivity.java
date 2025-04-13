package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import utils.FirebaseHelper;
import utils.LoginManager;

/**
 * Login screen that handles user authentication
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private TextView tvForgotPassword;
    private ImageButton btnBack;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase helper and login manager
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Initialize views
        initViews();

        // Set up click listeners
        setupListeners();

        // Add the back press callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tv_sign_up);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
            startActivity(intent);
            finish();
        });

        // Login button click listener
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Sign up text click listener
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            // Add source information for proper back navigation
            intent.putExtra("source", "login");
            startActivity(intent);
        });

        // Forgot password text click listener
        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Implement forgot password functionality or show a dialog
            Toast.makeText(LoginActivity.this, "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Validates input fields and attempts to sign in the user
     */
    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Show progress (could add a progress bar here)
        btnLogin.setEnabled(false);

        // Attempt to sign in
        firebaseHelper.signInUser(email, password, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                btnLogin.setEnabled(true);

                if (task.isSuccessful()) {
                    // Sign in success
                    String userName = ""; // We'll get the user name from Firebase database

                    // Get the current user from Firebase
                    if (firebaseHelper.getCurrentUser() != null) {
                        // TODO for now we just use the email as the username until we fetch the actual name
                        userName = email.substring(0, email.indexOf('@'));

                        // Save login state
                        loginManager.saveLoginState(LoginActivity.this, userName);

                        // Check if user has completed tutorial
                        checkTutorialStatus();
                    }
                } else {
                    // Sign in failed
                    String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Authentication failed";
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Checks if the user has completed the tutorial to determine where to navigate
     */
    private void checkTutorialStatus() {
        // TODO for now we just navigate to HomeActivity directly
        // In a complete implementation, we'd check if the user has completed the tutorial

        // You can implement this check by querying Firebase for the user's completedTutorial flag
        navigateToHome();
    }

    /**
     * Navigates to the home screen
     */
    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to the welcome/tutorial screen
     */
    private void navigateToTutorial() {
        //TODO
        //Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //startActivity(intent);
        finish();
    }

}