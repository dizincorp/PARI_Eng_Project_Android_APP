package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Summary metrics of completed reservations for driver analytics.
 */
public class DriverAnalyticsSummary {

    @SerializedName("completedReservations")
    private Integer completedReservations;

    @SerializedName("totalSpent")
    private Double totalSpent;

    @SerializedName("totalParkingDurationMinutes")
    private Long totalParkingDurationMinutes;

    @SerializedName("averageRating")
    private Double averageRating;

    @SerializedName("reviewedReservations")
    private Integer reviewedReservations;

    public DriverAnalyticsSummary() {
        this.completedReservations = 0;
        this.totalSpent = 0.0;
        this.totalParkingDurationMinutes = 0L;
        this.averageRating = 0.0;
        this.reviewedReservations = 0;
    }

    public DriverAnalyticsSummary(Integer completedReservations, Double totalSpent,
                                  Long totalParkingDurationMinutes, Double averageRating,
                                  Integer reviewedReservations) {
        this.completedReservations = completedReservations != null ? completedReservations : 0;
        this.totalSpent = totalSpent != null ? totalSpent : 0.0;
        this.totalParkingDurationMinutes = totalParkingDurationMinutes != null ? totalParkingDurationMinutes : 0L;
        this.averageRating = averageRating != null ? averageRating : 0.0;
        this.reviewedReservations = reviewedReservations != null ? reviewedReservations : 0;
    }

    public int getCompletedReservations() {
        return completedReservations != null ? completedReservations : 0;
    }

    public void setCompletedReservations(Integer completedReservations) {
        this.completedReservations = completedReservations;
    }

    public double getTotalSpent() {
        return totalSpent != null ? totalSpent : 0.0;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public long getTotalParkingDurationMinutes() {
        return totalParkingDurationMinutes != null ? totalParkingDurationMinutes : 0L;
    }

    public void setTotalParkingDurationMinutes(Long totalParkingDurationMinutes) {
        this.totalParkingDurationMinutes = totalParkingDurationMinutes;
    }

    public double getAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewedReservations() {
        return reviewedReservations != null ? reviewedReservations : 0;
    }

    public void setReviewedReservations(Integer reviewedReservations) {
        this.reviewedReservations = reviewedReservations;
    }
}
