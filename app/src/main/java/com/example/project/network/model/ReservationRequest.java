package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing the JSON request payload for creating
 * a parking reservation via POST /api/reservations.
 */
public class ReservationRequest {

    @SerializedName("spotId")
    private String spotId;

    @SerializedName("vehicleId")
    private String vehicleId;

    @SerializedName("start")
    private String start;

    @SerializedName("end")
    private String end;

    @SerializedName("paymentMethodToken")
    private String paymentMethodToken;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public ReservationRequest() {
        this.paymentMethodToken = "pm_card_visa";
    }

    /**
     * Parameterized constructor for ReservationRequest.
     *
     * @param spotId             Target parking spot UUID
     * @param vehicleId          Driver's vehicle UUID
     * @param start              Start timestamp in ISO-8601 UTC Instant format (e.g. 2026-10-24T15:50:44.285Z)
     * @param end                End timestamp in ISO-8601 UTC Instant format (e.g. 2026-10-24T18:50:44.285Z)
     * @param paymentMethodToken Payment method token (e.g. "pm_card_visa")
     */
    public ReservationRequest(String spotId, String vehicleId, String start, String end, String paymentMethodToken) {
        this.spotId = spotId;
        this.vehicleId = vehicleId;
        this.start = start;
        this.end = end;
        this.paymentMethodToken = (paymentMethodToken != null && !paymentMethodToken.trim().isEmpty())
                ? paymentMethodToken.trim() : "pm_card_visa";
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
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

    public String getPaymentMethodToken() {
        return paymentMethodToken;
    }

    public void setPaymentMethodToken(String paymentMethodToken) {
        this.paymentMethodToken = paymentMethodToken;
    }
}
