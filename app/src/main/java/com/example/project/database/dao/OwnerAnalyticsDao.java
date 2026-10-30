package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.OwnerAnalyticsEntity;

/**
 * Data Access Object (DAO) for managing persistent caching of owner analytics.
 */
@Dao
public interface OwnerAnalyticsDao {

    /**
     * Observes the cached owner analytics singleton from Room Database.
     *
     * @param id Unique singleton record identifier
     * @return LiveData emitting OwnerAnalyticsEntity
     */
    @Query("SELECT * FROM owner_analytics WHERE id = :id LIMIT 1")
    LiveData<OwnerAnalyticsEntity> getAnalyticsLiveData(String id);

    /**
     * Retrieves the cached owner analytics synchronously.
     *
     * @param id Unique singleton record identifier
     * @return Cached OwnerAnalyticsEntity or null
     */
    @Query("SELECT * FROM owner_analytics WHERE id = :id LIMIT 1")
    OwnerAnalyticsEntity getAnalyticsSync(String id);

    /**
     * Inserts or replaces the cached analytics record.
     *
     * @param entity OwnerAnalyticsEntity to cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(OwnerAnalyticsEntity entity);

    /**
     * Deletes all cached owner analytics records.
     */
    @Query("DELETE FROM owner_analytics")
    void deleteAll();
}
