package com.example.project.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.dao.ReservationDao;
import com.example.project.database.dao.TransactionDao;
import com.example.project.database.dao.UserDao;
import com.example.project.database.dao.VehicleDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.database.entity.TransactionEntity;
import com.example.project.database.entity.UserEntity;
import com.example.project.database.entity.VehicleEntity;

/**
 * Central Room Database class for local persistent caching.
 */
@Database(
        entities = {
                ParkingPlaceEntity.class,
                ReservationEntity.class,
                VehicleEntity.class,
                UserEntity.class,
                TransactionEntity.class
        },
        version = 2,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "parkshare_db";
    private static AppDatabase instance;

    public abstract ParkingPlaceDao parkingPlaceDao();
    public abstract ReservationDao reservationDao();
    public abstract VehicleDao vehicleDao();
    public abstract UserDao userDao();
    public abstract TransactionDao transactionDao();

    /**
     * Gets the singleton instance of AppDatabase.
     *
     * @param context Application context
     * @return Room database instance
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}