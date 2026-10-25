package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request DTO model for checking in and beginning parking session via POST /api/reservations/{id}/check-in.
 */
public class CheckInReservationRequest {

    @SerializedName("pinCode")
    private String pinCode;

    @SerializedName("qrToken")
    private String qrToken;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public CheckInReservationRequest() {
    }

    /**
     * Parameterized constructor initializing check-in credentials.
     *
     * @param pinCode Spot access PIN code
     * @param qrToken QR token for scanner or gate validation
     */
    public CheckInReservationRequest(String pinCode, String qrToken) {
        this.pinCode = pinCode;
        this.qrToken = qrToken;
    }

    /**
     * Gets the spot PIN code.
     *
     * @return PIN code string
     */
    public String getPinCode() {
        return pinCode;
    }

    /**
     * Sets the spot PIN code.
     *
     * @param pinCode PIN code string
     */
    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    /**
     * Gets the QR token.
     *
     * @return QR token string
     */
    public String getQrToken() {
        return qrToken;
    }

    /**
     * Sets the QR token.
     *
     * @param qrToken QR token string
     */
    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}
