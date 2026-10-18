package com.example.project.model;

/**
 * Model class representing an earnings transaction in the smart parking system.
 */
public class Transaction {

    private String id;
    private String ownerEmail;
    private String placeName;
    private String carInfo;
    private String duration;
    private String date;
    private double totalPrice;

    /**
     * Default constructor for Transaction.
     */
    public Transaction() {
        this.ownerEmail = "";
    }

    /**
     * Parameterized constructor for Transaction.
     *
     * @param id         Unique transaction ID
     * @param placeName  Parking spot title
     * @param carInfo    Vehicle info string
     * @param duration   Rental duration
     * @param date       Transaction date
     * @param totalPrice Total earnings in PLN
     */
    public Transaction(String id, String placeName, String carInfo, String duration, String date, double totalPrice) {
        this.id = id;
        this.ownerEmail = "";
        this.placeName = placeName;
        this.carInfo = carInfo;
        this.duration = duration;
        this.date = date;
        this.totalPrice = totalPrice;
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

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getCarInfo() {
        return carInfo;
    }

    public void setCarInfo(String carInfo) {
        this.carInfo = carInfo;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}