package com.example.project.network.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing aggregated financial and operational analytics
 * for an owner's parking locations (GET /api/owner/analytics).
 */
public class OwnerAnalyticsResponse {

    @SerializedName("totalEarnings")
    private double totalEarnings;

    @SerializedName("grossEarnings")
    private double grossEarnings;

    @SerializedName("earningsChangePercent")
    private double earningsChangePercent;

    @SerializedName("occupancyRatePercent")
    private double occupancyRatePercent;

    @SerializedName("activeSpotsCount")
    private int activeSpotsCount;

    @SerializedName("totalSpotsCount")
    private int totalSpotsCount;

    @SerializedName("averageHourlyRate")
    private double averageHourlyRate;

    @SerializedName("currency")
    private String currency;

    @SerializedName("dailyBreakdown")
    private List<DailyBreakdownItem> dailyBreakdown;

    /**
     * Default no-arg constructor for Gson deserialization.
     */
    public OwnerAnalyticsResponse() {
        this.dailyBreakdown = new ArrayList<>();
        this.currency = "PLN";
    }

    /**
     * Parameterized constructor for OwnerAnalyticsResponse.
     *
     * @param totalEarnings         Total net revenue earned by the owner
     * @param grossEarnings         Total gross revenue before system commission
     * @param earningsChangePercent Percentage difference compared to previous period (+/-)
     * @param occupancyRatePercent  Average occupancy rate (0-100)
     * @param activeSpotsCount      Count of currently active parking spots
     * @param totalSpotsCount       Count of all spots owned
     * @param averageHourlyRate     Average hourly booking price across spots
     * @param currency              Currency code (e.g., "PLN")
     * @param dailyBreakdown        Daily revenue breakdown list
     */
    public OwnerAnalyticsResponse(double totalEarnings, double grossEarnings,
                                  double earningsChangePercent, double occupancyRatePercent,
                                  int activeSpotsCount, int totalSpotsCount,
                                  double averageHourlyRate, @Nullable String currency,
                                  @Nullable List<DailyBreakdownItem> dailyBreakdown) {
        this.totalEarnings = totalEarnings;
        this.grossEarnings = grossEarnings;
        this.earningsChangePercent = earningsChangePercent;
        this.occupancyRatePercent = occupancyRatePercent;
        this.activeSpotsCount = activeSpotsCount;
        this.totalSpotsCount = totalSpotsCount;
        this.averageHourlyRate = averageHourlyRate;
        this.currency = currency != null ? currency : "PLN";
        this.dailyBreakdown = dailyBreakdown != null ? dailyBreakdown : new ArrayList<>();
    }

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public double getGrossEarnings() {
        return grossEarnings;
    }

    public void setGrossEarnings(double grossEarnings) {
        this.grossEarnings = grossEarnings;
    }

    public double getEarningsChangePercent() {
        return earningsChangePercent;
    }

    public void setEarningsChangePercent(double earningsChangePercent) {
        this.earningsChangePercent = earningsChangePercent;
    }

    public double getOccupancyRatePercent() {
        return occupancyRatePercent;
    }

    public void setOccupancyRatePercent(double occupancyRatePercent) {
        this.occupancyRatePercent = occupancyRatePercent;
    }

    public int getActiveSpotsCount() {
        return activeSpotsCount;
    }

    public void setActiveSpotsCount(int activeSpotsCount) {
        this.activeSpotsCount = activeSpotsCount;
    }

    public int getTotalSpotsCount() {
        return totalSpotsCount;
    }

    public void setTotalSpotsCount(int totalSpotsCount) {
        this.totalSpotsCount = totalSpotsCount;
    }

    public double getAverageHourlyRate() {
        return averageHourlyRate;
    }

    public void setAverageHourlyRate(double averageHourlyRate) {
        this.averageHourlyRate = averageHourlyRate;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public List<DailyBreakdownItem> getDailyBreakdown() {
        return dailyBreakdown != null ? dailyBreakdown : new ArrayList<>();
    }

    public void setDailyBreakdown(@Nullable List<DailyBreakdownItem> dailyBreakdown) {
        this.dailyBreakdown = dailyBreakdown != null ? dailyBreakdown : new ArrayList<>();
    }
}
