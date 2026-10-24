package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for user login request payload to /api/auth/login.
 */
public class LoginRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public LoginRequest() {
    }

    /**
     * Parameterized constructor for LoginRequest.
     *
     * @param email    User email address
     * @param password User plain text password
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Gets the user email address.
     *
     * @return Email string
     */
    public String getEmail() {
        return email != null ? email : "";
    }

    /**
     * Sets the user email address.
     *
     * @param email Email string
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user password.
     *
     * @return Password string
     */
    public String getPassword() {
        return password != null ? password : "";
    }

    /**
     * Sets the user password.
     *
     * @param password Password string
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
