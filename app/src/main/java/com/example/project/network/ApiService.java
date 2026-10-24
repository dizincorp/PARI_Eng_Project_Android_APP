package com.example.project.network;

import com.example.project.network.model.ParkingSpotResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit REST API interface defining endpoints for communicating with the PARI backend service.
 */
public interface ApiService {

    /**
     * Searches for nearby parking spots matching geographical criteria and filters.
     *
     * @param lat          Center latitude coordinate
     * @param lng          Center longitude coordinate
     * @param radiusMeters Search radius in meters (e.g. 5000)
     * @param maxPrice     Maximum hourly rate filter (optional)
     * @param onlyVerified Filter to show only officially verified spots (optional)
     * @param start        Start time filter in ISO-8601 UTC format (optional)
     * @param end          End time filter in ISO-8601 UTC format (optional)
     * @param page         Zero-based page index (optional)
     * @param size         Page size (optional)
     * @return Retrofit Call delivering a list of matching parking spots
     */
    @GET("api/spots")
    Call<List<ParkingSpotResponse>> searchSpots(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radiusMeters") Double radiusMeters,
            @Query("maxPrice") Double maxPrice,
            @Query("onlyVerified") Boolean onlyVerified,
            @Query("start") String start,
            @Query("end") String end,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    /**
     * Retrieves all parking spots owned by the currently authenticated user.
     *
     * @return Retrofit Call delivering a list of owned parking spots
     */
    @GET("api/spots/mine")
    Call<List<ParkingSpotResponse>> getMySpots();

    /**
     * Retrieves detailed information for a specific parking spot by its unique identifier.
     *
     * @param spotId Unique spot identifier (UUID)
     * @return Retrofit Call delivering the parking spot details
     */
    @GET("api/spots/{id}")
    Call<ParkingSpotResponse> getSpotById(
            @Path("id") String spotId
    );

    /**
     * Creates a new parking spot on the backend service.
     *
     * @param request Payload containing parking spot creation parameters
     * @return Retrofit Call delivering the created parking spot details
     */
    @POST("api/spots")
    Call<ParkingSpotResponse> createSpot(
            @Body Object request
    );

    /**
     * Updates an existing parking spot's attributes.
     *
     * @param spotId  Unique spot identifier (UUID)
     * @param request Payload containing updated spot parameters
     * @return Retrofit Call delivering the updated parking spot details
     */
    @PUT("api/spots/{id}")
    Call<ParkingSpotResponse> updateSpot(
            @Path("id") String spotId,
            @Body Object request
    );

    /**
     * Archives / deletes a parking spot.
     *
     * @param spotId Unique spot identifier (UUID)
     * @return Retrofit Call delivering an empty response on success (HTTP 204)
     */
    @DELETE("api/spots/{id}")
    Call<Void> deleteSpot(
            @Path("id") String spotId
    );

    /**
     * Activates a parking spot to make it discoverable for reservations.
     *
     * @param spotId Unique spot identifier (UUID)
     * @return Retrofit Call delivering the activated spot details
     */
    @POST("api/spots/{id}/activate")
    Call<ParkingSpotResponse> activateSpot(
            @Path("id") String spotId
    );

    /**
     * Deactivates a parking spot so it cannot be reserved.
     *
     * @param spotId Unique spot identifier (UUID)
     * @return Retrofit Call delivering the deactivated spot details
     */
    @POST("api/spots/{id}/deactivate")
    Call<ParkingSpotResponse> deactivateSpot(
            @Path("id") String spotId
    );
}
