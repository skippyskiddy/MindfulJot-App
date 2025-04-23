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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

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
                    // Get the current user from Firebase
                    if (firebaseHelper.getCurrentUser() != null) {
                        // Create a temporary username from email (just for initial login state)
                        // This will be replaced with the actual name from Firebase
                        String tempUserName = email.substring(0, email.indexOf('@'));

                        // Set a temporary login state with email username
                        loginManager.saveLoginState(LoginActivity.this, tempUserName);

                        // Fetch the actual user name from Firebase
                        fetchUserData(firebaseHelper.getCurrentUser().getUid());
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
     * Fetches the user's actual name from Firebase after login
     */
    private void fetchUserData(String userId) {
        firebaseHelper.getUserData(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the actual user name
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        // Update the login state with the actual name
                        loginManager.saveLoginState(LoginActivity.this, name);
                    }

                    // Check if user has completed tutorial
                    Boolean completedTutorial = snapshot.child("completedTutorial").getValue(Boolean.class);
                    if (completedTutorial != null && completedTutorial) {
                        navigateToHome();
                    } else {
                        navigateToTutorial();
                    }
                } else {
                    // User data doesn't exist, navigate to home anyway
                    navigateToHome();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Database error, just navigate to home
                navigateToHome();
            }
        });
    }

    /**
     * Checks if the user has completed the tutorial to determine where to navigate
     */
    private void checkTutorialStatus() {
        // TODO for now we just navigate to HomeActivity directly
        // In a complete implementation, we'd check if the user has completed the tutorial
        // every time.

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
        Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}