package com.example.project.network.model;

import com.example.project.model.ParkingPlace;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing the JSON request body for updating
 * an existing parking spot via PUT /api/spots/{id}.
 */
public class UpdateSpotRequest {

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
    public UpdateSpotRequest() {
        this.country = "PL";
        this.autoAccept = true;
        this.ownershipDeclared = true;
        this.requestVerification = false;
        this.amenities = new ArrayList<>();
        this.photoKeys = new ArrayList<>();
    }

    /**
     * Parameterized constructor initializing all editable spot properties.
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
     * @param hourlyPrice         Hourly rental rate
     * @param accessInstructions  Access/entry instructions
     * @param autoAccept          Whether reservations are automatically accepted
     * @param amenities           List of amenities/tags
     * @param photoKeys           List of photo storage keys
     * @param ownershipDeclared   Declaration of ownership confirmation
     * @param requestVerification Whether to request administrative verification
     */
    public UpdateSpotRequest(String name, String description, String street, String city,
                             String postalCode, String country, Double lat, Double lng,
                             Integer lengthCm, Integer widthCm, Integer heightCm,
                             Double hourlyPrice, String accessInstructions, Boolean autoAccept,
                             List<String> amenities, List<String> photoKeys,
                             Boolean ownershipDeclared, Boolean requestVerification) {
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
        this.accessInstructions = accessInstructions;
        this.autoAccept = autoAccept != null ? autoAccept : true;
        this.amenities = amenities != null ? amenities : new ArrayList<>();
        this.photoKeys = photoKeys != null ? photoKeys : new ArrayList<>();
        this.ownershipDeclared = ownershipDeclared != null ? ownershipDeclared : true;
        this.requestVerification = requestVerification != null ? requestVerification : false;
    }

    /**
     * Factory method creating an UpdateSpotRequest from an existing ParkingPlace model.
     *
     * @param place Existing ParkingPlace domain model
     * @return Prepared UpdateSpotRequest instance
     */
    public static UpdateSpotRequest fromParkingPlace(ParkingPlace place) {
        if (place == null) return new UpdateSpotRequest();
        UpdateSpotRequest request = new UpdateSpotRequest();
        request.setName(place.getTitle());
        request.setStreet(place.getStreet());
        request.setCity(place.getCity());
        request.setPostalCode(place.getPostalCode());
        request.setHourlyPrice(place.getPrice());
        request.setLat(place.getLatitude());
        request.setLng(place.getLongitude());
        request.setAccessInstructions(place.getAccessTime());
        request.setAmenities(place.getAmenities() != null ? new ArrayList<>(place.getAmenities()) : new ArrayList<>());
        if (place.getImageUri() != null && !place.getImageUri().isEmpty()) {
            List<String> photos = new ArrayList<>();
            photos.add(place.getImageUri());
            request.setPhotoKeys(photos);
        }
        return request;
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
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public List<String> getPhotoKeys() {
        return photoKeys;
    }

    public void setPhotoKeys(List<String> photoKeys) {
        this.photoKeys = photoKeys;
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
