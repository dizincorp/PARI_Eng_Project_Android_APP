package com.example.project.network;

import com.example.project.network.model.CreateVehicleRequest;
import com.example.project.network.model.VehicleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit REST API interface defining driver's vehicle management endpoints.
 */
public interface VehicleApiService {

    /**
     * Creates a new vehicle for the authenticated driver via POST /api/vehicles.
     *
     * @param request CreateVehicleRequest payload containing model and plate number
     * @return Retrofit Call delivering the created {@link VehicleResponse}
     */
    @POST("api/vehicles")
    Call<VehicleResponse> createVehicle(@Body CreateVehicleRequest request);

    /**
     * Retrieves all vehicles associated with the authenticated driver via GET /api/vehicles.
     *
     * @return Retrofit Call delivering list of {@link VehicleResponse}
     */
    @GET("api/vehicles")
    Call<List<VehicleResponse>> getVehicles();

    /**
     * Deletes a vehicle by its UUID via DELETE /api/vehicles/{id}.
     *
     * @param id Vehicle UUID
     * @return Retrofit Call delivering empty response
     */
    @DELETE("api/vehicles/{id}")
    Call<Void> deleteVehicle(@Path("id") String id);
}
