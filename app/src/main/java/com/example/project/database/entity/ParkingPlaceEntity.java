package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.project.R;

/**
 * Room Entity representing a parking spot in the database.
 */
@Entity(tableName = "parking_places")
public class ParkingPlaceEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String ownerEmail;
    private String title;
    private String description;
    private String accessInstructions;
    private String street;
    private String city;
    private String postalCode;
    private String placeNumber;
    private String floor;
    private double price;
    private String accessTime;
    private String amenitiesCsv;
    private double latitude;
    private double longitude;
    private boolean isOccupied;
    private String occupiedUntil;
    private String pinCode;
    private int photoResId;
    private String imageUri;
    private double rating;
    private int ratingCount;

    // Spot lifecycle status: DRAFT (#0), ACTIVE (#1), INACTIVE (#2), BLOCKED (#3), ARCHIVED (#4)
    private String status;

    // Current driver & vehicle information when spot is reserved/occupied
    private String driverName;
    private String driverVehicle;
    private boolean isCheckedIn;

    /**
     * Default constructor initializing defaults.
     */
    public ParkingPlaceEntity() {
        this.id = "p_" + System.currentTimeMillis();
        this.description = "";
        this.accessInstructions = "";
        this.pinCode = "4829";
        this.rating = 0.0;
        this.ratingCount = 0;
        this.ownerEmail = "";
        this.imageUri = "";
        this.status = "ACTIVE";
        this.isCheckedIn = false;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccessInstructions() {
        return accessInstructions != null ? accessInstructions : "";
    }

    public void setAccessInstructions(String accessInstructions) {
        this.accessInstructions = accessInstructions;
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

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPlaceNumber() {
        return placeNumber;
    }

    public void setPlaceNumber(String placeNumber) {
        this.placeNumber = placeNumber;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(String accessTime) {
        this.accessTime = accessTime;
    }

    public String getAmenitiesCsv() {
        return amenitiesCsv;
    }

    public void setAmenitiesCsv(String amenitiesCsv) {
        this.amenitiesCsv = amenitiesCsv;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public String getOccupiedUntil() {
        return occupiedUntil;
    }

    public void setOccupiedUntil(String occupiedUntil) {
        this.occupiedUntil = occupiedUntil;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public int getPhotoResId() {
        return photoResId != 0 ? photoResId : R.drawable.default_parking_spot;
    }

    public void setPhotoResId(int photoResId) {
        this.photoResId = photoResId;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    /**
     * Checks if this parking spot is active and available for public display and booking.
     *
     * @return True if status is ACTIVE
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(getStatus());
    }

    public String getStatus() {
        return status != null && !status.isEmpty() ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverVehicle() {
        return driverVehicle;
    }

    public void setDriverVehicle(String driverVehicle) {
        this.driverVehicle = driverVehicle;
    }

    public boolean isCheckedIn() {
        return isCheckedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        isCheckedIn = checkedIn;
    }

    /**
     * Determines whether the spot is currently reserved (booked by a driver, but check-in has not occurred yet).
     *
     * @return True if reserved, false otherwise
     */
    public boolean isReserved() {
        return (isOccupied || (driverName != null && !driverName.isEmpty())) && !isCheckedIn;
    }
}
