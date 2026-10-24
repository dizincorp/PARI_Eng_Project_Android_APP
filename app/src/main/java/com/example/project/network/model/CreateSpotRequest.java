package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing the JSON request body for creating
 * a new parking spot via POST /api/spots.
 */
public class CreateSpotRequest {

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

    @SerializedName("accessInstructions")
    private String accessInstructions;

    @SerializedName("autoAccept")
    private Boolean autoAccept;

    @SerializedName("amenities")
    private List<String> amenities;

    @SerializedName("photoKeys")
    private List<String> photoKeys;

    @SerializedName("ownershipDeclared")
    private Boolean ownershipDeclared;

    @SerializedName("requestVerification")
    private Boolean requestVerification;

    /**
     * Default no-argument constructor for Gson serialization.
     */
    public CreateSpotRequest() {
        this.country = "PL";
        this.autoAccept = true;
        this.ownershipDeclared = true;
        this.requestVerification = false;
        this.amenities = new ArrayList<>();
        this.photoKeys = new ArrayList<>();
    }

    /**
     * Parameterized constructor for CreateSpotRequest.
     *
     * @param name                Parking spot name
     * @param description         Spot description
     * @param street              Street address
     * @param city                City
     * @param postalCode          Postal code
     * @param country             ISO 2-letter country code (e.g., "PL")
     * @param lat                 Latitude coordinate
     * @param lng                 Longitude coordinate
     * @param lengthCm            Spot length in centimeters
     * @param widthCm             Spot width in centimeters
     * @param heightCm            Spot height in centimeters (optional)
     * @param hourlyPrice         Hourly price (e.g., 12.00)
     * @param accessInstructions  Access/entry instructions
     * @param autoAccept          Whether reservations are automatically accepted
     * @param amenities           List of amenities/tags (e.g., "CCTV", "LIGHTING")
     * @param photoKeys           List of photo storage keys
     * @param ownershipDeclared   Declaration of ownership confirmation (required)
     * @param requestVerification Whether to request administrative verification
     */
    public CreateSpotRequest(String name, String description, String street, String city,
                             String postalCode, String country, Double lat, Double lng,
                             Integer lengthCm, Integer widthCm, Integer heightCm, Double hourlyPrice,
                             String accessInstructions, Boolean autoAccept, List<String> amenities,
                             List<String> photoKeys, Boolean ownershipDeclared, Boolean requestVerification) {
        this.name = name;
        this.description = description;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = (country != null && !country.trim().isEmpty()) ? country.trim() : "PL";
        this.lat = lat;
        this.lng = lng;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.hourlyPrice = hourlyPrice;
        this.accessInstructions = accessInstructions;
        this.autoAccept = autoAccept != null ? autoAccept : true;
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
        this.ownershipDeclared = ownershipDeclared != null ? ownershipDeclared : true;
        this.requestVerification = requestVerification != null ? requestVerification : false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Integer getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(Integer lengthCm) {
        this.lengthCm = lengthCm;
    }

    public Integer getWidthCm() {
        return widthCm;
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

    public Double getHourlyPrice() {
        return hourlyPrice;
    }

    public void setHourlyPrice(Double hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    public String getAccessInstructions() {
        return accessInstructions;
    }

    public void setAccessInstructions(String accessInstructions) {
        this.accessInstructions = accessInstructions;
    }

    public Boolean getAutoAccept() {
        return autoAccept;
    }

    public void setAutoAccept(Boolean autoAccept) {
        this.autoAccept = autoAccept;
    }

    public List<String> getAmenities() {
        if (amenities == null) {
            amenities = new ArrayList<>();
        }
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
    }

    public List<String> getPhotoKeys() {
        if (photoKeys == null) {
            photoKeys = new ArrayList<>();
        }
        return photoKeys;
    }

    public void setPhotoKeys(List<String> photoKeys) {
        this.photoKeys = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
    }

    public Boolean getOwnershipDeclared() {
        return ownershipDeclared;
    }

    public void setOwnershipDeclared(Boolean ownershipDeclared) {
        this.ownershipDeclared = ownershipDeclared;
    }

    public Boolean getRequestVerification() {
        return requestVerification;
    }

    public void setRequestVerification(Boolean requestVerification) {
        this.requestVerification = requestVerification;
    }
}
