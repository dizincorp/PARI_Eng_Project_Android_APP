package com.example.project.network.model;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing the owner's revenue for a specific day.
 */
public class DailyBreakdownItem {

    @SerializedName("day")
    private String day;

    @SerializedName("date")
    private String date;

    @SerializedName("amount")
    private double amount;

    /**
     * Default no-arg constructor for Gson deserialization.
     */
    public DailyBreakdownItem() {
    }

    /**
     * Parameterized constructor for DailyBreakdownItem.
     *
     * @param day    Abbreviated or full day name (e.g., "Pon", "Wt", "Śr")
     * @param date   Calendar date string in YYYY-MM-DD format (e.g., "2026-10-30")
     * @param amount Revenue earned on this date in PLN
     */
    public DailyBreakdownItem(@Nullable String day, @Nullable String date, double amount) {
        this.day = day;
        this.date = date;
        this.amount = amount;
    }

    @Nullable
    public String getDay() {
        return day;
    }

    public void setDay(@Nullable String day) {
        this.day = day;
    }

    @Nullable
    public String getDate() {
        return date;
    }

    public void setDate(@Nullable String date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
