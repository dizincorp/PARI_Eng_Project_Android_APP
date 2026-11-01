package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for availability rules returned by GET and PUT /api/spots/{spotId}/availability-rules.
 */
public class AvailabilityRuleResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("dayOfWeek")
    private int dayOfWeek;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("validFrom")
    private String validFrom;

    @SerializedName("validTo")
    private String validTo;

    /**
     * Default constructor for Gson deserialization.
     */
    public AvailabilityRuleResponse() {
    }

    /**
     * Parameterized constructor.
     *
     * @param id        Unique rule identifier
     * @param dayOfWeek Day of week (1 = Monday ... 7 = Sunday)
     * @param startTime Start time (HH:mm or HH:mm:ss)
     * @param endTime   End time (HH:mm or HH:mm:ss)
     * @param validFrom Start date boundary (YYYY-MM-DD)
     * @param validTo   End date boundary (YYYY-MM-DD)
     */
    public AvailabilityRuleResponse(String id, int dayOfWeek, String startTime, String endTime, String validFrom, String validTo) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }
}
