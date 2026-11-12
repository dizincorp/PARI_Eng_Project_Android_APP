package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for updating the user profile via PUT /api/auth/profile.
 */
public class UpdateProfileRequest {

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public UpdateProfileRequest() {
    }

    /**
     * Parameterized constructor for UpdateProfileRequest.
     *
     * @param firstName   User's first name
     * @param lastName    User's last name
     * @param phoneNumber User's contact phone number
     * @param avatarUrl   Public URL or storage reference of the avatar
     */
    public UpdateProfileRequest(String firstName, String lastName, String phoneNumber, String avatarUrl) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }

    public String getFirstName() {
        return firstName != null ? firstName : "";
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName != null ? lastName : "";
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
