package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing a blocked period in the calendar
 * returned by POST /api/spots/{spotId}/calendar/blocks and POST /api/spots/{spotId}/blocked-periods.
 */
public class BlockedPeriodResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("start")
    private String start;

    @SerializedName("end")
    private String end;

    @SerializedName("reason")
    private String reason;

    /**
     * Default constructor for Gson deserialization.
     */
    public BlockedPeriodResponse() {
    }

    /**
     * Parameterized constructor.
     *
     * @param id     Unique block identifier
     * @param start  Start ISO-8601 timestamp
     * @param end    End ISO-8601 timestamp
     * @param reason Reason description
     */
    public BlockedPeriodResponse(String id, String start, String end, String reason) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
