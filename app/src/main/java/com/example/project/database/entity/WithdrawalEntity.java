package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Room Database entity representing cached owner withdrawal history records.
 */
@Entity(tableName = "owner_withdrawals")
public class WithdrawalEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "currency")
    private String currency;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "iban")
    private String iban;

    @ColumnInfo(name = "createdAt")
    private Date createdAt;

    @ColumnInfo(name = "availableBalance")
    private double availableBalance;

    public WithdrawalEntity() {
        this.id = "";
        this.currency = "PLN";
        this.status = "PENDING";
    }

    @Ignore
    public WithdrawalEntity(@NonNull String id, double amount, @Nullable String currency,
                            @Nullable String status, @Nullable String iban,
                            @Nullable Date createdAt, double availableBalance) {
        this.id = id;
        this.amount = amount;
        this.currency = currency != null ? currency : "PLN";
        this.status = status != null ? status : "PENDING";
        this.iban = iban != null ? iban : "";
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.availableBalance = availableBalance;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public String getIban() {
        return iban != null ? iban : "";
    }

    public void setIban(@Nullable String iban) {
        this.iban = iban;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@Nullable Date createdAt) {
        this.createdAt = createdAt;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = availableBalance;
    }
}
