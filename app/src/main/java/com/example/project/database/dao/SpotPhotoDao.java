package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.SpotPhotoEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for managing spot photos in the local Room cache.
 */
@Dao
public interface SpotPhotoDao {

    /**
     * Gets all photos for a parking spot ordered by position.
     *
     * @param spotId Unique spot identifier
     * @return LiveData list of photos
     */
    @Query("SELECT * FROM spot_photos WHERE spotId = :spotId ORDER BY position ASC")
    LiveData<List<SpotPhotoEntity>> getPhotosForSpotLiveData(String spotId);

    /**
     * Synchronously retrieves all photos for a parking spot ordered by position.
     *
     * @param spotId Unique spot identifier
     * @return List of photos
     */
    @Query("SELECT * FROM spot_photos WHERE spotId = :spotId ORDER BY position ASC")
    List<SpotPhotoEntity> getPhotosForSpotSync(String spotId);

    /**
     * Inserts or replaces a spot photo.
     *
     * @param photo SpotPhotoEntity instance
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SpotPhotoEntity photo);

    /**
     * Inserts or replaces a list of spot photos.
     *
     * @param photos List of SpotPhotoEntity items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SpotPhotoEntity> photos);

    /**
     * Deletes a photo by its unique identifier.
     *
     * @param photoId Photo identifier
     */
    @Query("DELETE FROM spot_photos WHERE id = :photoId")
    void deleteById(String photoId);

    /**
     * Deletes all photos associated with a spot.
     *
     * @param spotId Spot identifier
     */
    @Query("DELETE FROM spot_photos WHERE spotId = :spotId")
    void deleteForSpot(String spotId);
}
