package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a vehicle in the database.
 */
@Entity(tableName = "vehicles")
public class VehicleEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String makeAndModel;
    private String licensePlate;
    private String color;

    public VehicleEntity() {
        this.id = "v_" + System.currentTimeMillis();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getMakeAndModel() {
        return makeAndModel;
    }

    public void setMakeAndModel(String makeAndModel) {
        this.makeAndModel = makeAndModel;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}