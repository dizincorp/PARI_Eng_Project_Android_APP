package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.project.database.entity.BlockedPeriodEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for managing calendar blocked periods in the Room database.
 */
@Dao
public interface BlockedPeriodDao {

    /**
     * Observes all blocked periods for a specific parking spot ordered by start time.
     *
     * @param spotId Unique spot identifier
     * @return LiveData containing the list of blocked periods
     */
    @Query("SELECT * FROM blocked_periods WHERE spotId = :spotId ORDER BY start ASC")
    LiveData<List<BlockedPeriodEntity>> getBlockedPeriodsForSpotLiveData(String spotId);

    /**
     * Synchronously retrieves all blocked periods for a specific parking spot.
     *
     * @param spotId Unique spot identifier
     * @return List of blocked periods
     */
    @Query("SELECT * FROM blocked_periods WHERE spotId = :spotId ORDER BY start ASC")
    List<BlockedPeriodEntity> getBlockedPeriodsForSpotSync(String spotId);

    /**
     * Retrieves all blocked periods across all spots ordered by start time.
     *
     * @return List of all blocked periods
     */
    @Query("SELECT * FROM blocked_periods ORDER BY start ASC")
    List<BlockedPeriodEntity> getAllBlockedPeriodsSync();

    /**
     * Observes all blocked periods across all spots ordered by start time.
     *
     * @return LiveData emitting list of all blocked periods
     */
    @Query("SELECT * FROM blocked_periods ORDER BY start ASC")
    LiveData<List<BlockedPeriodEntity>> getAllBlockedPeriodsLiveData();

    /**
     * Inserts or replaces a single blocked period into the database.
     *
     * @param block Blocked period to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlockedPeriodEntity block);

    /**
     * Inserts or replaces a list of blocked periods into the database.
     *
     * @param blocks List of blocked periods to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BlockedPeriodEntity> blocks);

    /**
     * Deletes a blocked period by its unique identifier.
     *
     * @param periodId Unique block period identifier
     */
    @Query("DELETE FROM blocked_periods WHERE id = :periodId")
    void deleteById(String periodId);

    /**
     * Deletes all blocked periods associated with a parking spot.
     *
     * @param spotId Unique spot identifier
     */
    @Query("DELETE FROM blocked_periods WHERE spotId = :spotId")
    void deleteForSpot(String spotId);

    /**
     * Atomically replaces all blocked periods for a spot with a new collection.
     *
     * @param spotId Unique spot identifier
     * @param blocks New list of blocked periods
     */
    @Transaction
    default void replaceBlockedPeriodsForSpot(String spotId, List<BlockedPeriodEntity> blocks) {
        deleteForSpot(spotId);
        if (blocks != null && !blocks.isEmpty()) {
            insertAll(blocks);
        }
    }
}
