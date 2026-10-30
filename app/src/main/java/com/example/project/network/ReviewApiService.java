package com.example.project.network;

import com.example.project.network.model.CreateReviewRequest;
import com.example.project.network.model.ReviewPageResponse;
import com.example.project.network.model.ReviewResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit REST API interface for parking spot reviews and driver ratings.
 */
public interface ReviewApiService {

    /**
     * Submits a review and numerical rating (1-5 stars) for a completed parking reservation.
     * Requires valid Bearer authorization token.
     * Returns HTTP 201 Created on success, HTTP 409 Conflict if reservation is not completed
     * or was already reviewed.
     *
     * @param reservationId Unique reservation identifier
     * @param request       CreateReviewRequest with rating (1-5) and optional comment
     * @return Retrofit Call delivering the created {@link ReviewResponse}
     */
    @POST("api/reservations/{id}/reviews")
    Call<ReviewResponse> createReview(@Path("id") String reservationId,
                                      @Body CreateReviewRequest request);

    /**
     * Retrieves visible reviews for a specific parking spot, ordered newest first.
     * Supports pagination parameters page and size.
     *
     * @param spotId Unique parking spot identifier
     * @param page   Zero-based page index (defaults to 0 if null)
     * @param size   Page size (defaults to 10, maximum 50)
     * @return Retrofit Call delivering a paginated {@link ReviewPageResponse}
     */
    @GET("api/spots/{spotId}/reviews")
    Call<ReviewPageResponse> getSpotReviews(@Path("spotId") String spotId,
                                           @Query("page") Integer page,
                                           @Query("size") Integer size);
}
