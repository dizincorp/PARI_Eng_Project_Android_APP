package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.TransactionEntity;

import java.util.List;

/**
 * Data Access Object for Transaction table.
 */
@Dao
public interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    LiveData<List<TransactionEntity>> getAllTransactionsLiveData();

    @Query("SELECT * FROM transactions WHERE ownerEmail = :ownerEmail ORDER BY id DESC")
    LiveData<List<TransactionEntity>> getUserTransactionsLiveData(String ownerEmail);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TransactionEntity transaction);
}