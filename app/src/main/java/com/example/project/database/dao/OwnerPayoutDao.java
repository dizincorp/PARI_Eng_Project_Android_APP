package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.OwnerBalanceEntity;
import com.example.project.database.entity.WithdrawalEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for owner wallet balance and withdrawal transaction cache.
 */
@Dao
public interface OwnerPayoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBalance(OwnerBalanceEntity balance);

    @Query("SELECT * FROM owner_balance WHERE id = :id LIMIT 1")
    LiveData<OwnerBalanceEntity> getBalanceLiveData(String id);

    @Query("SELECT * FROM owner_balance WHERE id = :id LIMIT 1")
    OwnerBalanceEntity getBalanceSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWithdrawals(List<WithdrawalEntity> withdrawals);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWithdrawal(WithdrawalEntity withdrawal);

    @Query("SELECT * FROM owner_withdrawals ORDER BY createdAt DESC")
    LiveData<List<WithdrawalEntity>> getWithdrawalsLiveData();

    @Query("SELECT * FROM owner_withdrawals ORDER BY createdAt DESC")
    List<WithdrawalEntity> getWithdrawalsSync();

    @Query("DELETE FROM owner_withdrawals")
    void clearAllWithdrawals();
}
