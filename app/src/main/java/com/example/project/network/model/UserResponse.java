package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing user profile data returned within authentication responses.
 */
public class UserResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("role")
    private String role;

    @SerializedName("loyaltyPoints")
    private Integer loyaltyPoints;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public UserResponse() {
        this.loyaltyPoints = 0;
    }

    /**
     * Parameterized constructor for UserResponse.
     *
     * @param id            Unique user identifier (UUID)
     * @param email         User email address
     * @param firstName     User first name
     * @param lastName      User last name
     * @param role          User role (e.g. "DRIVER", "OWNER")
     * @param loyaltyPoints Accumulated loyalty reward points
     * @param avatarUrl     Public avatar image URL (nullable)
     */
    public UserResponse(String id, String email, String firstName, String lastName,
                        String role, Integer loyaltyPoints, String avatarUrl) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.loyaltyPoints = loyaltyPoints != null ? loyaltyPoints : 0;
        this.avatarUrl = avatarUrl;
    }

    /**
     * Gets the unique user identifier.
     *
     * @return User ID string
     */
    public String getId() {
        return id != null ? id : "";
    }

    /**
     * Sets the unique user identifier.
     *
     * @param id User ID string
     */
    public void setId(String id) {
        this.id = id;
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
     * Gets the user first name.
     *
     * @return First name string
     */
    public String getFirstName() {
        return firstName != null ? firstName : "";
    }

    /**
     * Sets the user first name.
     *
     * @param firstName First name string
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user last name.
     *
     * @return Last name string
     */
    public String getLastName() {
        return lastName != null ? lastName : "";
    }

    /**
     * Sets the user last name.
     *
     * @param lastName Last name string
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the full name formatted as "FirstName LastName".
     *
     * @return Formatted full name
     */
    public String getFullName() {
        String first = getFirstName();
        String last = getLastName();
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return first + " " + last;
    }

    /**
     * Gets the user active role.
     *
     * @return Role string (e.g. "DRIVER", "OWNER")
     */
    public String getRole() {
        return role != null ? role : "DRIVER";
    }

    /**
     * Sets the user active role.
     *
     * @param role Role string
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the user accumulated loyalty points.
     *
     * @return Loyalty points count
     */
    public int getLoyaltyPoints() {
        return loyaltyPoints != null ? loyaltyPoints : 0;
    }

    /**
     * Sets the user loyalty points count.
     *
     * @param loyaltyPoints Points count
     */
    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints != null ? loyaltyPoints : 0;
    }

    /**
     * Gets the user avatar URL.
     *
     * @return Avatar URL, or null if not set
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * Sets the user avatar URL.
     *
     * @param avatarUrl Avatar URL
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
