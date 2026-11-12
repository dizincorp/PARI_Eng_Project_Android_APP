package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for owner balance information from GET /api/owner/payouts/balance.
 */
public class OwnerBalanceResponse {

    @SerializedName("availableBalance")
    private Double availableBalance;

    @SerializedName("pendingEscrow")
    private Double pendingEscrow;

    @SerializedName("currency")
    private String currency;

    @SerializedName("totalEarned")
    private Double totalEarned;

    @SerializedName("platformFeesPaid")
    private Double platformFeesPaid;

    public OwnerBalanceResponse() {
        this.availableBalance = 0.0;
        this.pendingEscrow = 0.0;
        this.currency = "PLN";
        this.totalEarned = 0.0;
        this.platformFeesPaid = 0.0;
    }

    public OwnerBalanceResponse(Double availableBalance, Double pendingEscrow, String currency,
                                Double totalEarned, Double platformFeesPaid) {
        this.availableBalance = availableBalance != null ? availableBalance : 0.0;
        this.pendingEscrow = pendingEscrow != null ? pendingEscrow : 0.0;
        this.currency = currency != null ? currency : "PLN";
        this.totalEarned = totalEarned != null ? totalEarned : 0.0;
        this.platformFeesPaid = platformFeesPaid != null ? platformFeesPaid : 0.0;
    }

    public double getAvailableBalance() {
        return availableBalance != null ? availableBalance : 0.0;
    }

    public void setAvailableBalance(Double availableBalance) {
        this.availableBalance = availableBalance;
    }

    public double getPendingEscrow() {
        return pendingEscrow != null ? pendingEscrow : 0.0;
    }

    public void setPendingEscrow(Double pendingEscrow) {
        this.pendingEscrow = pendingEscrow;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getTotalEarned() {
        return totalEarned != null ? totalEarned : 0.0;
    }

    public void setTotalEarned(Double totalEarned) {
        this.totalEarned = totalEarned;
    }

    public double getPlatformFeesPaid() {
        return platformFeesPaid != null ? platformFeesPaid : 0.0;
    }

    public void setPlatformFeesPaid(Double platformFeesPaid) {
        this.platformFeesPaid = platformFeesPaid;
    }
}
