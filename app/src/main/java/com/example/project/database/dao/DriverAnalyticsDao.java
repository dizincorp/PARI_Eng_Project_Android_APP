package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.DriverAnalyticsEntity;

/**
 * Data Access Object (DAO) for driver analytics cache operations.
 */
@Dao
public interface DriverAnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DriverAnalyticsEntity entity);

    @Query("SELECT * FROM driver_analytics WHERE id = :id LIMIT 1")
    LiveData<DriverAnalyticsEntity> getDriverAnalyticsLiveData(String id);

    @Query("SELECT * FROM driver_analytics WHERE id = :id LIMIT 1")
    DriverAnalyticsEntity getDriverAnalyticsSync(String id);

    @Query("DELETE FROM driver_analytics")
    void clearAll();
}
