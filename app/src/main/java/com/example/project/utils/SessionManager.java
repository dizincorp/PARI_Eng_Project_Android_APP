package com.example.project.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing persistent user authentication session via SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "ParkShareSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    /**
     * Constructor for SessionManager.
     *
     * @param context Application context
     */
    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Saves user login session state.
     *
     * @param email User email
     * @param name  User full name
     */
    public void createLoginSession(String email, String name) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email != null ? email : "jan_kowalski@gmail.com");
        editor.putString(KEY_USER_NAME, name != null ? name : "Jan Kowalski");
        editor.apply();
    }

    /**
     * Checks if user is currently logged in.
     *
     * @return True if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Gets stored user email.
     *
     * @return Stored email address
     */
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "jan_kowalski@gmail.com");
    }

    /**
     * Gets stored user name.
     *
     * @return Stored user full name
     */
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Jan Kowalski");
    }

    /**
     * Updates stored user name.
     *
     * @param name New user name
     */
    public void updateUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    /**
     * Clears user session data upon logout.
     */
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}