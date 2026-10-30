package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Database entity representing a cached parking spot review.
 * Enforces the Single Source of Truth architecture for offline review viewing.
 */
@Entity(
        tableName = "reviews",
        indices = {
                @Index(value = {"spotId"}),
                @Index(value = {"createdAtTimestamp"})
        }
)
public class ReviewEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "spotId")
    private String spotId;

    @ColumnInfo(name = "reservationId")
    private String reservationId;

    @ColumnInfo(name = "authorName")
    private String authorName;

    @ColumnInfo(name = "rating")
    private int rating;

    @ColumnInfo(name = "comment")
    private String comment;

    @ColumnInfo(name = "createdAtTimestamp")
    private long createdAtTimestamp;

    /**
     * Default no-arg constructor required by Room.
     */
    public ReviewEntity() {
        this.id = "";
    }

    /**
     * Parameterized constructor for ReviewEntity.
     *
     * @param id                 Unique review ID
     * @param spotId             Associated parking spot ID
     * @param reservationId      Associated reservation ID
     * @param authorName         Author display name
     * @param rating             Review rating (1-5)
     * @param comment            Review comment text
     * @param createdAtTimestamp Epoch millisecond timestamp of review creation
     */
    @Ignore
    public ReviewEntity(@NonNull String id, @Nullable String spotId, @Nullable String reservationId,
                        @Nullable String authorName, int rating, @Nullable String comment,
                        long createdAtTimestamp) {
        this.id = id;
        this.spotId = spotId;
        this.reservationId = reservationId;
        this.authorName = authorName != null ? authorName : "Kierowca";
        this.rating = rating;
        this.comment = comment;
        this.createdAtTimestamp = createdAtTimestamp;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @Nullable
    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(@Nullable String spotId) {
        this.spotId = spotId;
    }

    @Nullable
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(@Nullable String reservationId) {
        this.reservationId = reservationId;
    }

    public String getAuthorName() {
        return authorName != null ? authorName : "Kierowca";
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment != null ? comment : "";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }

    public void setCreatedAtTimestamp(long createdAtTimestamp) {
        this.createdAtTimestamp = createdAtTimestamp;
    }
}
