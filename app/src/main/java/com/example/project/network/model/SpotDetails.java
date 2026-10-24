package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing detailed parking spot attributes within {@link ParkingSpot}.
 */
public class SpotDetails {

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
     * Default no-argument constructor for Gson deserialization.
     */
    public SpotDetails() {
        this.amenities = new ArrayList<>();
        this.photoKeys = new ArrayList<>();
        this.occupancy = "FREE";
        this.status = "DRAFT";
        this.currency = "PLN";
    }

    /**
     * Parameterized constructor for SpotDetails.
     *
     * @param id                    Unique spot identifier (UUID)
     * @param ownerId               Unique owner identifier (UUID)
     * @param ownerFirstName        First name of the owner
     * @param name                  Display name of the spot
     * @param description           Detailed description
     * @param street                Street address
     * @param city                  City name
     * @param postalCode            Postal code
     * @param country               Country code
     * @param lat                   Latitude coordinate
     * @param lng                   Longitude coordinate
     * @param lengthCm              Length in centimeters
     * @param widthCm               Width in centimeters
     * @param heightCm              Height limit in centimeters
     * @param hourlyPrice           Hourly rental price
     * @param currency              Currency code
     * @param accessInstructions   Entry and access instructions
     * @param autoAccept            Whether reservations are auto-accepted
     * @param realtimeStatusEnabled Whether IoT occupancy detection is enabled
     * @param occupancy             Current occupancy state ("FREE", "OCCUPIED")
     * @param status                Spot status ("DRAFT", "ACTIVE", "ARCHIVED")
     * @param verified              Whether the spot is verified
     * @param avgRating             Average user rating score
     * @param reviewCount           Number of reviews
     * @param amenities             List of amenity tags
     * @param photoKeys             List of photo storage keys
     */
    public SpotDetails(String id, String ownerId, String ownerFirstName, String name,
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

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId != null ? ownerId : "";
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerFirstName() {
        return ownerFirstName != null ? ownerFirstName : "";
    }

    public void setOwnerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStreet() {
        return street != null ? street : "";
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city != null ? city : "";
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode != null ? postalCode : "";
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country != null ? country : "";
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLat() {
        return lat != null ? lat : 0.0;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng != null ? lng : 0.0;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public int getLengthCm() {
        return lengthCm != null ? lengthCm : 0;
    }

    public void setLengthCm(Integer lengthCm) {
        this.lengthCm = lengthCm;
    }

    public int getWidthCm() {
        return widthCm != null ? widthCm : 0;
    }

    public void setWidthCm(Integer widthCm) {
        this.widthCm = widthCm;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public double getHourlyPrice() {
        return hourlyPrice != null ? hourlyPrice : 0.0;
    }

    public void setHourlyPrice(Double hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    public String getCurrency() {
        return currency != null ? currency : "PLN";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAccessInstructions() {
        return accessInstructions != null ? accessInstructions : "";
    }

    public void setAccessInstructions(String accessInstructions) {
        this.accessInstructions = accessInstructions;
    }

    public boolean isAutoAccept() {
        return Boolean.TRUE.equals(autoAccept);
    }

    public void setAutoAccept(Boolean autoAccept) {
        this.autoAccept = autoAccept;
    }

    public boolean isRealtimeStatusEnabled() {
        return Boolean.TRUE.equals(realtimeStatusEnabled);
    }

    public void setRealtimeStatusEnabled(Boolean realtimeStatusEnabled) {
        this.realtimeStatusEnabled = realtimeStatusEnabled;
    }

    public String getOccupancy() {
        return occupancy != null ? occupancy : "FREE";
    }

    public void setOccupancy(String occupancy) {
        this.occupancy = occupancy;
    }

    public boolean isFree() {
        return "FREE".equalsIgnoreCase(occupancy);
    }

    public String getStatus() {
        return status != null ? status : "DRAFT";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public double getAvgRating() {
        return avgRating != null ? avgRating : 0.0;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public int getReviewCount() {
        return reviewCount != null ? reviewCount : 0;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public List<String> getAmenities() {
        if (amenities == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(amenities);
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
    }

    public List<String> getPhotoKeys() {
        if (photoKeys == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(photoKeys);
    }

    public void setPhotoKeys(List<String> photoKeys) {
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
    }
}
