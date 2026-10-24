package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for creating a new vehicle via POST /api/vehicles.
 */
public class CreateVehicleRequest {

    @SerializedName("model")
    private String model;

    @SerializedName("plateNumber")
    private String plateNumber;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public CreateVehicleRequest() {
    }

    /**
     * Parameterized constructor for CreateVehicleRequest.
     *
     * @param model       Vehicle make and model (e.g. "Skoda Octavia")
     * @param plateNumber License plate number (2-15 characters, e.g. "LU12345")
     */
    public CreateVehicleRequest(String model, String plateNumber) {
        this.model = model != null ? model.trim() : "";
        this.plateNumber = plateNumber != null ? plateNumber.trim() : "";
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
}
