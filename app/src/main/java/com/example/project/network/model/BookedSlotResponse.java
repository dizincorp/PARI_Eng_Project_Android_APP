package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing an occupied slot (reservation or owner block)
 * returned by GET /api/spots/{spotId}/booked-slots?from={fromIso}&amp;to={toIso}.
 */
public class BookedSlotResponse {

    @SerializedName("reservationId")
    private String reservationId;

    @SerializedName("blockId")
    private String blockId;

    @SerializedName("start")
    private String start;

    @SerializedName("end")
    private String end;

    @SerializedName("status")
    private String status;

    public BookedSlotResponse() {
    }

    public BookedSlotResponse(String reservationId, String blockId, String start, String end, String status) {
        this.reservationId = reservationId;
        this.blockId = blockId;
        this.start = start;
        this.end = end;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBlocked() {
        return "BLOCKED".equalsIgnoreCase(status) || (blockId != null && !blockId.isEmpty());
    }
}
