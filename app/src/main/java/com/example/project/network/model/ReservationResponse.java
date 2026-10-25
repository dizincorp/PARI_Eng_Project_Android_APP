package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object (DTO) representing the JSON response body returned by POST /api/reservations.
 * Uses {@link Date} for all ISO-8601 temporal fields to support seamless parsing via Gson.
 */
public class ReservationResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("code")
    private String code;

    @SerializedName("spotId")
    private String spotId;

    @SerializedName("spotName")
    private String spotName;

    @SerializedName("driverId")
    private String driverId;

    @SerializedName("vehicleId")
    private String vehicleId;

    @SerializedName("vehiclePlate")
    private String vehiclePlate;

    @SerializedName("startsAt")
    private Date startsAt;

    @SerializedName("endsAt")
    private Date endsAt;

    @SerializedName("status")
    private String status;

    @SerializedName("hourlyRate")
    private Double hourlyRate;

    @SerializedName("spotCost")
    private Double spotCost;

    @SerializedName("serviceFee")
    private Double serviceFee;

    @SerializedName("total")
    private Double total;

    @SerializedName("currency")
    private String currency;

    @SerializedName("pinCode")
    private String pinCode;

    @SerializedName("qrToken")
    private String qrToken;

    @SerializedName("cancelReason")
    private String cancelReason;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("checkInAt")
    private Date checkInAt;

    @SerializedName("checkOutAt")
    private Date checkOutAt;

    @SerializedName("overstayPenalty")
    private Double overstayPenalty;

    @SerializedName("finalCost")
    private Double finalCost;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public ReservationResponse() {
        this.status = "CONFIRMED";
        this.currency = "PLN";
        this.total = 0.0;
        this.hourlyRate = 0.0;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSpotId() {
        return spotId != null ? spotId : "";
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getSpotName() {
        return spotName != null ? spotName : "";
    }

    public void setSpotName(String spotName) {
        this.spotName = spotName;
    }

    public String getDriverId() {
        return driverId != null ? driverId : "";
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getVehicleId() {
        return vehicleId != null ? vehicleId : "";
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehiclePlate() {
        return vehiclePlate != null ? vehiclePlate : "";
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public Date getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Date startsAt) {
        this.startsAt = startsAt;
    }

    public Date getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Date endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * Convenience method returning startsAt timestamp in milliseconds.
     *
     * @return Milliseconds since epoch, or 0 if startsAt is null
     */
    public long getStartsAtMillis() {
        return startsAt != null ? startsAt.getTime() : 0L;
    }

    /**
     * Convenience method returning endsAt timestamp in milliseconds.
     *
     * @return Milliseconds since epoch, or 0 if endsAt is null
     */
    public long getEndsAtMillis() {
        return endsAt != null ? endsAt.getTime() : 0L;
    }

    public String getStatus() {
        return status != null ? status : "";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getHourlyRate() {
        return hourlyRate != null ? hourlyRate : 0.0;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getSpotCost() {
        return spotCost != null ? spotCost : 0.0;
    }

    public void setSpotCost(Double spotCost) {
        this.spotCost = spotCost;
    }

    public double getServiceFee() {
        return serviceFee != null ? serviceFee : 0.0;
    }

    public void setServiceFee(Double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public double getTotal() {
        return total != null ? total : 0.0;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPinCode() {
        return pinCode != null ? pinCode : "";
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getQrToken() {
        return qrToken != null ? qrToken : "";
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(Date checkInAt) {
        this.checkInAt = checkInAt;
    }

    public Date getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(Date checkOutAt) {
        this.checkOutAt = checkOutAt;
    }

    public double getOverstayPenalty() {
        return overstayPenalty != null ? overstayPenalty : 0.0;
    }

    public void setOverstayPenalty(Double overstayPenalty) {
        this.overstayPenalty = overstayPenalty;
    }

    public double getFinalCost() {
        return finalCost != null ? finalCost : 0.0;
    }

    public void setFinalCost(Double finalCost) {
        this.finalCost = finalCost;
    }
}
