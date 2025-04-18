package edu.northeastern.numad25sp_group4;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import utils.FirebaseHelper;
import utils.LoginManager;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ImageButton btnBack;
    private EditText etEmail, etUsername, etPassword, etConfirmPassword, etName;
    private ImageView ivPasswordMatch;
    private Button btnSaveChanges;

    private FirebaseHelper firebaseHelper;
    private LoginManager loginManager;
    private String userId;
    private String currentEmail;
    private String currentUsername;
    private String currentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase helper
        firebaseHelper = FirebaseHelper.getInstance();
        loginManager = LoginManager.getInstance();

        // Get current user ID
        if (firebaseHelper.getCurrentUser() != null) {
            userId = firebaseHelper.getCurrentUser().getUid();
            Log.d(TAG, "User ID: " + userId);
        } else {
            // If not logged in, go back to settings
            Toast.makeText(this, "You must be logged in to edit profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Load current user data
        loadUserData();

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
        ivPasswordMatch = findViewById(R.id.iv_password_match);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
    }

    private void loadUserData() {
        // Show loading state
        setFieldsEnabled(false);

        firebaseHelper.getUserData(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get user data
                    currentEmail = snapshot.child("email").getValue(String.class);
                    currentUsername = snapshot.child("username").getValue(String.class);
                    currentName = snapshot.child("name").getValue(String.class);

                    Log.d(TAG, "Loaded user data: Name=" + currentName +
                            ", Username=" + currentUsername +
                            ", Email=" + currentEmail);

                    // Set current values
                    etEmail.setText(currentEmail);
                    etUsername.setText(currentUsername);
                    etName.setText(currentName);

                    // Enable fields after data is loaded
                    setFieldsEnabled(true);
                } else {
                    Log.e(TAG, "User data snapshot doesn't exist");
                    Toast.makeText(EditProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(EditProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        etEmail.setEnabled(enabled);
        etUsername.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        etConfirmPassword.setEnabled(enabled);
        etName.setEnabled(enabled);
        btnSaveChanges.setEnabled(enabled);
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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

        // Save changes button
        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndSaveChanges();
            }
        });
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
     * Validates fields and saves changes if valid
     */
    private void validateAndSaveChanges() {
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

        // Check password confirmation if password was entered
        if (!TextUtils.isEmpty(password) && !password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            focusView = etConfirmPassword;
            cancel = true;
        }

        // Check password if entered
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
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
            setFieldsEnabled(false);

            // Save changes
            saveChanges(email, username, password, name);
        }
    }

    /**
     * Saves user changes to Firebase
     */
    private void saveChanges(final String email, final String username, final String password, final String name) {
        // Debug logging
        Log.d(TAG, "Saving changes - Email: " + email + ", Username: " + username + ", Name: " + name);

        // Create updates map for database
        final Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("username", username);

        // Only update email in database if it's changed
        if (!email.equals(currentEmail)) {
            updates.put("email", email);
        }

        // First update the user's profile in the database
        firebaseHelper.updateUserData(userId, updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Database update successful");

                        // Also update the login manager's stored username
                        loginManager.saveLoginState(EditProfileActivity.this, name);

                        // Update Firebase Auth if email has changed
                        if (!email.equals(currentEmail)) {
                            updateEmail(email);
                        } else if (!TextUtils.isEmpty(password)) {
                            // Update password if provided
                            updatePassword(password);
                        } else {
                            // No auth changes needed, we're done
                            showSuccess();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Database update failed: " + e.getMessage());
                        Toast.makeText(EditProfileActivity.this,
                                "Failed to update profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        setFieldsEnabled(true);
                    }
                });
    }

    /**
     * Updates the user's email in Firebase Auth
     */
    private void updateEmail(final String newEmail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updateEmail(newEmail)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Email update successful");

                                // Check if password also needs to be updated
                                if (!TextUtils.isEmpty(etPassword.getText().toString())) {
                                    updatePassword(etPassword.getText().toString());
                                } else {
                                    showSuccess();
                                }
                            } else {
                                // Email update failed, might need re-authentication
                                Log.e(TAG, "Email update failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));

                                Toast.makeText(EditProfileActivity.this,
                                        "Failed to update email. You may need to re-login.",
                                        Toast.LENGTH_SHORT).show();
                                setFieldsEnabled(true);
                            }
                        }
                    });
        }
    }

    /**
     * Updates the user's password in Firebase Auth
     */
    private void updatePassword(String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Password update successful");
                                showSuccess();
                            } else {
                                // Password update failed, might need re-authentication
                                Log.e(TAG, "Password update failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));

                                Toast.makeText(EditProfileActivity.this,
                                        "Failed to update password. You may need to re-login.",
                                        Toast.LENGTH_SHORT).show();
                                setFieldsEnabled(true);
                            }
                        }
                    });
        }
    }

    /**
     * Shows success message and finishes activity
     */
    private void showSuccess() {
        // Update current values to reflect changes
        currentName = etName.getText().toString().trim();
        currentUsername = etUsername.getText().toString().trim();
        currentEmail = etEmail.getText().toString().trim();

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

        // Verify that fields match expected values
        refreshUserFromDatabase();
    }

    /**
     * Double-check that database update was successful by re-reading data
     */
    private void refreshUserFromDatabase() {
        firebaseHelper.getUserData(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String updatedName = snapshot.child("name").getValue(String.class);
                    String updatedUsername = snapshot.child("username").getValue(String.class);

                    Log.d(TAG, "Verification - Database now has: Name=" + updatedName +
                            ", Username=" + updatedUsername);

                    if (!currentName.equals(updatedName) || !currentUsername.equals(updatedUsername)) {
                        Log.w(TAG, "Update verification failed - values don't match expected values");
                    } else {
                        Log.d(TAG, "Update verification successful");
                    }
                }

                // Finish the activity regardless
                setFieldsEnabled(true);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to verify update: " + error.getMessage());
                setFieldsEnabled(true);
                finish();
            }
        });
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
        // Password must be at least 6 characters and contain at least one capital letter and one number
        return password.length() >= 6 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[0-9].*");
    }

    private boolean isNameValid(String name) {
        // Name can only contain letters and spaces
        return name.matches("^[a-zA-Z ]+$");
    }
}