package com.example.project.model;

import com.example.project.R;

/**
 * Model class representing a user's reservation in the smart parking system.
 */
public class Reservation {

    private String id;
    private String userEmail;
    private String placeId;
    private String placeName;
    private String street;
    private String city;
    private String placeNumber;
    private String dateString;
    private String timeRange;
    private long startTimeMs;
    private long endTimeMs;
    private String userName;
    private String carModel;
    private String carPlate;
    private String vehicleInfo;
    private String status;
    private double totalPrice;
    private String pinCode;
    private int photoResId;

    /**
     * Default constructor for Reservation.
     */
    public Reservation() {
        this.userEmail = "";
        this.pinCode = "4829";
        this.status = "AKTYWNA";
        this.userName = "Użytkownik";
        this.carModel = "Tesla Model 3";
        this.carPlate = "WX 12345";
    }

    /**
     * Parameterized constructor for Reservation.
     *
     * @param id          Unique reservation ID
     * @param placeId     ID of the reserved parking place
     * @param placeName   Name of the place
     * @param street      Street address
     * @param city        City name
     * @param placeNumber Designation number
     * @param dateString  Date string (e.g. Dziś, 14 Października)
     * @param timeRange   Time range string (e.g. 14:00 - 18:00)
     * @param startTimeMs Start timestamp in ms
     * @param endTimeMs   End timestamp in ms
     * @param vehicleInfo Vehicle description string
     * @param status      Reservation status (AKTYWNA, ANULOWANA, ZAKOŃCZONA)
     * @param totalPrice  Total price in PLN
     * @param pinCode     Entry PIN code
     * @param photoResId  Resource ID for place photo
     */
    public Reservation(String id, String placeId, String placeName, String street, String city,
                       String placeNumber, String dateString, String timeRange,
                       long startTimeMs, long endTimeMs, String vehicleInfo,
                       String status, double totalPrice, String pinCode, int photoResId) {
        this.id = id;
        this.userEmail = "";
        this.placeId = placeId;
        this.placeName = placeName;
        this.street = street;
        this.city = city;
        this.placeNumber = placeNumber;
        this.dateString = dateString;
        this.timeRange = timeRange;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.vehicleInfo = vehicleInfo;
        this.status = status;
        this.totalPrice = totalPrice;
        this.pinCode = pinCode != null ? pinCode : "4829";
        this.photoResId = photoResId != 0 ? photoResId : R.drawable.default_parking_spot;
        this.userName = "Jan K.";
        this.carModel = vehicleInfo != null ? vehicleInfo : "Pojazd";
        this.carPlate = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPlaceNumber() {
        return placeNumber;
    }

    public void setPlaceNumber(String placeNumber) {
        this.placeNumber = placeNumber;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public void setEndTimeMs(long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }

    public String getUserName() {
        return userName != null ? userName : "Użytkownik";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCarModel() {
        return carModel != null ? carModel : (vehicleInfo != null ? vehicleInfo : "Pojazd");
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarPlate() {
        return carPlate != null ? carPlate : "";
    }

    public void setCarPlate(String carPlate) {
        this.carPlate = carPlate;
    }

    public String getVehicleInfo() {
        return vehicleInfo;
    }

    public void setVehicleInfo(String vehicleInfo) {
        this.vehicleInfo = vehicleInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPinCode() {
        return pinCode != null ? pinCode : "4829";
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public int getPhotoResId() {
        return photoResId != 0 ? photoResId : R.drawable.default_parking_spot;
    }

    /**
     * Checks if the reservation has been checked in (parking session currently in progress).
     *
     * @return True if status is IN_PROGRESS
     */
    public boolean isCheckedIn() {
        return status != null && status.equalsIgnoreCase("IN_PROGRESS");
    }

    public void setPhotoResId(int photoResId) {
        this.photoResId = photoResId;
    }
}