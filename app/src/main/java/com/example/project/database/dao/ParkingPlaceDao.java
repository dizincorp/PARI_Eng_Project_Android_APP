package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.ParkingPlaceEntity;

import java.util.List;

/**
 * Data Access Object for ParkingPlace table.
 */
@Dao
public interface ParkingPlaceDao {

    @Query("SELECT * FROM parking_places")
    LiveData<List<ParkingPlaceEntity>> getAllPlacesLiveData();

    @Query("SELECT * FROM parking_places WHERE ownerEmail = :ownerEmail")
    LiveData<List<ParkingPlaceEntity>> getMyPlacesLiveData(String ownerEmail);

    @Query("SELECT * FROM parking_places WHERE isOccupied = 0")
    LiveData<List<ParkingPlaceEntity>> getAvailablePlacesLiveData();

    @Query("SELECT * FROM parking_places WHERE id = :id LIMIT 1")
    ParkingPlaceEntity getPlaceById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ParkingPlaceEntity place);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<ParkingPlaceEntity> places);

    @Delete
    void delete(ParkingPlaceEntity place);

    @Query("DELETE FROM parking_places WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM parking_places")
    void deleteAll();
}