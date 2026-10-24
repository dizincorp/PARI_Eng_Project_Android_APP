package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing the authentication response payload returned by /api/auth/login.
 */
public class LoginResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("tokenType")
    private String tokenType;

    @SerializedName("expiresIn")
    private Long expiresIn;

    @SerializedName("user")
    private UserResponse user;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public LoginResponse() {
        this.tokenType = "Bearer";
        this.expiresIn = 0L;
    }

    /**
     * Parameterized constructor for LoginResponse.
     *
     * @param token     JWT authentication token
     * @param tokenType Token scheme (e.g. "Bearer")
     * @param expiresIn Token validity period in seconds
     * @param user      Authenticated user profile details
     */
    public LoginResponse(String token, String tokenType, Long expiresIn, UserResponse user) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresIn = expiresIn != null ? expiresIn : 0L;
        this.user = user;
    }

    /**
     * Parameterized convenience constructor accepting primitive long.
     *
     * @param token     JWT authentication token
     * @param tokenType Token scheme (e.g. "Bearer")
     * @param expiresIn Token validity period in seconds
     * @param user      Authenticated user profile details
     */
    public LoginResponse(String token, String tokenType, long expiresIn, UserResponse user) {
        this(token, tokenType, Long.valueOf(expiresIn), user);
    }

    /**
     * Gets the JWT token string.
     *
     * @return JWT token
     */
    public String getToken() {
        return token != null ? token : "";
    }

    /**
     * Sets the JWT token string.
     *
     * @param token JWT token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the authorization token type.
     *
     * @return Token type (e.g. "Bearer")
     */
    public String getTokenType() {
        return tokenType != null ? tokenType : "Bearer";
    }

    /**
     * Sets the authorization token type.
     *
     * @param tokenType Token type
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Gets the token expiration lifespan in seconds.
     *
     * @return Lifespan in seconds
     */
    public long getExpiresIn() {
        return expiresIn != null ? expiresIn : 0L;
    }

    /**
     * Sets the token expiration lifespan in seconds.
     *
     * @param expiresIn Lifespan in seconds
     */
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn != null ? expiresIn : 0L;
    }

    /**
     * Gets the authenticated user profile details.
     *
     * @return UserResponse instance, or empty user object if null
     */
    public UserResponse getUser() {
        return user != null ? user : new UserResponse();
    }

    /**
     * Sets the authenticated user profile details.
     *
     * @param user User profile DTO
     */
    public void setUser(UserResponse user) {
        this.user = user;
    }

    /**
     * Gets the full Authorization header value (e.g. "Bearer eyJhbGci...").
     *
     * @return Formatted authorization header value
     */
    public String getAuthorizationHeader() {
        return getTokenType() + " " + getToken();
    }
}
