package com.example.project.network;

import com.example.project.network.model.DriverAnalyticsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit REST API interface for driver dashboard metrics and historical parking analytics.
 */
public interface DriverApiService {

    /**
     * Retrieves aggregated statistics, savings, loyalty points, and paginated parking history
     * for the authenticated driver via GET /api/driver/analytics.
     *
     * @param page Zero-based page index (defaults to 0)
     * @param size Page size between 1 and 100 (defaults to 20)
     * @return Retrofit Call delivering {@link DriverAnalyticsResponse}
     */
    @GET("api/driver/analytics")
    Call<DriverAnalyticsResponse> getDriverAnalytics(
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Retrieves driver analytics with default pagination parameters (page=0, size=20).
     *
     * @return Retrofit Call delivering {@link DriverAnalyticsResponse}
     */
    @GET("api/driver/analytics")
    Call<DriverAnalyticsResponse> getDriverAnalytics();
}
