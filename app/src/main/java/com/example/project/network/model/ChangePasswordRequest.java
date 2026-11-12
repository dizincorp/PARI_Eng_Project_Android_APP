package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for changing account password via POST /api/auth/change-password.
 */
public class ChangePasswordRequest {

    @SerializedName("currentPassword")
    private String currentPassword;

    @SerializedName("newPassword")
    private String newPassword;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public ChangePasswordRequest() {
    }

    /**
     * Parameterized constructor for ChangePasswordRequest.
     *
     * @param currentPassword Current valid account password
     * @param newPassword     New password (8 to 72 characters)
     */
    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword != null ? currentPassword : "";
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword != null ? newPassword : "";
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
