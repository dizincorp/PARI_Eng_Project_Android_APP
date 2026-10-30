package com.example.project.network;

import com.example.project.network.model.CreateSpotRequest;
import com.example.project.network.model.ParkingSpot;
import com.example.project.network.model.UpdateSpotRequest;

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
 * Retrofit REST API interface defining parking spot discovery, creation,
 * activation, deactivation, edition, and deletion endpoints.
 */
public interface SpotApiService {

    /**
     * Creates a new parking spot on the backend service.
     *
     * @param request CreateSpotRequest payload containing parking spot attributes
     * @return Retrofit Call delivering the created {@link ParkingSpot} POJO
     */
    @POST("api/spots")
    Call<ParkingSpot> createSpot(@Body CreateSpotRequest request);

    /**
     * Creates a new parking spot on the backend service using a ParkingSpot POJO.
     *
     * @param spot ParkingSpot payload
     * @return Retrofit Call delivering the created {@link ParkingSpot} POJO
     */
    @POST("api/spots")
    Call<ParkingSpot> createSpot(@Body ParkingSpot spot);

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

    /**
     * Retrieves all parking spots owned by the authenticated user via GET /api/spots/mine.
     * Uses automatic JWT Bearer token injection from AuthInterceptor.
     *
     * @return Retrofit Call delivering a list of {@link ParkingSpot} items owned by the current user
     */
    @GET("api/spots/mine")
    Call<List<ParkingSpot>> getMySpots();

    /**
     * Activates a parking spot via POST /api/spots/{id}/activate with an empty request body.
     *
     * @param id Parking spot unique identifier
     * @return Retrofit Call delivering the updated {@link ParkingSpot}
     */
    @POST("api/spots/{id}/activate")
    Call<ParkingSpot> activateSpot(@Path("id") String id);

    /**
     * Deactivates a parking spot via POST /api/spots/{id}/deactivate with an empty request body.
     *
     * @param id Parking spot unique identifier
     * @return Retrofit Call delivering the updated {@link ParkingSpot}
     */
    @POST("api/spots/{id}/deactivate")
    Call<ParkingSpot> deactivateSpot(@Path("id") String id);

    /**
     * Updates an existing parking spot via PUT /api/spots/{id}.
     *
     * @param id      Parking spot unique identifier
     * @param request UpdateSpotRequest payload containing modified attributes
     * @return Retrofit Call delivering the updated {@link ParkingSpot}
     */
    @PUT("api/spots/{id}")
    Call<ParkingSpot> updateSpot(@Path("id") String id, @Body UpdateSpotRequest request);

    /**
     * Deletes a parking spot via DELETE /api/spots/{id} with an empty request body.
     *
     * @param id Parking spot unique identifier
     * @return Retrofit Call delivering empty response body (HTTP 200/204)
     */
    @DELETE("api/spots/{id}")
    Call<Void> deleteSpot(@Path("id") String id);
}
