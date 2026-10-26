package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request DTO model for rejecting a pending reservation request by the parking spot owner
 * via POST /api/reservations/{id}/reject.
 */
public class RejectReservationRequest {

    @SerializedName("reason")
    private String reason;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public RejectReservationRequest() {
        this.reason = "";
    }

    /**
     * Parameterized constructor initializing rejection explanation reason.
     *
     * @param reason Explanation for reservation rejection
     */
    public RejectReservationRequest(String reason) {
        this.reason = reason;
    }

    /**
     * Gets the rejection reason.
     *
     * @return Rejection reason string
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the rejection reason.
     *
     * @param reason Rejection reason string
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
