package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a calendar blocked period for a parking spot.
 */
@Entity(tableName = "blocked_periods", indices = {@Index("spotId")})
public class BlockedPeriodEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String spotId;

    private String start; // ISO-8601 string
    private String end;   // ISO-8601 string
    private String reason;

    /**
     * Default no-arg constructor required by Room.
     */
    public BlockedPeriodEntity() {
        this.id = "";
        this.spotId = "";
        this.start = "";
        this.end = "";
        this.reason = "";
    }

    /**
     * Parameterized constructor.
     *
     * @param id     Unique block identifier
     * @param spotId Spot identifier
     * @param start  Start ISO-8601 timestamp
     * @param end    End ISO-8601 timestamp
     * @param reason Reason description
     */
    @Ignore
    public BlockedPeriodEntity(@NonNull String id, @NonNull String spotId, String start, String end, String reason) {
        this.id = id;
        this.spotId = spotId;
        this.start = start != null ? start : "";
        this.end = end != null ? end : "";
        this.reason = reason != null ? reason : "";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(@NonNull String spotId) {
        this.spotId = spotId;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
