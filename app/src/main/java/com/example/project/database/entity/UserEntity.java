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
    private boolean isPremium;
    private double rating;
    private int ratingCount;
    private int avatarResId;

    public UserEntity() {
        this.email = "jan_kowalski@gmail.com";
        this.name = "Jan Kowalski";
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