package com.example.project.model;

import com.example.project.R;

/**
 * Model class representing user profile details.
 */
public class UserProfile {

    private String name;
    private String email;
    private boolean isPremium;
    private double rating;
    private int avatarResId;

    /**
     * Default constructor for UserProfile.
     */
    public UserProfile() {
        this.name = "Jan Kowalski";
        this.email = "jan_kowalski@gmail.com";
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
        this.name = name;
        this.email = email;
        this.isPremium = isPremium;
        this.rating = rating;
        this.avatarResId = avatarResId != 0 ? avatarResId : R.drawable.ic_avatar;
    }

    public String getName() {
        return name != null ? name : "Jan Kowalski";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email : "jan_kowalski@gmail.com";
    }

    public void setEmail(String email) {
        this.email = email;
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