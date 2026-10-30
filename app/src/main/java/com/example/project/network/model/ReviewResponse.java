package com.example.project.network.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object representing an individual parking spot review submitted by a driver.
 */
public class ReviewResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("rating")
    private int rating;

    @SerializedName("comment")
    private String comment;

    @SerializedName("createdAt")
    private Date createdAt;

    /**
     * Default constructor for serialization frameworks.
     */
    public ReviewResponse() {
    }

    /**
     * Parameterized constructor for ReviewResponse.
     *
     * @param id         Unique review identifier
     * @param authorName Formatted display name of review author (e.g. "Jan K.")
     * @param rating     Numerical rating from 1 to 5
     * @param comment    Optional comment text
     * @param createdAt  Timestamp when the review was submitted
     */
    public ReviewResponse(String id, String authorName, int rating,
                          @Nullable String comment, Date createdAt) {
        this.id = id;
        this.authorName = authorName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    /**
     * Gets the unique review identifier.
     *
     * @return Review ID string
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique review identifier.
     *
     * @param id Review ID string
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the author's public display name.
     *
     * @return Author display name
     */
    public String getAuthorName() {
        return authorName != null ? authorName : "Kierowca";
    }

    /**
     * Sets the author's public display name.
     *
     * @param authorName Author display name
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * Gets the rating score (1-5).
     *
     * @return Integer rating
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets the rating score (1-5).
     *
     * @param rating Integer rating
     */
    public void setRating(int rating) {
        this.rating = rating;
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
     * Sets the optional review comment.
     *
     * @param comment Comment text
     */
    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return Creation date
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt Creation date
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
