package com.example.project.network;

import com.example.project.network.model.ParkingSpot;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit REST API interface defining parking spot discovery endpoints.
 */
public interface SpotApiService {

    /**
     * Searches for nearby parking spots matching geographical criteria and search radius.
     *
     * @param lat          Center latitude coordinate
     * @param lng          Center longitude coordinate
     * @param radiusMeters Search radius in meters (optional, default 5000)
     * @return Retrofit Call delivering a list of ParkingSpot items
     */
    @GET("api/spots")
    Call<List<ParkingSpot>> getSpots(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radiusMeters") Double radiusMeters
    );

    /**
     * Retrieves parking spots without query parameters.
     *
     * @return Retrofit Call delivering a list of ParkingSpot items
     */
    @GET("api/spots")
    Call<List<ParkingSpot>> getSpots();
}
