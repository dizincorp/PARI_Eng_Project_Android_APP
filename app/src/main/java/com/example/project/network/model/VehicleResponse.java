package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing a vehicle response from the backend REST API
 * returned by POST /api/vehicles and GET /api/vehicles.
 */
public class VehicleResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("model")
    private String model;

    @SerializedName("plateNumber")
    private String plateNumber;

    @SerializedName("createdAt")
    private String createdAt;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public VehicleResponse() {
    }

    /**
     * Parameterized constructor for VehicleResponse.
     *
     * @param id          Unique vehicle UUID
     * @param model       Vehicle model
     * @param plateNumber License plate number
     * @param createdAt   Timestamp of vehicle creation
     */
    public VehicleResponse(String id, String model, String plateNumber, String createdAt) {
        this.id = id;
        this.model = model;
        this.plateNumber = plateNumber;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model != null ? model : "";
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPlateNumber() {
        return plateNumber != null ? plateNumber : "";
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
