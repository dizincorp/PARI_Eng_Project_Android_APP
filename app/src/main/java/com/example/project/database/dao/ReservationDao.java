package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.ReservationEntity;

import java.util.List;

/**
 * Data Access Object for Reservation table providing CRUD operations
 * and reactive LiveData query streams.
 */
@Dao
public interface ReservationDao {

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND isOwnerIncoming = 0 AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    LiveData<ReservationEntity> getActiveReservationLiveData();

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    LiveData<ReservationEntity> getUserActiveReservationLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') AND (status = 'IN_PROGRESS' OR endTimeMs = 0 OR endTimeMs > :currentTimeMs) ORDER BY startTimeMs ASC")
    LiveData<List<ReservationEntity>> getIncomingReservationsLiveData(String userEmail, long currentTimeMs);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs ASC")
    LiveData<List<ReservationEntity>> getIncomingReservationsLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND isOwnerIncoming = 0 AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    ReservationEntity getActiveReservationSync();

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    ReservationEntity getReservationByIdSync(String id);

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    LiveData<ReservationEntity> getReservationByIdLiveData(String id);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND isOwnerIncoming = 0 AND status != 'AKTYWNA' ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsLiveData();

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 AND status != 'AKTYWNA' ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getUserHistoryReservationsLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 AND (status IN ('COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED', 'NO_SHOW', 'ZAKOŃCZONA', 'ZAKONCZONA', 'ANULOWANA', 'ODRZUCONA') OR (status NOT IN ('IN_PROGRESS') AND endTimeMs > 0 AND endTimeMs <= :currentTimeMs)) ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsListLiveData(String userEmail, long currentTimeMs);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 AND status IN ('COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED', 'NO_SHOW', 'ZAKOŃCZONA', 'ZAKONCZONA', 'ANULOWANA', 'ODRZUCONA') ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsListLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0 ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getAllReservationsListLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (userEmail IS NULL OR userEmail != '__calendar_slot__') AND (:userEmail IS NULL OR TRIM(:userEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:userEmail))) AND isOwnerIncoming = 0")
    List<ReservationEntity> getDriverReservationsSync(String userEmail);

    @Query("SELECT * FROM reservations WHERE (:ownerEmail IS NULL OR TRIM(:ownerEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:ownerEmail))) AND isOwnerIncoming = 1 ORDER BY startTimeMs DESC")
    List<ReservationEntity> getOwnerIncomingReservationsSync(String ownerEmail);

    @Query("SELECT * FROM reservations WHERE (:ownerEmail IS NULL OR TRIM(:ownerEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:ownerEmail))) AND isOwnerIncoming = 1 AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs ASC")
    LiveData<List<ReservationEntity>> getOwnerIncomingReservationsLiveData(String ownerEmail);

    @Query("SELECT * FROM reservations WHERE (:ownerEmail IS NULL OR TRIM(:ownerEmail) = '' OR userEmail IS NULL OR TRIM(userEmail) = '' OR LOWER(TRIM(userEmail)) = LOWER(TRIM(:ownerEmail))) AND isOwnerIncoming = 1 ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getAllOwnerIncomingReservationsLiveData(String ownerEmail);

    @Query("SELECT * FROM reservations WHERE dateString = :dateStr")
    List<ReservationEntity> getReservationsForDate(String dateStr);

    @Query("SELECT * FROM reservations WHERE placeId IN (:spotIds) AND isOwnerIncoming = 0")
    LiveData<List<ReservationEntity>> getReservationsForSpotIdsLiveData(List<String> spotIds);

    @Query("SELECT * FROM reservations WHERE placeId = :spotId AND isOwnerIncoming = 0")
    LiveData<List<ReservationEntity>> getReservationsForSpotLiveData(String spotId);

    @Query("SELECT * FROM reservations")
    LiveData<List<ReservationEntity>> getAllReservationsLiveData();

    /**
     * Gets the latest active reservation for a specific parking spot by spot ID.
     *
     * @param spotId Unique spot identifier
     * @return Active ReservationEntity or null if none
     */
    @Query("SELECT * FROM reservations WHERE placeId = :spotId AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    ReservationEntity getActiveReservationForSpotSync(String spotId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ReservationEntity reservation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<ReservationEntity> reservations);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnoreAll(List<ReservationEntity> reservations);

    @Query("DELETE FROM reservations WHERE id LIKE 'block_%' OR status IN ('ZABLOKOWANA', 'BLOCKED')")
    void deleteBlockReservations();

    @Query("DELETE FROM reservations")
    void deleteAll();
}
