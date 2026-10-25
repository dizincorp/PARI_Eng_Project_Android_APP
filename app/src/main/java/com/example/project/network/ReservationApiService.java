package com.example.project.network;

import com.example.project.network.model.ReservationRequest;
import com.example.project.network.model.ReservationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit REST API interface defining parking reservation endpoints.
 */
public interface ReservationApiService {

    /**
     * Creates a new reservation on the backend service via POST /api/reservations.
     *
     * @param request ReservationRequest payload containing spot, vehicle, time interval, and payment token
     * @return Retrofit Call delivering the created {@link ReservationResponse}
     */
    @POST("/api/reservations")
    Call<ReservationResponse> createReservation(@Body ReservationRequest request);
}
