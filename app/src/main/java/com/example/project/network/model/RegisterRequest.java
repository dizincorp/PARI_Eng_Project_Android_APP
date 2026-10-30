package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Transfer Object (DTO) for user account registration payload to /api/auth/register.
 * Dispatches an array of statuses / roles containing both "DRIVER" and "OWNER",
 * removing the need for the user to pick only one role during registration.
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

    @SerializedName("status")
    private List<String> status;

    @SerializedName("roles")
    private List<String> roles;

    @SerializedName("role")
    private String role;

    /**
     * Default no-argument constructor for Gson serialization.
     * Initializes status and roles with both DRIVER and OWNER.
     */
    public RegisterRequest() {
        this.status = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        this.roles = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        this.role = "DRIVER";
    }

    /**
     * Parameterized constructor for RegisterRequest with automatic dual status (DRIVER and OWNER).
     *
     * @param email       User email address
     * @param password    User plain text password (8-72 characters)
     * @param firstName   User first name
     * @param lastName    User last name
     * @param phoneNumber User phone number
     */
    public RegisterRequest(String email, String password, String firstName,
                           String lastName, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.status = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        this.roles = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        this.role = "DRIVER";
    }

    /**
     * Parameterized constructor accepting custom statuses list.
     *
     * @param email       User email address
     * @param password    User plain text password (8-72 characters)
     * @param firstName   User first name
     * @param lastName    User last name
     * @param phoneNumber User phone number
     * @param status      List of user statuses/roles (e.g. ["DRIVER", "OWNER"])
     */
    public RegisterRequest(String email, String password, String firstName,
                           String lastName, String phoneNumber, List<String> status) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.status = status != null ? new ArrayList<>(status) : new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        this.roles = new ArrayList<>(this.status);
        this.role = (!this.status.isEmpty()) ? this.status.get(0) : "DRIVER";
    }

    /**
     * Parameterized constructor for backward compatibility with a single role.
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
        this.status = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        this.roles = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
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
     * Gets the user status array containing assigned statuses.
     *
     * @return List of status strings
     */
    public List<String> getStatus() {
        if (status == null) {
            status = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        }
        return status;
    }

    /**
     * Sets the user status array.
     *
     * @param status List of status strings
     */
    public void setStatus(List<String> status) {
        this.status = status;
        if (status != null) {
            this.roles = new ArrayList<>(status);
        }
    }

    /**
     * Gets the user roles array.
     *
     * @return List of role strings
     */
    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<>(Arrays.asList("DRIVER", "OWNER"));
        }
        return roles;
    }

    /**
     * Sets the user roles array.
     *
     * @param roles List of role strings
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    /**
     * Gets the primary user role.
     *
     * @return Role string (e.g. "DRIVER", "OWNER")
     */
    public String getRole() {
        return role != null ? role : "DRIVER";
    }

    /**
     * Sets the primary user role.
     *
     * @param role Role string
     */
    public void setRole(String role) {
        this.role = role;
    }
}
