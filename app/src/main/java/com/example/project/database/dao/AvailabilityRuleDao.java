package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.project.database.entity.AvailabilityRuleEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for managing weekly availability rules in the Room database.
 */
@Dao
public interface AvailabilityRuleDao {

    /**
     * Observes all availability rules for a specific parking spot ordered by day of week and start time.
     *
     * @param spotId Unique spot identifier
     * @return LiveData containing the list of availability rules
     */
    @Query("SELECT * FROM availability_rules WHERE spotId = :spotId ORDER BY dayOfWeek ASC, startTime ASC")
    LiveData<List<AvailabilityRuleEntity>> getRulesForSpotLiveData(String spotId);

    /**
     * Synchronously retrieves all availability rules for a specific parking spot.
     *
     * @param spotId Unique spot identifier
     * @return List of availability rules
     */
    @Query("SELECT * FROM availability_rules WHERE spotId = :spotId ORDER BY dayOfWeek ASC, startTime ASC")
    List<AvailabilityRuleEntity> getRulesForSpotSync(String spotId);

    /**
     * Inserts or replaces a list of availability rules into the database.
     *
     * @param rules List of rules to insert or replace
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AvailabilityRuleEntity> rules);

    /**
     * Deletes all availability rules associated with the given spot identifier.
     *
     * @param spotId Unique spot identifier
     */
    @Query("DELETE FROM availability_rules WHERE spotId = :spotId")
    void deleteRulesForSpot(String spotId);

    /**
     * Atomically replaces all existing availability rules for a spot with a new set of rules.
     *
     * @param spotId Unique spot identifier
     * @param rules  New list of rules to store
     */
    @Transaction
    default void replaceRulesForSpot(String spotId, List<AvailabilityRuleEntity> rules) {
        deleteRulesForSpot(spotId);
        if (rules != null && !rules.isEmpty()) {
            insertAll(rules);
        }
    }
}
