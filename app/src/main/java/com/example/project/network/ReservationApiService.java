package com.example.project.network;

import com.example.project.network.model.CancelReservationRequest;
import com.example.project.network.model.CheckInReservationRequest;
import com.example.project.network.model.CheckOutResponse;
import com.example.project.network.model.QuoteRequest;
import com.example.project.network.model.QuoteResponse;
import com.example.project.network.model.RejectReservationRequest;
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
 * for creating, retrieving, price quoting, and managing the entire lifecycle
 * (check-in, check-out, accept, reject, cancellation) of reservations.
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
     * Retrieves detailed information for a single reservation by its unique identifier via GET /api/reservations/{id}.
     *
     * @param id Reservation unique identifier
     * @return Retrofit Call delivering {@link ReservationResponse}
     */
    @GET("/api/reservations/{id}")
    Call<ReservationResponse> getReservationById(@Path("id") String id);

    /**
     * Retrieves the current active parking session for the authenticated driver via GET /api/reservations/active-session.
     *
     * @return Retrofit Call delivering the active {@link ReservationResponse}
     */
    @GET("/api/reservations/active-session")
    Call<ReservationResponse> getActiveSession();

    /**
     * Retrieves the current active parking session for the authenticated driver via POST /api/reservations/active-session.
     *
     * @return Retrofit Call delivering the active {@link ReservationResponse}
     */
    @POST("/api/reservations/active-session")
    Call<ReservationResponse> postActiveSession();

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

    /**
     * Completes parking session and checks out from the parking spot via POST /api/reservations/{id}/check-out
     * with an empty request body.
     *
     * @param id Reservation unique identifier
     * @return Retrofit Call delivering the finalized {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/check-out")
    Call<CheckOutResponse> checkOutReservation(@Path("id") String id);

    /**
     * Confirms and accepts a pending reservation by the spot owner via POST /api/reservations/{id}/accept
     * with an empty request body.
     *
     * @param id Reservation unique identifier
     * @return Retrofit Call delivering the accepted {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/accept")
    Call<ReservationResponse> acceptReservation(@Path("id") String id);

    /**
     * Rejects a pending reservation request by the spot owner via POST /api/reservations/{id}/reject
     * with an empty request body.
     *
     * @param id Reservation unique identifier
     * @return Retrofit Call delivering the rejected {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/reject")
    Call<ReservationResponse> rejectReservation(@Path("id") String id);

    /**
     * Rejects a pending reservation request by the spot owner via POST /api/reservations/{id}/reject
     * providing an explanation reason.
     *
     * @param id      Reservation unique identifier
     * @param request Payload containing rejection reason
     * @return Retrofit Call delivering the rejected {@link ReservationResponse}
     */
    @POST("/api/reservations/{id}/reject")
    Call<ReservationResponse> rejectReservationWithReason(@Path("id") String id, @Body RejectReservationRequest request);

    /**
     * Calculates preliminary price breakdown and fees for a potential reservation via POST /api/reservations/quote.
     *
     * @param request QuoteRequest payload containing spotId, startsAt, and endsAt
     * @return Retrofit Call delivering the calculated {@link QuoteResponse}
     */
    @POST("/api/reservations/quote")
    Call<QuoteResponse> calculateQuote(@Body QuoteRequest request);
}
