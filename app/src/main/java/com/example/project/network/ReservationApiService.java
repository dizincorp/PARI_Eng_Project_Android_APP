package com.example.project.network;

import com.example.project.network.model.ReservationRequest;
import com.example.project.network.model.ReservationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Retrofit REST API interface defining parking reservation endpoints
 * for creating and retrieving driver reservations.
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

    /**
     * Retrieves the list of all reservations for the current authenticated user via GET /api/reservations.
     *
     * @return Retrofit Call delivering a list of {@link ReservationResponse}
     */
    @GET("/api/reservations")
    Call<List<ReservationResponse>> getReservations();

    /**
     * Retrieves incoming and active reservations for the current authenticated user via GET /api/reservations/incoming.
     *
     * @return Retrofit Call delivering a list of incoming {@link ReservationResponse}
     */
    @GET("/api/reservations/incoming")
    Call<List<ReservationResponse>> getIncomingReservations();
}
