package com.example.project.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing persistent user authentication session, tokens, and credentials via SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "ParkShareSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_FIRST_NAME = "userFirstName";
    private static final String KEY_LAST_NAME = "userLastName";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_AVATAR_URI = "avatarUri";

    private static volatile SessionManager instance;

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    /**
     * Gets or initializes the singleton SessionManager instance.
     *
     * @param context Application context
     * @return Singleton SessionManager instance
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Gets the current SessionManager instance, if initialized.
     *
     * @return Singleton SessionManager, or null if not yet initialized
     */
    public static synchronized SessionManager getInstance() {
        return instance;
    }

    /**
     * Constructor for SessionManager.
     *
     * @param context Application context
     */
    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        instance = this;
    }

    /**
     * Saves complete user login session state with JWT token and user profile details.
     *
     * @param token  JWT authentication token
     * @param userId Unique user ID (UUID)
     * @param email  User email
     * @param name   User full name
     * @param role   User role (e.g. "DRIVER", "OWNER")
     */
    public void createLoginSession(String token, String userId, String email, String name, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_AUTH_TOKEN, token != null ? token : "");
        editor.putString(KEY_USER_ID, userId != null ? userId : "");
        editor.putString(KEY_USER_EMAIL, email != null ? email : "jan_kowalski@gmail.com");
        editor.putString(KEY_USER_NAME, name != null ? name : "Jan Kowalski");
        editor.putString(KEY_USER_ROLE, role != null ? role : "DRIVER");
        editor.apply();
    }

    /**
     * Backward-compatible convenience overload for creating user login session.
     *
     * @param email User email
     * @param name  User full name
     */
    public void createLoginSession(String email, String name) {
        createLoginSession("", "", email, name, "DRIVER");
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
     * Gets stored JWT authentication token.
     *
     * @return Stored token string
     */
    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, "");
    }

    /**
     * Gets formatted Authorization HTTP header value.
     *
     * @return "Bearer <token>" string, or empty string if no token exists
     */
    public String getAuthorizationHeader() {
        String token = getAuthToken();
        if (token.isEmpty()) return "";
        return "Bearer " + token;
    }

    /**
     * Gets stored unique user identifier.
     *
     * @return User ID string
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    /**
     * Gets stored user role.
     *
     * @return User role string
     */
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "DRIVER");
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
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            editor.putString(KEY_FIRST_NAME, parts[0]);
            editor.putString(KEY_LAST_NAME, parts[1]);
        } else {
            editor.putString(KEY_FIRST_NAME, name != null ? name : "");
        }
        editor.apply();
    }

    public String getFirstName() {
        return pref.getString(KEY_FIRST_NAME, "Jan");
    }

    public String getLastName() {
        return pref.getString(KEY_LAST_NAME, "Kowalski");
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "+48 500 123 456");
    }

    public String getAvatarUri() {
        return pref.getString(KEY_AVATAR_URI, "");
    }

    public void updateUserName(String firstName, String lastName) {
        editor.putString(KEY_FIRST_NAME, firstName != null ? firstName.trim() : "");
        editor.putString(KEY_LAST_NAME, lastName != null ? lastName.trim() : "");
        String fullName = ((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim();
        editor.putString(KEY_USER_NAME, fullName);
        editor.apply();
    }

    public void updateUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone != null ? phone.trim() : "");
        editor.apply();
    }

    public void updateAvatarUri(String uri) {
        editor.putString(KEY_AVATAR_URI, uri != null ? uri.trim() : "");
        editor.apply();
    }

    /**
     * Clears all user session and token data upon logout.
     */
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}
