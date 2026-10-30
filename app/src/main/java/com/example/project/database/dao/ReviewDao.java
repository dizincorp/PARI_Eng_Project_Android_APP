package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.ReviewEntity;

import java.util.List;

/**
 * Data Access Object for local persistent caching of parking spot reviews.
 */
@Dao
public interface ReviewDao {

    /**
     * Observes all cached reviews for a specific parking spot ordered by creation time descending.
     *
     * @param spotId Unique parking spot identifier
     * @return LiveData list of cached ReviewEntity items
     */
    @Query("SELECT * FROM reviews WHERE spotId = :spotId ORDER BY createdAtTimestamp DESC")
    LiveData<List<ReviewEntity>> getReviewsBySpotIdLiveData(String spotId);

    /**
     * Synchronously retrieves all cached reviews for a specific parking spot.
     *
     * @param spotId Unique parking spot identifier
     * @return List of cached ReviewEntity items
     */
    @Query("SELECT * FROM reviews WHERE spotId = :spotId ORDER BY createdAtTimestamp DESC")
    List<ReviewEntity> getReviewsBySpotIdSync(String spotId);

    /**
     * Inserts or updates an individual review in the Room database cache.
     *
     * @param review ReviewEntity instance
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ReviewEntity review);

    /**
     * Inserts or updates a list of reviews in the Room database cache.
     *
     * @param reviews List of ReviewEntity items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<ReviewEntity> reviews);

    /**
     * Deletes all cached reviews for a specific parking spot.
     *
     * @param spotId Unique parking spot identifier
     */
    @Query("DELETE FROM reviews WHERE spotId = :spotId")
    void deleteBySpotId(String spotId);

    /**
     * Clears all cached reviews from the table.
     */
    @Query("DELETE FROM reviews")
    void deleteAll();
}
