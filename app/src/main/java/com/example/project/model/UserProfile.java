package com.example.project.model;

import com.example.project.R;

/**
 * Model class representing user profile details.
 */
public class UserProfile {

    private String name;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String role;
    private String avatarUri;
    private boolean isPremium;
    private double rating;
    private int avatarResId;

    /**
     * Default constructor for UserProfile.
     */
    public UserProfile() {
        this.name = "Jan Kowalski";
        this.firstName = "Jan";
        this.lastName = "Kowalski";
        this.phone = "+48 500 123 456";
        this.email = "jan_kowalski@gmail.com";
        this.role = "DRIVER";
        this.avatarUri = "";
        this.isPremium = true;
        this.rating = 4.9;
        this.avatarResId = R.drawable.ic_avatar;
    }

    /**
     * Parameterized constructor for UserProfile.
     *
     * @param name        User name
     * @param email       User email
     * @param isPremium   Premium membership status
     * @param rating      User rating
     * @param avatarResId Avatar image resource ID
     */
    public UserProfile(String name, String email, boolean isPremium, double rating, int avatarResId) {
        this();
        this.name = name;
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts[1];
        } else {
            this.firstName = name;
            this.lastName = "";
        }
        this.email = email;
        this.isPremium = isPremium;
        this.rating = rating;
        this.avatarResId = avatarResId != 0 ? avatarResId : R.drawable.ic_avatar;
    }

    public String getName() {
        if (name != null && !name.isEmpty()) return name;
        if (firstName != null && !firstName.isEmpty()) {
            return (firstName + " " + (lastName != null ? lastName : "")).trim();
        }
        return "Jan Kowalski";
    }

    public void setName(String name) {
        this.name = name;
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts[1];
        } else {
            this.firstName = name;
        }
    }

    public String getFirstName() {
        return firstName != null ? firstName : "Jan";
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName != null ? lastName : "Kowalski";
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone != null ? phone : "+48 500 123 456";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email != null ? email : "jan_kowalski@gmail.com";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role != null ? role : "DRIVER";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUri() {
        return avatarUri != null ? avatarUri : "";
    }

    public void setAvatarUri(String avatarUri) {
        this.avatarUri = avatarUri;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getAvatarResId() {
        return avatarResId != 0 ? avatarResId : R.drawable.ic_avatar;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }
}
