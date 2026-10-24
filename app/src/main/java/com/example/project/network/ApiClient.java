package com.example.project.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton client providing configured Retrofit ApiService and SpotApiService instances.
 */
public class ApiClient {

    /**
     * Base URL for the PARI backend service.
     * Uses 10.0.2.2 for Android Emulator connecting to host machine localhost:8080.
     */
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/";

    private static String baseUrl = DEFAULT_BASE_URL;
    private static Retrofit retrofitInstance;
    private static ApiService apiService;
    private static SpotApiService spotApiService;

    private ApiClient() {
    }

    private static synchronized Retrofit getRetrofit() {
        if (retrofitInstance == null) {
            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitInstance;
    }

    /**
     * Gets the configured singleton instance of ApiService (Authentication).
     *
     * @return Retrofit ApiService instance
     */
    public static synchronized ApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService.class);
        }
        return apiService;
    }

    /**
     * Gets the configured singleton instance of SpotApiService (Parking Spots).
     *
     * @return Retrofit SpotApiService instance
     */
    public static synchronized SpotApiService getSpotApiService() {
        if (spotApiService == null) {
            spotApiService = getRetrofit().create(SpotApiService.class);
        }
        return spotApiService;
    }

    /**
     * Sets a custom base URL (useful for physical test devices or staging environments).
     *
     * @param newBaseUrl New base URL ending with a trailing slash
     */
    public static synchronized void setBaseUrl(String newBaseUrl) {
        if (newBaseUrl != null && !newBaseUrl.trim().isEmpty()) {
            baseUrl = newBaseUrl;
            retrofitInstance = null;
            apiService = null;
            spotApiService = null;
        }
    }
}
