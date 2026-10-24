package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing a parking spot returned by the backend REST API.
 */
public class ParkingSpotResponse {

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

    /**
     * Default no-argument constructor for Gson serialization/deserialization.
     */
    public ParkingSpotResponse() {
        this.amenities = new ArrayList<>();
        this.photoKeys = new ArrayList<>();
    }

    /**
     * Parameterized constructor for ParkingSpotResponse.
     *
     * @param id                    Unique spot identifier (UUID)
     * @param ownerId               Unique owner identifier (UUID)
     * @param ownerFirstName        First name of the owner
     * @param name                  Name/title of the parking spot
     * @param description           Spot description
     * @param street                Street address
     * @param city                  City name
     * @param postalCode            Postal code
     * @param country               Country code or name
     * @param lat                   Latitude coordinate
     * @param lng                   Longitude coordinate
     * @param lengthCm              Spot length in centimeters
     * @param widthCm               Spot width in centimeters
     * @param heightCm              Spot height limit in centimeters (nullable)
     * @param hourlyPrice           Hourly rental rate
     * @param currency              Currency code (e.g. PLN)
     * @param accessInstructions   Access instructions visible to authorized users
     * @param autoAccept            Whether reservations are automatically accepted
     * @param realtimeStatusEnabled Whether IoT / real-time occupancy status is enabled
     * @param occupancy             Current occupancy state (e.g. "FREE", "OCCUPIED")
     * @param status                Lifecycle status (e.g. "DRAFT", "ACTIVE", "ARCHIVED")
     * @param verified              Whether the spot is officially verified
     * @param avgRating             Average user review rating
     * @param reviewCount           Total number of submitted reviews
     * @param amenities             List of amenities/tags
     * @param photoKeys             List of uploaded photo storage keys
     */
    public ParkingSpotResponse(String id, String ownerId, String ownerFirstName, String name,
                               String description, String street, String city, String postalCode,
                               String country, Double lat, Double lng, Integer lengthCm,
                               Integer widthCm, Integer heightCm, Double hourlyPrice,
                               String currency, String accessInstructions, Boolean autoAccept,
                               Boolean realtimeStatusEnabled, String occupancy, String status,
                               Boolean verified, Double avgRating, Integer reviewCount,
                               List<String> amenities, List<String> photoKeys) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerFirstName = ownerFirstName;
        this.name = name;
        this.description = description;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.lat = lat;
        this.lng = lng;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.hourlyPrice = hourlyPrice;
        this.currency = currency;
        this.accessInstructions = accessInstructions;
        this.autoAccept = autoAccept;
        this.realtimeStatusEnabled = realtimeStatusEnabled;
        this.occupancy = occupancy;
        this.status = status;
        this.verified = verified;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
    }

    /**
     * Gets the unique spot identifier.
     *
     * @return Spot identifier string
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique spot identifier.
     *
     * @param id Spot identifier string
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the owner identifier.
     *
     * @return Owner identifier string
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the owner identifier.
     *
     * @param ownerId Owner identifier string
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Gets the first name of the spot owner.
     *
     * @return Owner first name
     */
    public String getOwnerFirstName() {
        return ownerFirstName != null ? ownerFirstName : "";
    }

    /**
     * Sets the first name of the spot owner.
     *
     * @param ownerFirstName Owner first name
     */
    public void setOwnerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
    }

    /**
     * Gets the display name or title of the parking spot.
     *
     * @return Parking spot name
     */
    public String getName() {
        return name != null ? name : "";
    }

    /**
     * Sets the display name or title of the parking spot.
     *
     * @param name Parking spot name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the spot description.
     *
     * @return Spot description
     */
    public String getDescription() {
        return description != null ? description : "";
    }

    /**
     * Sets the spot description.
     *
     * @param description Spot description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the street address.
     *
     * @return Street address
     */
    public String getStreet() {
        return street != null ? street : "";
    }

    /**
     * Sets the street address.
     *
     * @param street Street address
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * Gets the city name.
     *
     * @return City name
     */
    public String getCity() {
        return city != null ? city : "";
    }

    /**
     * Sets the city name.
     *
     * @param city City name
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Gets the postal code.
     *
     * @return Postal code
     */
    public String getPostalCode() {
        return postalCode != null ? postalCode : "";
    }

    /**
     * Sets the postal code.
     *
     * @param postalCode Postal code
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Gets the country code or name.
     *
     * @return Country string
     */
    public String getCountry() {
        return country != null ? country : "";
    }

    /**
     * Sets the country code or name.
     *
     * @param country Country string
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Gets the latitude coordinate.
     *
     * @return Latitude value (or 0.0 if not provided)
     */
    public double getLat() {
        return lat != null ? lat : 0.0;
    }

    /**
     * Sets the latitude coordinate.
     *
     * @param lat Latitude value
     */
    public void setLat(Double lat) {
        this.lat = lat;
    }

    /**
     * Gets the longitude coordinate.
     *
     * @return Longitude value (or 0.0 if not provided)
     */
    public double getLng() {
        return lng != null ? lng : 0.0;
    }

    /**
     * Sets the longitude coordinate.
     *
     * @param lng Longitude value
     */
    public void setLng(Double lng) {
        this.lng = lng;
    }

    /**
     * Gets the length of the parking spot in centimeters.
     *
     * @return Spot length in cm
     */
    public int getLengthCm() {
        return lengthCm != null ? lengthCm : 0;
    }

    /**
     * Sets the length of the parking spot in centimeters.
     *
     * @param lengthCm Spot length in cm
     */
    public void setLengthCm(Integer lengthCm) {
        this.lengthCm = lengthCm;
    }

    /**
     * Gets the width of the parking spot in centimeters.
     *
     * @return Spot width in cm
     */
    public int getWidthCm() {
        return widthCm != null ? widthCm : 0;
    }

    /**
     * Sets the width of the parking spot in centimeters.
     *
     * @param widthCm Spot width in cm
     */
    public void setWidthCm(Integer widthCm) {
        this.widthCm = widthCm;
    }

    /**
     * Gets the height clearance limit of the spot in centimeters.
     *
     * @return Spot height limit in cm, or null if open/unrestricted
     */
    public Integer getHeightCm() {
        return heightCm;
    }

    /**
     * Sets the height clearance limit of the spot in centimeters.
     *
     * @param heightCm Spot height limit in cm
     */
    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    /**
     * Gets the hourly rental price in the specified currency.
     *
     * @return Hourly rental rate
     */
    public double getHourlyPrice() {
        return hourlyPrice != null ? hourlyPrice : 0.0;
    }

    /**
     * Sets the hourly rental price.
     *
     * @param hourlyPrice Hourly rental rate
     */
    public void setHourlyPrice(Double hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    /**
     * Gets the currency of the hourly price.
     *
     * @return Currency string (e.g. PLN)
     */
    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    /**
     * Sets the currency of the hourly price.
     *
     * @param currency Currency string
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Gets access instructions for reaching the spot.
     *
     * @return Access instructions string
     */
    public String getAccessInstructions() {
        return accessInstructions != null ? accessInstructions : "";
    }

    /**
     * Sets access instructions for reaching the spot.
     *
     * @param accessInstructions Access instructions string
     */
    public void setAccessInstructions(String accessInstructions) {
        this.accessInstructions = accessInstructions;
    }

    /**
     * Checks if reservations are automatically accepted.
     *
     * @return True if auto-accept is enabled, false otherwise
     */
    public boolean isAutoAccept() {
        return Boolean.TRUE.equals(autoAccept);
    }

    /**
     * Sets whether reservations are automatically accepted.
     *
     * @param autoAccept Auto-accept flag
     */
    public void setAutoAccept(Boolean autoAccept) {
        this.autoAccept = autoAccept;
    }

    /**
     * Checks if real-time occupancy status is enabled.
     *
     * @return True if real-time status is enabled, false otherwise
     */
    public boolean isRealtimeStatusEnabled() {
        return Boolean.TRUE.equals(realtimeStatusEnabled);
    }

    /**
     * Sets whether real-time occupancy status is enabled.
     *
     * @param realtimeStatusEnabled Real-time status flag
     */
    public void setRealtimeStatusEnabled(Boolean realtimeStatusEnabled) {
        this.realtimeStatusEnabled = realtimeStatusEnabled;
    }

    /**
     * Gets the current occupancy status (e.g. "FREE", "OCCUPIED").
     *
     * @return Occupancy string
     */
    public String getOccupancy() {
        return occupancy != null ? occupancy : "FREE";
    }

    /**
     * Sets the current occupancy status.
     *
     * @param occupancy Occupancy string
     */
    public void setOccupancy(String occupancy) {
        this.occupancy = occupancy;
    }

    /**
     * Checks if the spot is currently free.
     *
     * @return True if occupancy is "FREE"
     */
    public boolean isFree() {
        return "FREE".equalsIgnoreCase(occupancy);
    }

    /**
     * Gets the lifecycle status of the spot (e.g. "DRAFT", "ACTIVE", "ARCHIVED").
     *
     * @return Lifecycle status string
     */
    public String getStatus() {
        return status != null ? status : "DRAFT";
    }

    /**
     * Sets the lifecycle status of the spot.
     *
     * @param status Lifecycle status string
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if the spot is verified by administrators.
     *
     * @return True if verified, false otherwise
     */
    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }

    /**
     * Sets whether the spot is verified.
     *
     * @param verified Verification flag
     */
    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    /**
     * Gets the average rating of the spot.
     *
     * @return Average rating score
     */
    public double getAvgRating() {
        return avgRating != null ? avgRating : 0.0;
    }

    /**
     * Sets the average rating of the spot.
     *
     * @param avgRating Average rating score
     */
    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    /**
     * Gets the total number of user reviews.
     *
     * @return Review count
     */
    public int getReviewCount() {
        return reviewCount != null ? reviewCount : 0;
    }

    /**
     * Sets the total number of user reviews.
     *
     * @param reviewCount Review count
     */
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    /**
     * Gets the immutable list of amenities for the spot.
     *
     * @return List of amenities (never null)
     */
    public List<String> getAmenities() {
        if (amenities == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(amenities);
    }

    /**
     * Sets the list of amenities for the spot.
     *
     * @param amenities List of amenities
     */
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
    }

    /**
     * Gets the immutable list of photo storage keys.
     *
     * @return List of photo keys (never null)
     */
    public List<String> getPhotoKeys() {
        if (photoKeys == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(photoKeys);
    }

    /**
     * Sets the list of photo storage keys.
     *
     * @param photoKeys List of photo keys
     */
    public void setPhotoKeys(List<String> photoKeys) {
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
    }
}
