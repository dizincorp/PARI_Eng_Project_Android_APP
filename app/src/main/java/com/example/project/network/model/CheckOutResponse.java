package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object (DTO) representing the billing and time summary returned
 * upon completing parking session checkout via POST /api/reservations/{id}/check-out.
 */
public class CheckOutResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("code")
    private String code;

    @SerializedName("status")
    private String status;

    @SerializedName("actualCheckoutAt")
    private Date actualCheckoutAt;

    @SerializedName("plannedEndsAt")
    private Date plannedEndsAt;

    @SerializedName("overstayMinutes")
    private Integer overstayMinutes;

    @SerializedName("baseCost")
    private Double baseCost;

    @SerializedName("overstayPenaltyFee")
    private Double overstayPenaltyFee;

    @SerializedName("totalFinalAmount")
    private Double totalFinalAmount;

    @SerializedName("currency")
    private String currency;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public CheckOutResponse() {
        this.status = "COMPLETED";
        this.currency = "PLN";
        this.baseCost = 0.0;
        this.overstayPenaltyFee = 0.0;
        this.totalFinalAmount = 0.0;
        this.overstayMinutes = 0;
    }

    /**
     * Parameterized constructor initializing checkout summary fields.
     *
     * @param id                 Reservation unique identifier
     * @param code               Reservation human-readable code
     * @param status             Resulting status string
     * @param actualCheckoutAt   Actual checkout timestamp
     * @param plannedEndsAt      Original planned reservation end timestamp
     * @param overstayMinutes    Overstay duration in minutes
     * @param baseCost           Standard reservation cost
     * @param overstayPenaltyFee Penalty fee charged for overstay
     * @param totalFinalAmount   Final total amount billed
     * @param currency           Currency denomination code (e.g. "PLN")
     */
    public CheckOutResponse(String id, String code, String status, Date actualCheckoutAt,
                            Date plannedEndsAt, Integer overstayMinutes, Double baseCost,
                            Double overstayPenaltyFee, Double totalFinalAmount, String currency) {
        this.id = id;
        this.code = code;
        this.status = status;
        this.actualCheckoutAt = actualCheckoutAt;
        this.plannedEndsAt = plannedEndsAt;
        this.overstayMinutes = overstayMinutes;
        this.baseCost = baseCost;
        this.overstayPenaltyFee = overstayPenaltyFee;
        this.totalFinalAmount = totalFinalAmount;
        this.currency = currency;
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

    public String getStatus() {
        return status != null ? status : "COMPLETED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getActualCheckoutAt() {
        return actualCheckoutAt;
    }

    public void setActualCheckoutAt(Date actualCheckoutAt) {
        this.actualCheckoutAt = actualCheckoutAt;
    }

    public Date getPlannedEndsAt() {
        return plannedEndsAt;
    }

    public void setPlannedEndsAt(Date plannedEndsAt) {
        this.plannedEndsAt = plannedEndsAt;
    }

    public Integer getOverstayMinutes() {
        return overstayMinutes != null ? overstayMinutes : 0;
    }

    public void setOverstayMinutes(Integer overstayMinutes) {
        this.overstayMinutes = overstayMinutes;
    }

    public Double getBaseCost() {
        return baseCost != null ? baseCost : 0.0;
    }

    public void setBaseCost(Double baseCost) {
        this.baseCost = baseCost;
    }

    public Double getOverstayPenaltyFee() {
        return overstayPenaltyFee != null ? overstayPenaltyFee : 0.0;
    }

    public void setOverstayPenaltyFee(Double overstayPenaltyFee) {
        this.overstayPenaltyFee = overstayPenaltyFee;
    }

    public Double getTotalFinalAmount() {
        return totalFinalAmount != null ? totalFinalAmount : 0.0;
    }

    public void setTotalFinalAmount(Double totalFinalAmount) {
        this.totalFinalAmount = totalFinalAmount;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
