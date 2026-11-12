package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Data Transfer Object (DTO) representing an individual withdrawal transaction
 * returned by POST /api/owner/payouts/withdraw, GET /api/owner/payouts/history,
 * and GET /api/owner/payouts/withdrawals.
 */
public class WithdrawalResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("status")
    private String status;

    @SerializedName("iban")
    private String iban;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("availableBalance")
    private Double availableBalance;

    public WithdrawalResponse() {
        this.currency = "PLN";
        this.status = "PENDING";
        this.amount = 0.0;
        this.availableBalance = 0.0;
    }

    public WithdrawalResponse(String id, Double amount, String currency, String status,
                              String iban, Date createdAt, Double availableBalance) {
        this.id = id;
        this.amount = amount != null ? amount : 0.0;
        this.currency = currency != null ? currency : "PLN";
        this.status = status != null ? status : "PENDING";
        this.iban = iban != null ? iban : "";
        this.createdAt = createdAt;
        this.availableBalance = availableBalance != null ? availableBalance : 0.0;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount != null ? amount : 0.0;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIban() {
        return iban != null ? iban : "";
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public double getAvailableBalance() {
        return availableBalance != null ? availableBalance : 0.0;
    }

    public void setAvailableBalance(Double availableBalance) {
        this.availableBalance = availableBalance;
    }
}
