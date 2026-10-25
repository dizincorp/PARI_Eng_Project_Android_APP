package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.VehicleEntity;

import java.util.List;

/**
 * Data Access Object for Vehicle table.
 */
@Dao
public interface VehicleDao {

    @Query("SELECT * FROM vehicles")
    LiveData<List<VehicleEntity>> getAllVehiclesLiveData();

    @Query("SELECT * FROM vehicles WHERE ownerEmail = :ownerEmail")
    LiveData<List<VehicleEntity>> getUserVehiclesLiveData(String ownerEmail);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(VehicleEntity vehicle);

    @Query("DELETE FROM vehicles WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM vehicles WHERE ownerEmail = :ownerEmail AND UPPER(REPLACE(licensePlate, ' ', '')) = UPPER(REPLACE(:plate, ' ', '')) LIMIT 1")
    VehicleEntity getVehicleByPlate(String ownerEmail, String plate);

    @Query("DELETE FROM vehicles WHERE ownerEmail = :ownerEmail AND UPPER(REPLACE(licensePlate, ' ', '')) = UPPER(REPLACE(:plate, ' ', ''))")
    void deleteByPlate(String ownerEmail, String plate);
}
