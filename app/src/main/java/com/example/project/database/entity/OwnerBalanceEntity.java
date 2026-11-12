package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Database entity representing cached owner wallet balance, pending escrow,
 * and platform fee metrics from GET /api/owner/payouts/balance.
 */
@Entity(tableName = "owner_balance")
public class OwnerBalanceEntity {

    public static final String SINGLETON_ID = "CURRENT_OWNER_BALANCE";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "availableBalance")
    private double availableBalance;

    @ColumnInfo(name = "pendingEscrow")
    private double pendingEscrow;

    @ColumnInfo(name = "currency")
    private String currency;

    @ColumnInfo(name = "totalEarned")
    private double totalEarned;

    @ColumnInfo(name = "platformFeesPaid")
    private double platformFeesPaid;

    @ColumnInfo(name = "lastUpdatedTimestamp")
    private long lastUpdatedTimestamp;

    public OwnerBalanceEntity() {
        this.id = SINGLETON_ID;
        this.currency = "PLN";
    }

    @Ignore
    public OwnerBalanceEntity(@NonNull String id, double availableBalance, double pendingEscrow,
                              @Nullable String currency, double totalEarned, double platformFeesPaid,
                              long lastUpdatedTimestamp) {
        this.id = id;
        this.availableBalance = availableBalance;
        this.pendingEscrow = pendingEscrow;
        this.currency = currency != null ? currency : "PLN";
        this.totalEarned = totalEarned;
        this.platformFeesPaid = platformFeesPaid;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = availableBalance;
    }

    public double getPendingEscrow() {
        return pendingEscrow;
    }

    public void setPendingEscrow(double pendingEscrow) {
        this.pendingEscrow = pendingEscrow;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(double totalEarned) {
        this.totalEarned = totalEarned;
    }

    public double getPlatformFeesPaid() {
        return platformFeesPaid;
    }

    public void setPlatformFeesPaid(double platformFeesPaid) {
        this.platformFeesPaid = platformFeesPaid;
    }

    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}
