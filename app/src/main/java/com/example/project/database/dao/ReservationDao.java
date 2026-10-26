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

    @Query("SELECT * FROM reservations WHERE status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    LiveData<ReservationEntity> getActiveReservationLiveData();

    @Query("SELECT * FROM reservations WHERE userEmail = :userEmail AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    LiveData<ReservationEntity> getUserActiveReservationLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') AND (status = 'IN_PROGRESS' OR endTimeMs = 0 OR endTimeMs > :currentTimeMs) ORDER BY startTimeMs ASC")
    LiveData<List<ReservationEntity>> getIncomingReservationsLiveData(String userEmail, long currentTimeMs);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) AND status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs ASC")
    LiveData<List<ReservationEntity>> getIncomingReservationsLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE status IN ('PENDING_PAYMENT', 'PENDING_APPROVAL', 'CONFIRMED', 'IN_PROGRESS', 'AKTYWNA', 'ACTIVE') ORDER BY startTimeMs DESC LIMIT 1")
    ReservationEntity getActiveReservationSync();

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    ReservationEntity getReservationByIdSync(String id);

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    LiveData<ReservationEntity> getReservationByIdLiveData(String id);

    @Query("SELECT * FROM reservations WHERE status != 'AKTYWNA' ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsLiveData();

    @Query("SELECT * FROM reservations WHERE userEmail = :userEmail AND status != 'AKTYWNA' ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getUserHistoryReservationsLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) AND (status IN ('COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED', 'ZAKOŃCZONA', 'ZAKONCZONA', 'ANULOWANA', 'ODRZUCONA') OR (status NOT IN ('IN_PROGRESS') AND endTimeMs > 0 AND endTimeMs <= :currentTimeMs)) ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsListLiveData(String userEmail, long currentTimeMs);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) AND status IN ('COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED', 'ZAKOŃCZONA', 'ZAKONCZONA', 'ANULOWANA', 'ODRZUCONA') ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsListLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getAllReservationsListLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE dateString = :dateStr")
    List<ReservationEntity> getReservationsForDate(String dateStr);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ReservationEntity reservation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<ReservationEntity> reservations);

    @Query("DELETE FROM reservations")
    void deleteAll();
}
