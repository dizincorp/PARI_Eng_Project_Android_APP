package com.example.project.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.project.database.entity.UserEntity;

/**
 * Data Access Object for User table.
 */
@Dao
public interface UserDao {

    @Query("SELECT * FROM users LIMIT 1")
    LiveData<UserEntity> getCurrentUserLiveData();

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserEntity user);
}