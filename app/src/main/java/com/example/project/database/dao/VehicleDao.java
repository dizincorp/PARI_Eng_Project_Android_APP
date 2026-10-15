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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(VehicleEntity vehicle);
}