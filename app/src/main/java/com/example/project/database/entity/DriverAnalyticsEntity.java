package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.project.network.ApiClient;
import com.example.project.network.model.DriverAnalyticsItem;
import com.example.project.network.model.DriverAnalyticsSummary;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Room Database entity representing cached driver analytics metrics, lifetime spending,
 * and recent completed parking sessions.
 * Enforces the Single Source of Truth (SSOT) architecture.
 */
@Entity(tableName = "driver_analytics")
public class DriverAnalyticsEntity {

    public static final String SINGLETON_ID = "CURRENT_DRIVER_ANALYTICS";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "totalSpent")
    private double totalSpent;

    @ColumnInfo(name = "totalSaved")
    private double totalSaved;

    @ColumnInfo(name = "totalBookingsCount")
    private int totalBookingsCount;

    @ColumnInfo(name = "activeBookingsCount")
    private int activeBookingsCount;

    @ColumnInfo(name = "loyaltyPoints")
    private int loyaltyPoints;

    @ColumnInfo(name = "currency")
    private String currency;

    @ColumnInfo(name = "summaryJson")
    private String summaryJson;

    @ColumnInfo(name = "itemsJson")
    private String itemsJson;

    @ColumnInfo(name = "lastUpdatedTimestamp")
    private long lastUpdatedTimestamp;

    public DriverAnalyticsEntity() {
        this.id = SINGLETON_ID;
        this.currency = "PLN";
        this.summaryJson = "{}";
        this.itemsJson = "[]";
    }

    @Ignore
    public DriverAnalyticsEntity(@NonNull String id, double totalSpent, double totalSaved,
                                 int totalBookingsCount, int activeBookingsCount,
                                 int loyaltyPoints, @Nullable String currency,
                                 @Nullable String summaryJson, @Nullable String itemsJson,
                                 long lastUpdatedTimestamp) {
        this.id = id;
        this.totalSpent = totalSpent;
        this.totalSaved = totalSaved;
        this.totalBookingsCount = totalBookingsCount;
        this.activeBookingsCount = activeBookingsCount;
        this.loyaltyPoints = loyaltyPoints;
        this.currency = currency != null ? currency : "PLN";
        this.summaryJson = summaryJson != null ? summaryJson : "{}";
        this.itemsJson = itemsJson != null ? itemsJson : "[]";
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @NonNull
    public DriverAnalyticsSummary getSummaryObject() {
        if (summaryJson == null || summaryJson.trim().isEmpty()) {
            return new DriverAnalyticsSummary();
        }
        try {
            DriverAnalyticsSummary s = ApiClient.getGson().fromJson(summaryJson, DriverAnalyticsSummary.class);
            return s != null ? s : new DriverAnalyticsSummary();
        } catch (Exception e) {
            return new DriverAnalyticsSummary();
        }
    }

    @NonNull
    public List<DriverAnalyticsItem> getItemsList() {
        if (itemsJson == null || itemsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<DriverAnalyticsItem>>() {}.getType();
            List<DriverAnalyticsItem> list = ApiClient.getGson().fromJson(itemsJson, listType);
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

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public double getTotalSaved() {
        return totalSaved;
    }

    public void setTotalSaved(double totalSaved) {
        this.totalSaved = totalSaved;
    }

    public int getTotalBookingsCount() {
        return totalBookingsCount;
    }

    public void setTotalBookingsCount(int totalBookingsCount) {
        this.totalBookingsCount = totalBookingsCount;
    }

    public int getActiveBookingsCount() {
        return activeBookingsCount;
    }

    public void setActiveBookingsCount(int activeBookingsCount) {
        this.activeBookingsCount = activeBookingsCount;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSummaryJson() {
        return summaryJson != null ? summaryJson : "{}";
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public String getItemsJson() {
        return itemsJson != null ? itemsJson : "[]";
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}
