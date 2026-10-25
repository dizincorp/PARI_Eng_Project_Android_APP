package com.example.project.network;

import com.example.project.network.model.CancelReservationRequest;
import com.example.project.network.model.CheckInReservationRequest;
import com.example.project.network.model.ReservationRequest;
import com.example.project.network.model.ReservationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit REST API interface defining parking reservation endpoints
 * for creating, retrieving, and managing the lifecycle (check-in, cancellation)
 * of driver reservations.
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

    /**
     * Cancels an existing reservation by its unique identifier via POST /api/reservations/{id}/cancel
     * with an empty request body.
     *
     * @param id Reservation unique identifier
     * @return Retrofit Call delivering the updated {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/cancel")
    Call<ReservationResponse> cancelReservation(@Path("id") String id);

    /**
     * Cancels an existing reservation by its unique identifier via POST /api/reservations/{id}/cancel
     * providing an optional cancellation reason.
     *
     * @param id      Reservation unique identifier
     * @param request Payload containing cancellation reason
     * @return Retrofit Call delivering the updated {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/cancel")
    Call<ReservationResponse> cancelReservationWithReason(@Path("id") String id, @Body CancelReservationRequest request);

    /**
     * Performs check-in to begin parking for the specified reservation via POST /api/reservations/{id}/check-in
     * with an empty request body.
     *
     * @param id Reservation unique identifier
     * @return Retrofit Call delivering the updated {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/check-in")
    Call<ReservationResponse> checkInReservation(@Path("id") String id);

    /**
     * Performs check-in to begin parking for the specified reservation via POST /api/reservations/{id}/check-in
     * providing validation credentials (PIN code or QR token).
     *
     * @param id      Reservation unique identifier
     * @param request Payload containing PIN code and QR token
     * @return Retrofit Call delivering the updated {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/check-in")
    Call<ReservationResponse> checkInReservationWithCredentials(@Path("id") String id, @Body CheckInReservationRequest request);
}
