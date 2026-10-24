package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing a parking spot returned by the backend REST API
 * (e.g., POST /api/spots, GET /api/spots, and GET /api/spots/{id}).
 */
public class ParkingSpot {

    @SerializedName("id")
    private String id;

    @SerializedName("ownerId")
    private String ownerId;

    @SerializedName("ownerFirstName")
    private String ownerFirstName;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("street")
    private String street;

    @SerializedName("city")
    private String city;

    @SerializedName("postalCode")
    private String postalCode;

    @SerializedName("country")
    private String country;

    @SerializedName("lat")
    private Double lat;

    @SerializedName("lng")
    private Double lng;

    @SerializedName("lengthCm")
    private Integer lengthCm;

    @SerializedName("widthCm")
    private Integer widthCm;

    @SerializedName("heightCm")
    private Integer heightCm;

    @SerializedName("hourlyPrice")
    private Double hourlyPrice;

    @SerializedName("currency")
    private String currency;

    @SerializedName("accessInstructions")
    private String accessInstructions;

    @SerializedName("autoAccept")
    private Boolean autoAccept;

    @SerializedName("realtimeStatusEnabled")
    private Boolean realtimeStatusEnabled;

    @SerializedName("occupancy")
    private String occupancy;

    @SerializedName("status")
    private String status;

    @SerializedName("verified")
    private Boolean verified;

    @SerializedName("avgRating")
    private Double avgRating;

    @SerializedName("reviewCount")
    private Integer reviewCount;

    @SerializedName("amenities")
    private List<String> amenities;

    @SerializedName("photoKeys")
    private List<String> photoKeys;

    // Support for search hit response wrapper in GET /api/spots
    @SerializedName("spot")
    private SpotDetails spot;

    @SerializedName("distanceMeters")
    private Double distanceMeters;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public ParkingSpot() {
        this.occupancy = "FREE";
        this.status = "DRAFT";
        this.currency = "PLN";
        this.amenities = new ArrayList<>();
        this.photoKeys = new ArrayList<>();
    }

    /**
     * Parameterized constructor for ParkingSpot with wrapped SpotDetails.
     *
     * @param spot           Parking spot attributes and details
     * @param distanceMeters Distance in meters from query location
     */
    public ParkingSpot(SpotDetails spot, Double distanceMeters) {
        this();
        this.spot = spot != null ? spot : new SpotDetails();
        this.distanceMeters = distanceMeters != null ? distanceMeters : 0.0;
    }

    /**
     * Full parameterized constructor for ParkingSpot.
     */
    public ParkingSpot(String id, String ownerId, String ownerFirstName, String name,
                       String description, String street, String city, String postalCode,
                       String country, Double lat, Double lng, Integer lengthCm, Integer widthCm,
                       Integer heightCm, Double hourlyPrice, String currency, String accessInstructions,
                       Boolean autoAccept, Boolean realtimeStatusEnabled, String occupancy,
                       String status, Boolean verified, Double avgRating, Integer reviewCount,
                       List<String> amenities, List<String> photoKeys) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerFirstName = ownerFirstName;
        this.name = name;
        this.description = description;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country != null ? country : "PL";
        this.lat = lat;
        this.lng = lng;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.hourlyPrice = hourlyPrice;
        this.currency = currency != null ? currency : "PLN";
        this.accessInstructions = accessInstructions;
        this.autoAccept = autoAccept != null ? autoAccept : true;
        this.realtimeStatusEnabled = realtimeStatusEnabled != null ? realtimeStatusEnabled : false;
        this.occupancy = occupancy != null ? occupancy : "FREE";
        this.status = status != null ? status : "DRAFT";
        this.verified = verified != null ? verified : false;
        this.avgRating = avgRating != null ? avgRating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0;
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
    }

    public String getId() {
        if (id != null) return id;
        return spot != null ? spot.getId() : "";
    }

    public void setId(String id) {
        this.id = id;
        if (spot != null) spot.setId(id);
    }

    public String getOwnerId() {
        if (ownerId != null) return ownerId;
        return spot != null ? spot.getOwnerId() : "";
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        if (spot != null) spot.setOwnerId(ownerId);
    }

    public String getOwnerFirstName() {
        if (ownerFirstName != null) return ownerFirstName;
        return spot != null ? spot.getOwnerFirstName() : "";
    }

    public void setOwnerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
        if (spot != null) spot.setOwnerFirstName(ownerFirstName);
    }

    public String getName() {
        if (name != null) return name;
        return spot != null ? spot.getName() : "";
    }

    public void setName(String name) {
        this.name = name;
        if (spot != null) spot.setName(name);
    }

    public String getDescription() {
        if (description != null) return description;
        return spot != null ? spot.getDescription() : "";
    }

    public void setDescription(String description) {
        this.description = description;
        if (spot != null) spot.setDescription(description);
    }

    public String getStreet() {
        if (street != null) return street;
        return spot != null ? spot.getStreet() : "";
    }

    public void setStreet(String street) {
        this.street = street;
        if (spot != null) spot.setStreet(street);
    }

    public String getCity() {
        if (city != null) return city;
        return spot != null ? spot.getCity() : "";
    }

    public void setCity(String city) {
        this.city = city;
        if (spot != null) spot.setCity(city);
    }

    public String getPostalCode() {
        if (postalCode != null) return postalCode;
        return spot != null ? spot.getPostalCode() : "";
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        if (spot != null) spot.setPostalCode(postalCode);
    }

    public String getCountry() {
        if (country != null) return country;
        return spot != null ? spot.getCountry() : "PL";
    }

    public void setCountry(String country) {
        this.country = country;
        if (spot != null) spot.setCountry(country);
    }

    public double getLat() {
        if (lat != null) return lat;
        return spot != null ? spot.getLat() : 0.0;
    }

    public void setLat(Double lat) {
        this.lat = lat;
        if (spot != null) spot.setLat(lat);
    }

    public double getLng() {
        if (lng != null) return lng;
        return spot != null ? spot.getLng() : 0.0;
    }

    public void setLng(Double lng) {
        this.lng = lng;
        if (spot != null) spot.setLng(lng);
    }

    public int getLengthCm() {
        if (lengthCm != null) return lengthCm;
        return spot != null ? spot.getLengthCm() : 0;
    }

    public void setLengthCm(Integer lengthCm) {
        this.lengthCm = lengthCm;
        if (spot != null) spot.setLengthCm(lengthCm);
    }

    public int getWidthCm() {
        if (widthCm != null) return widthCm;
        return spot != null ? spot.getWidthCm() : 0;
    }

    public void setWidthCm(Integer widthCm) {
        this.widthCm = widthCm;
        if (spot != null) spot.setWidthCm(widthCm);
    }

    public Integer getHeightCm() {
        if (heightCm != null) return heightCm;
        return spot != null ? spot.getHeightCm() : null;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
        if (spot != null) spot.setHeightCm(heightCm);
    }

    public double getHourlyPrice() {
        if (hourlyPrice != null) return hourlyPrice;
        return spot != null ? spot.getHourlyPrice() : 0.0;
    }

    public void setHourlyPrice(Double hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
        if (spot != null) spot.setHourlyPrice(hourlyPrice);
    }

    public String getCurrency() {
        if (currency != null) return currency;
        return spot != null ? spot.getCurrency() : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
        if (spot != null) spot.setCurrency(currency);
    }

    public String getAccessInstructions() {
        if (accessInstructions != null) return accessInstructions;
        return spot != null ? spot.getAccessInstructions() : "";
    }

    public void setAccessInstructions(String accessInstructions) {
        this.accessInstructions = accessInstructions;
        if (spot != null) spot.setAccessInstructions(accessInstructions);
    }

    public boolean isAutoAccept() {
        if (autoAccept != null) return autoAccept;
        return spot != null && spot.isAutoAccept();
    }

    public void setAutoAccept(Boolean autoAccept) {
        this.autoAccept = autoAccept;
        if (spot != null) spot.setAutoAccept(autoAccept);
    }

    public boolean isRealtimeStatusEnabled() {
        if (realtimeStatusEnabled != null) return realtimeStatusEnabled;
        return spot != null && spot.isRealtimeStatusEnabled();
    }

    public void setRealtimeStatusEnabled(Boolean realtimeStatusEnabled) {
        this.realtimeStatusEnabled = realtimeStatusEnabled;
        if (spot != null) spot.setRealtimeStatusEnabled(realtimeStatusEnabled);
    }

    public String getOccupancy() {
        if (occupancy != null) return occupancy;
        return spot != null ? spot.getOccupancy() : "FREE";
    }

    public void setOccupancy(String occupancy) {
        this.occupancy = occupancy;
        if (spot != null) spot.setOccupancy(occupancy);
    }

    public boolean isFree() {
        return "FREE".equalsIgnoreCase(getOccupancy());
    }

    public String getStatus() {
        if (status != null) return status;
        return spot != null ? spot.getStatus() : "DRAFT";
    }

    public void setStatus(String status) {
        this.status = status;
        if (spot != null) spot.setStatus(status);
    }

    public boolean isVerified() {
        if (verified != null) return verified;
        return spot != null && spot.isVerified();
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
        if (spot != null) spot.setVerified(verified);
    }

    public double getAvgRating() {
        if (avgRating != null) return avgRating;
        return spot != null ? spot.getAvgRating() : 0.0;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
        if (spot != null) spot.setAvgRating(avgRating);
    }

    public int getReviewCount() {
        if (reviewCount != null) return reviewCount;
        return spot != null ? spot.getReviewCount() : 0;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
        if (spot != null) spot.setReviewCount(reviewCount);
    }

    public List<String> getAmenities() {
        if (amenities != null) return amenities;
        return spot != null ? spot.getAmenities() : Collections.emptyList();
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
        if (spot != null) spot.setAmenities(amenities);
    }

    public List<String> getPhotoKeys() {
        if (photoKeys != null) return photoKeys;
        return spot != null ? spot.getPhotoKeys() : Collections.emptyList();
    }

    public void setPhotoKeys(List<String> photoKeys) {
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
        if (spot != null) spot.setPhotoKeys(photoKeys);
    }

    public SpotDetails getSpot() {
        return spot != null ? spot : new SpotDetails();
    }

    public void setSpot(SpotDetails spot) {
        this.spot = spot;
    }

    public double getDistanceMeters() {
        return distanceMeters != null ? distanceMeters : 0.0;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}
