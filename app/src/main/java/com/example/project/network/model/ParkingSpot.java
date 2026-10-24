package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing a parking spot search result item returned by GET /api/spots.
 * Wraps the {@link SpotDetails} and distance relative to the query coordinates.
 */
public class ParkingSpot {

    @SerializedName("spot")
    private SpotDetails spot;

    @SerializedName("distanceMeters")
    private Double distanceMeters;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public ParkingSpot() {
        this.spot = new SpotDetails();
        this.distanceMeters = 0.0;
    }

    /**
     * Parameterized constructor for ParkingSpot.
     *
     * @param spot           Parking spot attributes and details
     * @param distanceMeters Distance in meters from query location
     */
    public ParkingSpot(SpotDetails spot, Double distanceMeters) {
        this.spot = spot != null ? spot : new SpotDetails();
        this.distanceMeters = distanceMeters != null ? distanceMeters : 0.0;
    }

    /**
     * Gets the inner parking spot details.
     *
     * @return SpotDetails instance
     */
    public SpotDetails getSpot() {
        return spot != null ? spot : new SpotDetails();
    }

    /**
     * Sets the inner parking spot details.
     *
     * @param spot SpotDetails instance
     */
    public void setSpot(SpotDetails spot) {
        this.spot = spot;
    }

    /**
     * Gets distance in meters from search coordinates.
     *
     * @return Distance in meters
     */
    public double getDistanceMeters() {
        return distanceMeters != null ? distanceMeters : 0.0;
    }

    /**
     * Sets distance in meters from search coordinates.
     *
     * @param distanceMeters Distance in meters
     */
    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    // Convenience delegation getters for direct access to underlying spot attributes

    public String getId() {
        return spot != null ? spot.getId() : "";
    }

    public String getOwnerId() {
        return spot != null ? spot.getOwnerId() : "";
    }

    public String getOwnerFirstName() {
        return spot != null ? spot.getOwnerFirstName() : "";
    }

    public String getName() {
        return spot != null ? spot.getName() : "";
    }

    public String getDescription() {
        return spot != null ? spot.getDescription() : "";
    }

    public String getStreet() {
        return spot != null ? spot.getStreet() : "";
    }

    public String getCity() {
        return spot != null ? spot.getCity() : "";
    }

    public String getPostalCode() {
        return spot != null ? spot.getPostalCode() : "";
    }

    public String getCountry() {
        return spot != null ? spot.getCountry() : "";
    }

    public double getLat() {
        return spot != null ? spot.getLat() : 0.0;
    }

    public double getLng() {
        return spot != null ? spot.getLng() : 0.0;
    }

    public int getLengthCm() {
        return spot != null ? spot.getLengthCm() : 0;
    }

    public int getWidthCm() {
        return spot != null ? spot.getWidthCm() : 0;
    }

    public Integer getHeightCm() {
        return spot != null ? spot.getHeightCm() : null;
    }

    public double getHourlyPrice() {
        return spot != null ? spot.getHourlyPrice() : 0.0;
    }

    public String getCurrency() {
        return spot != null ? spot.getCurrency() : "PLN";
    }

    public String getAccessInstructions() {
        return spot != null ? spot.getAccessInstructions() : "";
    }

    public boolean isAutoAccept() {
        return spot != null && spot.isAutoAccept();
    }

    public boolean isRealtimeStatusEnabled() {
        return spot != null && spot.isRealtimeStatusEnabled();
    }

    public String getOccupancy() {
        return spot != null ? spot.getOccupancy() : "FREE";
    }

    public boolean isFree() {
        return spot != null && spot.isFree();
    }

    public String getStatus() {
        return spot != null ? spot.getStatus() : "DRAFT";
    }

    public boolean isVerified() {
        return spot != null && spot.isVerified();
    }

    public double getAvgRating() {
        return spot != null ? spot.getAvgRating() : 0.0;
    }

    public int getReviewCount() {
        return spot != null ? spot.getReviewCount() : 0;
    }

    public List<String> getAmenities() {
        return spot != null ? spot.getAmenities() : Collections.emptyList();
    }

    public List<String> getPhotoKeys() {
        return spot != null ? spot.getPhotoKeys() : Collections.emptyList();
    }
}
