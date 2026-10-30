package com.example.project.network;

import com.example.project.network.model.OwnerAnalyticsResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit REST service defining operations for owner analytics and earnings metrics.
 */
public interface OwnerApiService {

    /**
     * Retrieves aggregated analytics and daily earnings breakdown for the authenticated owner.
     *
     * @return Retrofit Call delivering OwnerAnalyticsResponse
     */
    @GET("api/owner/analytics")
    Call<OwnerAnalyticsResponse> getOwnerAnalytics();
}
