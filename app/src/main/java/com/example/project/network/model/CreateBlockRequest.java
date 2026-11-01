package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for creating calendar blocked periods via POST /api/spots/{spotId}/calendar/blocks
 * or POST /api/spots/{spotId}/blocked-periods.
 */
public class CreateBlockRequest {

    @SerializedName("start")
    private String start;

    @SerializedName("end")
    private String end;

    @SerializedName("reason")
    private String reason;

    /**
     * Default constructor for Gson deserialization.
     */
    public CreateBlockRequest() {
    }

    /**
     * Parameterized constructor.
     *
     * @param start  Start ISO-8601 UTC timestamp (e.g. 2026-11-01T15:10:27Z)
     * @param end    End ISO-8601 UTC timestamp
     * @param reason Optional reason description for blocking
     */
    public CreateBlockRequest(String start, String end, String reason) {
        this.start = start;
        this.end = end;
        this.reason = reason;
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
