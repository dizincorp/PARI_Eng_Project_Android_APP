package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object (DTO) representing the JSON request payload for calculating
 * a preliminary parking reservation quote via POST /api/reservations/quote.
 */
public class QuoteRequest {

    @SerializedName("spotId")
    private String spotId;

    @SerializedName("startsAt")
    private Date startsAt;

    @SerializedName("endsAt")
    private Date endsAt;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public QuoteRequest() {
    }

    /**
     * Parameterized constructor using {@link Date} for ISO-8601 temporal fields.
     *
     * @param spotId   Target parking spot UUID
     * @param startsAt Reservation start timestamp
     * @param endsAt   Reservation end timestamp
     */
    public QuoteRequest(String spotId, Date startsAt, Date endsAt) {
        this.spotId = spotId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * Overloaded constructor accepting epoch millisecond timestamps.
     *
     * @param spotId  Target parking spot UUID
     * @param startMs Start timestamp in milliseconds
     * @param endMs   End timestamp in milliseconds
     */
    public QuoteRequest(String spotId, long startMs, long endMs) {
        this.spotId = spotId;
        this.startsAt = new Date(startMs);
        this.endsAt = new Date(endMs);
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public Date getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Date startsAt) {
        this.startsAt = startsAt;
    }

    public Date getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Date endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * Convenience getter for start time in milliseconds.
     *
     * @return Start timestamp in epoch milliseconds, or 0 if null
     */
    public long getStartsAtMillis() {
        return startsAt != null ? startsAt.getTime() : 0L;
    }

    /**
     * Convenience getter for end time in milliseconds.
     *
     * @return End timestamp in epoch milliseconds, or 0 if null
     */
    public long getEndsAtMillis() {
        return endsAt != null ? endsAt.getTime() : 0L;
    }
}
