package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request DTO model for canceling a reservation with an optional reason via POST /api/reservations/{id}/cancel.
 */
public class CancelReservationRequest {

    @SerializedName("reason")
    private String reason;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public CancelReservationRequest() {
    }

    /**
     * Parameterized constructor initializing cancellation reason.
     *
     * @param reason Explanation for reservation cancellation
     */
    public CancelReservationRequest(String reason) {
        this.reason = reason;
    }

    /**
     * Gets the cancellation reason.
     *
     * @return Cancellation reason string
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the cancellation reason.
     *
     * @param reason Cancellation reason string
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
