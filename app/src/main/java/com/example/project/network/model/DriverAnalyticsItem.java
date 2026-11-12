package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Individual completed reservation session entry within driver analytics.
 */
public class DriverAnalyticsItem {

    @SerializedName("reservationId")
    private String reservationId;

    @SerializedName("spotName")
    private String spotName;

    @SerializedName("checkInAt")
    private Date checkInAt;

    @SerializedName("checkOutAt")
    private Date checkOutAt;

    @SerializedName("durationMinutes")
    private Long durationMinutes;

    @SerializedName("amountPaid")
    private Double amountPaid;

    @SerializedName("currency")
    private String currency;

    @SerializedName("reviewed")
    private Boolean reviewed;

    @SerializedName("rating")
    private Integer rating;

    public DriverAnalyticsItem() {
        this.currency = "PLN";
        this.durationMinutes = 0L;
        this.amountPaid = 0.0;
        this.reviewed = false;
    }

    public DriverAnalyticsItem(String reservationId, String spotName, Date checkInAt,
                               Date checkOutAt, Long durationMinutes, Double amountPaid,
                               String currency, Boolean reviewed, Integer rating) {
        this.reservationId = reservationId;
        this.spotName = spotName;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.durationMinutes = durationMinutes != null ? durationMinutes : 0L;
        this.amountPaid = amountPaid != null ? amountPaid : 0.0;
        this.currency = currency != null ? currency : "PLN";
        this.reviewed = reviewed != null ? reviewed : false;
        this.rating = rating;
    }

    public String getReservationId() {
        return reservationId != null ? reservationId : "";
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getSpotName() {
        return spotName != null ? spotName : "";
    }

    public void setSpotName(String spotName) {
        this.spotName = spotName;
    }

    public Date getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(Date checkInAt) {
        this.checkInAt = checkInAt;
    }

    public Date getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(Date checkOutAt) {
        this.checkOutAt = checkOutAt;
    }

    public long getDurationMinutes() {
        return durationMinutes != null ? durationMinutes : 0L;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public double getAmountPaid() {
        return amountPaid != null ? amountPaid : 0.0;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isReviewed() {
        return Boolean.TRUE.equals(reviewed);
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
