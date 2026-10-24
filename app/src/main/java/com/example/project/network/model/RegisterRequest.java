package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for user account registration payload to /api/auth/register.
 */
public class RegisterRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("role")
    private String role;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public RegisterRequest() {
        this.role = "DRIVER";
    }

    /**
     * Parameterized constructor for RegisterRequest.
     *
     * @param email       User email address
     * @param password    User plain text password (8-72 characters)
     * @param firstName   User first name
     * @param lastName    User last name
     * @param phoneNumber User phone number
     * @param role        User role (e.g. "DRIVER", "OWNER")
     */
    public RegisterRequest(String email, String password, String firstName,
                           String lastName, String phoneNumber, String role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.role = role != null ? role : "DRIVER";
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
     * Gets the user contact phone number.
     *
     * @return Phone number string
     */
    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }

    /**
     * Sets the user contact phone number.
     *
     * @param phoneNumber Phone number string
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Gets the chosen user role.
     *
     * @return Role string (e.g. "DRIVER", "OWNER")
     */
    public String getRole() {
        return role != null ? role : "DRIVER";
    }

    /**
     * Sets the chosen user role.
     *
     * @param role Role string
     */
    public void setRole(String role) {
        this.role = role;
    }
}
