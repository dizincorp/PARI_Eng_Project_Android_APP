package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response payload delivering aggregated driver parking statistics, historical spending,
 * and paginated session items from GET /api/driver/analytics.
 */
public class DriverAnalyticsResponse {

    @SerializedName("totalSpent")
    private Double totalSpent;

    @SerializedName("totalSaved")
    private Double totalSaved;

    @SerializedName("totalBookingsCount")
    private Integer totalBookingsCount;

    @SerializedName("activeBookingsCount")
    private Integer activeBookingsCount;

    @SerializedName("loyaltyPoints")
    private Integer loyaltyPoints;

    @SerializedName("currency")
    private String currency;

    @SerializedName("summary")
    private DriverAnalyticsSummary summary;

    @SerializedName("items")
    private List<DriverAnalyticsItem> items;

    @SerializedName("page")
    private Integer page;

    @SerializedName("size")
    private Integer size;

    @SerializedName("totalItems")
    private Long totalItems;

    public DriverAnalyticsResponse() {
        this.totalSpent = 0.0;
        this.totalSaved = 0.0;
        this.totalBookingsCount = 0;
        this.activeBookingsCount = 0;
        this.loyaltyPoints = 0;
        this.currency = "PLN";
        this.summary = new DriverAnalyticsSummary();
        this.items = new ArrayList<>();
        this.page = 0;
        this.size = 20;
        this.totalItems = 0L;
    }

    public DriverAnalyticsResponse(Double totalSpent, Double totalSaved, Integer totalBookingsCount,
                                   Integer activeBookingsCount, Integer loyaltyPoints, String currency,
                                   DriverAnalyticsSummary summary, List<DriverAnalyticsItem> items,
                                   Integer page, Integer size, Long totalItems) {
        this.totalSpent = totalSpent != null ? totalSpent : 0.0;
        this.totalSaved = totalSaved != null ? totalSaved : 0.0;
        this.totalBookingsCount = totalBookingsCount != null ? totalBookingsCount : 0;
        this.activeBookingsCount = activeBookingsCount != null ? activeBookingsCount : 0;
        this.loyaltyPoints = loyaltyPoints != null ? loyaltyPoints : 0;
        this.currency = currency != null ? currency : "PLN";
        this.summary = summary != null ? summary : new DriverAnalyticsSummary();
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 20;
        this.totalItems = totalItems != null ? totalItems : 0L;
    }

    public double getTotalSpent() {
        return totalSpent != null ? totalSpent : 0.0;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public double getTotalSaved() {
        return totalSaved != null ? totalSaved : 0.0;
    }

    public void setTotalSaved(Double totalSaved) {
        this.totalSaved = totalSaved;
    }

    public int getTotalBookingsCount() {
        return totalBookingsCount != null ? totalBookingsCount : 0;
    }

    public void setTotalBookingsCount(Integer totalBookingsCount) {
        this.totalBookingsCount = totalBookingsCount;
    }

    public int getActiveBookingsCount() {
        return activeBookingsCount != null ? activeBookingsCount : 0;
    }

    public void setActiveBookingsCount(Integer activeBookingsCount) {
        this.activeBookingsCount = activeBookingsCount;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints != null ? loyaltyPoints : 0;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public DriverAnalyticsSummary getSummary() {
        return summary != null ? summary : new DriverAnalyticsSummary();
    }

    public void setSummary(DriverAnalyticsSummary summary) {
        this.summary = summary;
    }

    public List<DriverAnalyticsItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public void setItems(List<DriverAnalyticsItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public int getPage() {
        return page != null ? page : 0;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public int getSize() {
        return size != null ? size : 20;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public long getTotalItems() {
        return totalItems != null ? totalItems : 0L;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }
}
