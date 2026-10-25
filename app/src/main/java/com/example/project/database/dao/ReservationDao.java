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

    @Query("SELECT * FROM reservations WHERE status = 'AKTYWNA' ORDER BY startTimeMs DESC LIMIT 1")
    LiveData<ReservationEntity> getActiveReservationLiveData();

    @Query("SELECT * FROM reservations WHERE userEmail = :userEmail AND status = 'AKTYWNA' ORDER BY startTimeMs DESC LIMIT 1")
    LiveData<ReservationEntity> getUserActiveReservationLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) AND (status = 'AKTYWNA' OR status = 'CONFIRMED' OR status = 'PENDING_PAYMENT' OR status = 'ACTIVE') ORDER BY startTimeMs ASC")
    LiveData<List<ReservationEntity>> getIncomingReservationsLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE status = 'AKTYWNA' ORDER BY startTimeMs DESC LIMIT 1")
    ReservationEntity getActiveReservationSync();

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    ReservationEntity getReservationByIdSync(String id);

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    LiveData<ReservationEntity> getReservationByIdLiveData(String id);

    @Query("SELECT * FROM reservations WHERE status != 'AKTYWNA' ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getHistoryReservationsLiveData();

    @Query("SELECT * FROM reservations WHERE userEmail = :userEmail AND status != 'AKTYWNA' ORDER BY startTimeMs DESC")
    LiveData<List<ReservationEntity>> getUserHistoryReservationsLiveData(String userEmail);

    @Query("SELECT * FROM reservations WHERE (:userEmail = '' OR userEmail = :userEmail) AND status NOT IN ('AKTYWNA', 'CONFIRMED', 'PENDING_PAYMENT', 'ACTIVE') ORDER BY startTimeMs DESC")
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
