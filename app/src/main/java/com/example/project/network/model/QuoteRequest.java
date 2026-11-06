package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object (DTO) representing the JSON request payload for calculating
 * a preliminary parking reservation quote via POST /api/reservations/quote.
 */
public class QuoteRequest {

    @SerializedName(value = "spotId", alternate = {"id"})
    private String spotId;

    @SerializedName(value = "start", alternate = {"startsAt"})
    private Date startsAt;

    @SerializedName(value = "end", alternate = {"endsAt"})
    private Date endsAt;

    @SerializedName("bookingType")
    private String bookingType; // "HOURLY", "DAILY", "MONTHLY"

    @SerializedName("durationMonths")
    private Integer durationMonths;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public QuoteRequest() {
        this.bookingType = "HOURLY";
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
        this.bookingType = "HOURLY";
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
        this.bookingType = "HOURLY";
    }

    public QuoteRequest(String spotId, long startMs, long endMs, String bookingType, Integer durationMonths) {
        this.spotId = spotId;
        this.startsAt = new Date(startMs);
        this.endsAt = new Date(endMs);
        this.bookingType = (bookingType != null && !bookingType.trim().isEmpty()) ? bookingType.trim() : "HOURLY";
        this.durationMonths = durationMonths;
    }

    public String getBookingType() {
        return bookingType != null ? bookingType : "HOURLY";
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
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
