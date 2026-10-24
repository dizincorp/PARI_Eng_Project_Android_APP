package com.example.project.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.network.ApiClient;
import com.example.project.network.Resource;
import com.example.project.network.SpotApiService;
import com.example.project.network.model.ParkingSpot;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository responsible for executing parking spot REST API requests via {@link SpotApiService}
 * and exposing reactive {@link LiveData} wrapped in {@link Resource} to ViewModels.
 */
public class ParkingSpotRepository {

    private static final String TAG = "ParkingSpotRepository";
    private static ParkingSpotRepository instance;

    private final SpotApiService apiService;

    /**
     * Private constructor initializing default SpotApiService from ApiClient.
     */
    private ParkingSpotRepository() {
        this(ApiClient.getSpotApiService());
    }

    /**
     * Constructor allowing custom SpotApiService injection for modularity and testing.
     *
     * @param apiService Retrofit SpotApiService instance
     */
    public ParkingSpotRepository(@NonNull SpotApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Gets the singleton instance of ParkingSpotRepository.
     *
     * @return Synchronized ParkingSpotRepository instance
     */
    public static synchronized ParkingSpotRepository getInstance() {
        if (instance == null) {
            instance = new ParkingSpotRepository();
        }
        return instance;
    }

    /**
     * Searches for parking spots by geographic coordinates and search radius.
     *
     * @param lat          Latitude coordinate
     * @param lng          Longitude coordinate
     * @param radiusMeters Search radius in meters
     * @return LiveData containing Resource with list of matching parking spots
     */
    public LiveData<Resource<List<ParkingSpot>>> searchSpots(double lat, double lng, Double radiusMeters) {
        MutableLiveData<Resource<List<ParkingSpot>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getSpots(lat, lng, radiusMeters).enqueue(new Callback<List<ParkingSpot>>() {
            @Override
            public void onResponse(@NonNull Call<List<ParkingSpot>> call,
                                   @NonNull Response<List<ParkingSpot>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Failed to fetch spots. HTTP status: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ParkingSpot>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                Log.e(TAG, "searchSpots network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }
}
