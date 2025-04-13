package edu.northeastern.numad25sp_group4;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import utils.FirebaseHelper;
import utils.LoginManager;

/**
 * Activity for user registration
 */
public class SignupActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etEmail, etUsername, etPassword, etConfirmPassword, etName;
    private Spinner spinnerAge;
    private Button btnCreateAccount;
    private ImageView ivPasswordMatch;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String sourceActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Get source activity to handle back navigation correctly
        if (getIntent().hasExtra("source")) {
            sourceActivity = getIntent().getStringExtra("source");
        }

        // Initialize helpers
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Initialize views
        initViews();

        // Setup the age spinner
        setupAgeSpinner();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etName = findViewById(R.id.et_name);
        spinnerAge = findViewById(R.id.spinner_age);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        ivPasswordMatch = findViewById(R.id.iv_password_match);
    }

    private void setupAgeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.age_ranges,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAge.setAdapter(adapter);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> handleBackNavigation());

        // Password confirmation matcher
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkPasswordMatch();
            }
        });

        // Create account button
        btnCreateAccount.setOnClickListener(v -> attemptSignup());
    }

    /**
     * Handles back button navigation based on which activity launched the signup
     */
    private void handleBackNavigation() {
        if ("login".equals(sourceActivity)) {
            // Navigate back to LoginActivity
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            // Default: go back to SplashActivity
            Intent intent = new Intent(SignupActivity.this, SplashActivity.class);
            startActivity(intent);
        }
        finish();
    }

    /**
     * Checks if the confirmation password matches and updates the UI indicator
     */
    private void checkPasswordMatch() {
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(confirmPassword)) {
            // Hide the indicator if field is empty
            ivPasswordMatch.setVisibility(View.INVISIBLE);
        } else if (password.equals(confirmPassword)) {
            // Passwords match - show checkmark
            ivPasswordMatch.setImageResource(R.drawable.ic_check);
            ivPasswordMatch.setVisibility(View.VISIBLE);
        } else {
            // Passwords don't match - show X
            ivPasswordMatch.setImageResource(R.drawable.ic_x);
            ivPasswordMatch.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Validates all fields and attempts to register the user
     */
    private void attemptSignup() {
        // Reset errors
        etEmail.setError(null);
        etUsername.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
        etName.setError(null);

        // Get values
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String name = etName.getText().toString().trim();
        String ageRange = spinnerAge.getSelectedItemPosition() > 0 ?
                spinnerAge.getSelectedItem().toString() : "";

        boolean cancel = false;
        View focusView = null;

        // Check name
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            focusView = etName;
            cancel = true;
        } else if (!isNameValid(name)) {
            etName.setError("Name can only contain letters");
            focusView = etName;
            cancel = true;
        }

        // Check password confirmation
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            focusView = etConfirmPassword;
            cancel = true;
        }

        // Check password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            focusView = etPassword;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            etPassword.setError("Password must be 6 characters long and contain at least one capital letter and one number");
            focusView = etPassword;
            cancel = true;
        }

        // Check username
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            focusView = etUsername;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            etUsername.setError("Username can only contain letters, numbers, and underscores");
            focusView = etUsername;
            cancel = true;
        }

        // Check email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            focusView = etEmail;
            cancel = true;
        } else if (!isEmailValid(email)) {
            etEmail.setError("Invalid email format");
            focusView = etEmail;
            cancel = true;
        }

        if (cancel) {
            // There was an error; focus the first form field with an error
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // Show progress (could add a progress bar)
            btnCreateAccount.setEnabled(false);

            // Attempt to create account
            createAccount(email, password, name, username, ageRange);
        }
    }

    /**
     * Creates a new user account with Firebase
     */
    private void createAccount(String email, String password, String name, String username, String ageRange) {
        firebaseHelper.createUser(email, password, name, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                btnCreateAccount.setEnabled(true);

                if (task.isSuccessful()) {
                    // Account created successfully
                    Toast.makeText(SignupActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();

                    // Save login state
                    loginManager.saveLoginState(SignupActivity.this, name);

                    // Navigate to onboarding/tutorial
                    navigateToWelcome();
                } else {
                    // If sign up fails, display a message to the user
                    String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Account creation failed";

                    // Check for specific error types
                    if (errorMessage.contains("email address is already in use")) {
                        etEmail.setError("Email already in use");
                        etEmail.requestFocus();
                    } else {
                        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /**
     * Navigate to the welcome/onboarding screen
     */
    private void navigateToWelcome() {
        Intent intent = new Intent(SignupActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Validation helper methods
     */
    private boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isUsernameValid(String username) {
        // Username can only contain letters, numbers, and underscores
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean isPasswordValid(String password) {
        // Password must contain at least one capital letter and one number
        return password.matches(".*[A-Z].*") && password.matches(".*[0-9].*");
    }

    private boolean isNameValid(String name) {
        // Name can only contain letters and spaces
        return name.matches("^[a-zA-Z ]+$");
    }

}