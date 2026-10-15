package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    public ParkingPlaceEntity() {
        this.id = "p_" + System.currentTimeMillis();
        this.pinCode = "4829";
        this.rating = 0.0;
        this.ratingCount = 0;
        this.ownerEmail = "";
        this.imageUri = "";
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
        return photoResId;
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
}