package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for submitting an owner withdrawal request via POST /api/owner/payouts/withdraw.
 */
public class WithdrawRequest {

    @SerializedName("amount")
    private Double amount;

    @SerializedName("iban")
    private String iban;

    public WithdrawRequest() {
    }

    public WithdrawRequest(Double amount, String iban) {
        this.amount = amount;
        this.iban = iban;
    }

    public double getAmount() {
        return amount != null ? amount : 0.0;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getIban() {
        return iban != null ? iban : "";
    }

    public void setIban(String iban) {
        this.iban = iban;
    }
}
