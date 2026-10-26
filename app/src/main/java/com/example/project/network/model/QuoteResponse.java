package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object (DTO) representing the calculated pricing breakdown
 * returned by POST /api/reservations/quote.
 */
public class QuoteResponse {

    @SerializedName("spotId")
    private String spotId;

    @SerializedName("spotName")
    private String spotName;

    @SerializedName("startsAt")
    private Date startsAt;

    @SerializedName("endsAt")
    private Date endsAt;

    @SerializedName("hourlyRate")
    private Double hourlyRate;

    @SerializedName("durationHours")
    private Double durationHours;

    @SerializedName("spotCost")
    private Double spotCost;

    @SerializedName("serviceFee")
    private Double serviceFee;

    @SerializedName("total")
    private Double total;

    @SerializedName("currency")
    private String currency;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public QuoteResponse() {
        this.hourlyRate = 0.0;
        this.durationHours = 0.0;
        this.spotCost = 0.0;
        this.serviceFee = 0.0;
        this.total = 0.0;
        this.currency = "PLN";
    }

    /**
     * Parameterized constructor initializing all pricing quote fields.
     *
     * @param spotId        Target spot UUID
     * @param spotName      Spot name / title
     * @param startsAt      Start timestamp
     * @param endsAt        End timestamp
     * @param hourlyRate    Hourly rate
     * @param durationHours Duration in hours
     * @param spotCost      Base spot cost
     * @param serviceFee    Platform service fee
     * @param total         Total calculated cost
     * @param currency      Currency code (e.g. "PLN")
     */
    public QuoteResponse(String spotId, String spotName, Date startsAt, Date endsAt,
                         Double hourlyRate, Double durationHours, Double spotCost,
                         Double serviceFee, Double total, String currency) {
        this.spotId = spotId;
        this.spotName = spotName;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.hourlyRate = hourlyRate;
        this.durationHours = durationHours;
        this.spotCost = spotCost;
        this.serviceFee = serviceFee;
        this.total = total;
        this.currency = (currency != null && !currency.isEmpty()) ? currency : "PLN";
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

    public Double getHourlyRate() {
        return hourlyRate != null ? hourlyRate : 0.0;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Double getDurationHours() {
        return durationHours != null ? durationHours : 0.0;
    }

    public void setDurationHours(Double durationHours) {
        this.durationHours = durationHours;
    }

    public Double getSpotCost() {
        return spotCost != null ? spotCost : 0.0;
    }

    public void setSpotCost(Double spotCost) {
        this.spotCost = spotCost;
    }

    public Double getServiceFee() {
        return serviceFee != null ? serviceFee : 0.0;
    }

    public void setServiceFee(Double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public Double getTotal() {
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

    /**
     * Convenience getter for start time in milliseconds.
     *
     * @return Start timestamp in epoch milliseconds, or 0 if null
     */
    public long getStartsAtMillis() {
        return startsAt != null ? startsAt.getTime() : 0L;
    }

    /**
     * Convenience getter for end time in milliseconds.
     *
     * @return End timestamp in epoch milliseconds, or 0 if null
     */
    public long getEndsAtMillis() {
        return endsAt != null ? endsAt.getTime() : 0L;
    }
}
