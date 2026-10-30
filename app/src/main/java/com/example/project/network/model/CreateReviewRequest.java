package com.example.project.network.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for submitting a driver review for a completed parking reservation.
 * Encapsulates rating (integer from 1 to 5) and an optional comment (up to 2000 characters).
 */
public class CreateReviewRequest {

    @SerializedName("rating")
    private int rating;

    @SerializedName("comment")
    private String comment;

    /**
     * Default constructor for serialization frameworks.
     */
    public CreateReviewRequest() {
    }

    /**
     * Parameterized constructor initializing review parameters with bounds validation.
     *
     * @param rating  Numerical rating from 1 to 5 stars
     * @param comment Optional text comment describing parking experience
     */
    public CreateReviewRequest(int rating, @Nullable String comment) {
        this.rating = Math.max(1, Math.min(5, rating));
        this.comment = comment != null ? comment.trim() : null;
    }

    /**
     * Gets the submitted review rating (1-5).
     *
     * @return Integer rating between 1 and 5
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets the numerical rating between 1 and 5.
     *
     * @param rating Integer rating between 1 and 5
     */
    public void setRating(int rating) {
        this.rating = Math.max(1, Math.min(5, rating));
    }

    /**
     * Gets the optional review comment.
     *
     * @return Comment text or null
     */
    @Nullable
    public String getComment() {
        return comment;
    }

    /**
     * Sets the optional review comment text.
     *
     * @param comment Comment text
     */
    public void setComment(@Nullable String comment) {
        this.comment = comment != null ? comment.trim() : null;
    }
}
