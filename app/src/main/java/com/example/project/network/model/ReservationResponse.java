package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing the JSON response body returned by POST /api/reservations.
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
    private String startsAt;

    @SerializedName("endsAt")
    private String endsAt;

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
    private String createdAt;

    @SerializedName("checkInAt")
    private String checkInAt;

    @SerializedName("checkOutAt")
    private String checkOutAt;

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

    public String getStartsAt() {
        return startsAt != null ? startsAt : "";
    }

    public void setStartsAt(String startsAt) {
        this.startsAt = startsAt;
    }

    public String getEndsAt() {
        return endsAt != null ? endsAt : "";
    }

    public void setEndsAt(String endsAt) {
        this.endsAt = endsAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(String checkInAt) {
        this.checkInAt = checkInAt;
    }

    public String getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(String checkOutAt) {
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
