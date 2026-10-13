package com.example.project;

public class Vehicle {

    private String id;
    private String makeAndModel; // e.g. "Tesla Model 3"
    private String licensePlate; // e.g. "WX 12345"
    private String color;        // e.g. "Czarny"

    public Vehicle() {
    }

    public Vehicle(String id, String makeAndModel, String licensePlate, String color) {
        this.id = id;
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

    public String getDisplayText() {
        if (licensePlate != null && !licensePlate.isEmpty()) {
            return makeAndModel + " - " + licensePlate;
        }
        return makeAndModel;
    }
}