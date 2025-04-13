package utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Utility class to handle login state and persistence throughout the app
 */
public class LoginManager {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_NAME = "userName";

    private static LoginManager instance;
    private FirebaseAuth mAuth;

    private LoginManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized LoginManager getInstance() {
        if (instance == null) {
            instance = new LoginManager();
        }
        return instance;
    }

    /**
     * Saves the login state to SharedPreferences
     */
    public void saveLoginState(Context context, String userName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_NAME, userName);
        editor.apply();
    }

    /**
     * Clears the login state from SharedPreferences on logout
     */
    public void clearLoginState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_NAME);
        editor.apply();

        // Sign out from Firebase
        mAuth.signOut();
    }

    /**
     * Checks if the user is logged in both in SharedPreferences and Firebase
     */
    public boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedInPrefs = prefs.getBoolean(KEY_IS_LOGGED_IN, false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean isLoggedInFirebase = (currentUser != null);

        // User is logged in if both conditions are true
        return isLoggedInPrefs && isLoggedInFirebase;
    }

    /**
     * Gets the current user's name from SharedPreferences
     */
    public String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }

    /**
     * Gets the current Firebase user
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
}