package com.example.project.model;

/**
 * Model class representing a user's vehicle in the smart parking system.
 */
public class Vehicle {

    private String id;
    private String ownerEmail;
    private String makeAndModel;
    private String licensePlate;
    private String color;

    /**
     * Default constructor for Vehicle.
     */
    public Vehicle() {
        this.ownerEmail = "";
    }

    /**
     * Parameterized constructor for Vehicle.
     *
     * @param id           Unique vehicle ID
     * @param makeAndModel Car make and model
     * @param licensePlate License plate number
     * @param color        Car body color
     */
    public Vehicle(String id, String makeAndModel, String licensePlate, String color) {
        this.id = id;
        this.ownerEmail = "";
        this.makeAndModel = makeAndModel;
        this.licensePlate = licensePlate;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getMakeAndModel() {
        return makeAndModel;
    }

    public void setMakeAndModel(String makeAndModel) {
        this.makeAndModel = makeAndModel;
    }

    public String getModel() {
        return makeAndModel;
    }

    public void setModel(String model) {
        this.makeAndModel = model;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getPlateNumber() {
        return licensePlate;
    }

    public void setPlateNumber(String plateNumber) {
        this.licensePlate = plateNumber;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Gets a formatted string description of the vehicle.
     *
     * @return Formatted vehicle text
     */
    public String getDisplayText() {
        if (licensePlate != null && !licensePlate.isEmpty()) {
            return makeAndModel + " - " + licensePlate;
        }
        return makeAndModel;
    }
}
