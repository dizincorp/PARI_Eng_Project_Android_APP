package com.example.project.model;

import com.example.project.R;

import java.util.List;
import java.util.Locale;

/**
 * Model class representing a parking spot in the smart parking system.
 */
public class ParkingPlace {

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
    private List<String> amenities;
    private double latitude;
    private double longitude;
    private boolean isOccupied;
    private String occupiedUntil;
    private String pinCode;
    private int photoResId;
    private String imageUri;
    private double rating;
    private int ratingCount;

    /**
     * Default constructor for ParkingPlace.
     */
    public ParkingPlace() {
        this.isOccupied = false;
        this.pinCode = "4829";
        this.rating = 0.0;
        this.ratingCount = 0;
        this.ownerEmail = "";
        this.imageUri = "";
    }

    /**
     * Parameterized constructor for ParkingPlace.
     *
     * @param id          Unique identifier
     * @param title       Title of the spot
     * @param street      Street name and number
     * @param city        City name
     * @param postalCode  Postal code
     * @param placeNumber Spot designation number
     * @param floor       Floor level
     * @param price       Hourly rental rate in PLN
     * @param accessTime  Available hours (e.g. 08:00 - 20:00)
     * @param amenities   List of amenities/tags
     * @param latitude    GPS latitude
     * @param longitude   GPS longitude
     */
    public ParkingPlace(String id, String title, String street, String city, String postalCode,
                        String placeNumber, String floor, double price, String accessTime,
                        List<String> amenities, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.placeNumber = placeNumber;
        this.floor = floor;
        this.price = price;
        this.accessTime = accessTime;
        this.amenities = amenities;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isOccupied = false;
        this.occupiedUntil = "";
        this.pinCode = "4829";
        this.photoResId = R.drawable.ic_launcher_background;
        this.ownerEmail = "";
        this.imageUri = "";
        this.rating = 0.0;
        this.ratingCount = 0;
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

    public String getTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return getDisplayName();
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
        return accessTime != null && !accessTime.isEmpty() ? accessTime : "24/7";
    }

    public void setAccessTime(String accessTime) {
        this.accessTime = accessTime;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
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
        return photoResId != 0 ? photoResId : R.drawable.ic_launcher_background;
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
     * Gets a formatted rating text string for UI display.
     *
     * @return Formatted rating or "Nowe miejsce" if unrated
     */
    public String getRatingDisplayText() {
        if (ratingCount == 0) {
            return "★ Nowe miejsce • Brak opinii";
        }
        return String.format(Locale.getDefault(), "★ %.1f (%d opinii)", rating, ratingCount);
    }

    /**
     * Helper method to get formatted display name.
     *
     * @return Formatted string with place number and street
     */
    public String getDisplayName() {
        if (street == null || street.isEmpty()) {
            return "Miejsce " + (placeNumber != null ? placeNumber : "");
        }
        return "Miejsce " + (placeNumber != null ? placeNumber : "") + " - " + street;
    }
}