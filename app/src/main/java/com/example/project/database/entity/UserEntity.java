package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a user profile in the database.
 */
@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String email;
    private String name;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String avatarUri;
    private boolean isPremium;
    private double rating;
    private int ratingCount;
    private int avatarResId;

    public UserEntity() {
        this.email = "jan_kowalski@gmail.com";
        this.name = "Jan Kowalski";
        this.firstName = "Jan";
        this.lastName = "Kowalski";
        this.phone = "+48 500 123 456";
        this.role = "DRIVER";
        this.avatarUri = "";
        this.isPremium = true;
        this.rating = 4.9;
        this.ratingCount = 12;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUri() {
        return avatarUri;
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

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }
}
