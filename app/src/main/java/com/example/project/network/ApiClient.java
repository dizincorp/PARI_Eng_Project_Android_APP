package com.example.project.network;

import android.content.Context;

import com.example.project.utils.SessionManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton client providing configured Retrofit ApiService and SpotApiService instances
 * with automatic JWT authorization header injection via {@link AuthInterceptor}.
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
    private static ReservationApiService reservationApiService;
    private static VehicleApiService vehicleApiService;
    private static OkHttpClient okHttpClient;

    private ApiClient() {
    }

    /**
     * Initializes ApiClient with application context to ensure SessionManager is initialized.
     *
     * @param context Application context
     */
    public static synchronized void init(Context context) {
        if (context != null) {
            SessionManager.getInstance(context.getApplicationContext());
        }
    }

    /**
     * Gets or creates the configured singleton OkHttpClient with {@link AuthInterceptor} attached.
     *
     * @return Configured OkHttpClient instance
     */
    public static synchronized OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor())
                    .build();
        }
        return okHttpClient;
    }

    private static synchronized Retrofit getRetrofit() {
        if (retrofitInstance == null) {
            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(getOkHttpClient())
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
     * Gets the configured singleton instance of ReservationApiService (Reservations).
     *
     * @return Retrofit ReservationApiService instance
     */
    public static synchronized ReservationApiService getReservationApiService() {
        if (reservationApiService == null) {
            reservationApiService = getRetrofit().create(ReservationApiService.class);
        }
        return reservationApiService;
    }

    /**
     * Gets the configured singleton instance of VehicleApiService (Vehicles).
     *
     * @return Retrofit VehicleApiService instance
     */
    public static synchronized VehicleApiService getVehicleApiService() {
        if (vehicleApiService == null) {
            vehicleApiService = getRetrofit().create(VehicleApiService.class);
        }
        return vehicleApiService;
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
            reservationApiService = null;
            vehicleApiService = null;
        }
    }
}
