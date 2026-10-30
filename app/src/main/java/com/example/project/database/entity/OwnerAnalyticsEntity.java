package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.project.network.ApiClient;
import com.example.project.network.model.DailyBreakdownItem;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Room Database entity representing cached owner analytics and financial breakdown.
 * Serves as the Single Source of Truth for offline dashboard display.
 */
@Entity(tableName = "owner_analytics")
public class OwnerAnalyticsEntity {

    public static final String SINGLETON_ID = "CURRENT_OWNER_ANALYTICS";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "totalEarnings")
    private double totalEarnings;

    @ColumnInfo(name = "grossEarnings")
    private double grossEarnings;

    @ColumnInfo(name = "earningsChangePercent")
    private double earningsChangePercent;

    @ColumnInfo(name = "occupancyRatePercent")
    private double occupancyRatePercent;

    @ColumnInfo(name = "activeSpotsCount")
    private int activeSpotsCount;

    @ColumnInfo(name = "totalSpotsCount")
    private int totalSpotsCount;

    @ColumnInfo(name = "averageHourlyRate")
    private double averageHourlyRate;

    @ColumnInfo(name = "currency")
    private String currency;

    @ColumnInfo(name = "dailyBreakdownJson")
    private String dailyBreakdownJson;

    @ColumnInfo(name = "lastUpdatedTimestamp")
    private long lastUpdatedTimestamp;

    /**
     * Default no-arg constructor required by Room.
     */
    public OwnerAnalyticsEntity() {
        this.id = SINGLETON_ID;
        this.currency = "PLN";
        this.dailyBreakdownJson = "[]";
    }

    /**
     * Parameterized constructor for OwnerAnalyticsEntity.
     *
     * @param id                    Unique singleton identifier
     * @param totalEarnings         Total net revenue
     * @param grossEarnings         Total gross revenue
     * @param earningsChangePercent Percentage difference compared to last period
     * @param occupancyRatePercent  Occupancy rate percentage (0-100)
     * @param activeSpotsCount      Active spots count
     * @param totalSpotsCount       Total spots owned
     * @param averageHourlyRate     Average hourly rate
     * @param currency              Currency ISO code
     * @param dailyBreakdownJson    Serialized JSON array of daily breakdown items
     * @param lastUpdatedTimestamp  Epoch timestamp when the data was saved
     */
    @Ignore
    public OwnerAnalyticsEntity(@NonNull String id, double totalEarnings, double grossEarnings,
                                double earningsChangePercent, double occupancyRatePercent,
                                int activeSpotsCount, int totalSpotsCount,
                                double averageHourlyRate, @Nullable String currency,
                                @Nullable String dailyBreakdownJson, long lastUpdatedTimestamp) {
        this.id = id;
        this.totalEarnings = totalEarnings;
        this.grossEarnings = grossEarnings;
        this.earningsChangePercent = earningsChangePercent;
        this.occupancyRatePercent = occupancyRatePercent;
        this.activeSpotsCount = activeSpotsCount;
        this.totalSpotsCount = totalSpotsCount;
        this.averageHourlyRate = averageHourlyRate;
        this.currency = currency != null ? currency : "PLN";
        this.dailyBreakdownJson = dailyBreakdownJson != null ? dailyBreakdownJson : "[]";
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    /**
     * Helper method to parse the cached JSON array back into typed DailyBreakdownItem objects.
     *
     * @return List of DailyBreakdownItem
     */
    @NonNull
    public List<DailyBreakdownItem> getDailyBreakdownList() {
        if (dailyBreakdownJson == null || dailyBreakdownJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<DailyBreakdownItem>>() {}.getType();
            List<DailyBreakdownItem> list = ApiClient.getGson().fromJson(dailyBreakdownJson, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
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

    public String getDailyBreakdownJson() {
        return dailyBreakdownJson != null ? dailyBreakdownJson : "[]";
    }

    public void setDailyBreakdownJson(@Nullable String dailyBreakdownJson) {
        this.dailyBreakdownJson = dailyBreakdownJson;
    }

    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}
