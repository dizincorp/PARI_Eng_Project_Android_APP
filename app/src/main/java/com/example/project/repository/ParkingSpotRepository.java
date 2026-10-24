package com.example.project.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.network.ApiClient;
import com.example.project.network.ApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.ParkingSpotResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository responsible for executing parking spot REST API requests via {@link ApiService}
 * and exposing reactive {@link LiveData} wrapped in {@link Resource} to ViewModels.
 */
public class ParkingSpotRepository {

    private static final String TAG = "ParkingSpotRepository";
    private static ParkingSpotRepository instance;

    private final ApiService apiService;

    /**
     * Private constructor initializing default ApiService from ApiClient.
     */
    private ParkingSpotRepository() {
        this(ApiClient.getApiService());
    }

    /**
     * Constructor allowing custom ApiService injection for modularity and testing.
     *
     * @param apiService Retrofit ApiService instance
     */
    public ParkingSpotRepository(@NonNull ApiService apiService) {
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
     * Searches for parking spots by geographic coordinates and optional filter criteria.
     *
     * @param lat          Latitude coordinate
     * @param lng          Longitude coordinate
     * @param radiusMeters Search radius in meters
     * @param maxPrice     Optional maximum hourly price
     * @param onlyVerified Optional filter for verified spots only
     * @param start        Optional start date-time (ISO-8601 UTC)
     * @param end          Optional end date-time (ISO-8601 UTC)
     * @param page         Optional zero-based page index
     * @param size         Optional page size
     * @return LiveData containing Resource with list of matching parking spots
     */
    public LiveData<Resource<List<ParkingSpotResponse>>> searchSpots(
            double lat,
            double lng,
            Double radiusMeters,
            Double maxPrice,
            Boolean onlyVerified,
            String start,
            String end,
            Integer page,
            Integer size) {

        MutableLiveData<Resource<List<ParkingSpotResponse>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.searchSpots(lat, lng, radiusMeters, maxPrice, onlyVerified, start, end, page, size)
                .enqueue(new Callback<List<ParkingSpotResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ParkingSpotResponse>> call,
                                           @NonNull Response<List<ParkingSpotResponse>> response) {
                        // Check if HTTP response is in 2xx range
                        if (response.isSuccessful() && response.body() != null) {
                            resultLiveData.setValue(Resource.success(response.body()));
                        } else {
                            String errorMsg = "Failed to fetch spots. HTTP status: " + response.code();
                            Log.e(TAG, errorMsg);
                            resultLiveData.setValue(Resource.error(errorMsg, null));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ParkingSpotResponse>> call,
                                          @NonNull Throwable t) {
                        String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                        Log.e(TAG, "searchSpots network failure: " + errorMsg, t);
                        resultLiveData.setValue(Resource.error(errorMsg, null));
                    }
                });

        return resultLiveData;
    }

    /**
     * Retrieves the list of parking spots owned by the currently authenticated user.
     *
     * @return LiveData containing Resource with list of owned spots
     */
    public LiveData<Resource<List<ParkingSpotResponse>>> getMySpots() {
        MutableLiveData<Resource<List<ParkingSpotResponse>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getMySpots().enqueue(new Callback<List<ParkingSpotResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ParkingSpotResponse>> call,
                                   @NonNull Response<List<ParkingSpotResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Failed to fetch user spots. HTTP status: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ParkingSpotResponse>> call,
                                  @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                Log.e(TAG, "getMySpots network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Retrieves detailed information of a parking spot by its unique identifier.
     *
     * @param spotId Unique spot identifier
     * @return LiveData containing Resource with the parking spot details
     */
    public LiveData<Resource<ParkingSpotResponse>> getSpotById(String spotId) {
        MutableLiveData<Resource<ParkingSpotResponse>> resultLiveData = new MutableLiveData<>();

        // Defensive validation against null or empty spot ID
        if (spotId == null || spotId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Spot ID cannot be empty", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));

        apiService.getSpotById(spotId.trim()).enqueue(new Callback<ParkingSpotResponse>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpotResponse> call,
                                   @NonNull Response<ParkingSpotResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Failed to fetch spot " + spotId + ". HTTP status: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpotResponse> call,
                                  @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                Log.e(TAG, "getSpotById network failure for " + spotId + ": " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Sends a request to create a new parking spot on the backend.
     *
     * @param request Payload containing parking spot attributes
     * @return LiveData containing Resource with the created spot
     */
    public LiveData<Resource<ParkingSpotResponse>> createSpot(Object request) {
        MutableLiveData<Resource<ParkingSpotResponse>> resultLiveData = new MutableLiveData<>();

        if (request == null) {
            resultLiveData.setValue(Resource.error("Spot request cannot be null", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));

        apiService.createSpot(request).enqueue(new Callback<ParkingSpotResponse>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpotResponse> call,
                                   @NonNull Response<ParkingSpotResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Failed to create spot. HTTP status: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpotResponse> call,
                                  @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                Log.e(TAG, "createSpot network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Sends a request to update an existing parking spot.
     *
     * @param spotId  Unique spot identifier
     * @param request Payload containing updated attributes
     * @return LiveData containing Resource with the updated spot
     */
    public LiveData<Resource<ParkingSpotResponse>> updateSpot(String spotId, Object request) {
        MutableLiveData<Resource<ParkingSpotResponse>> resultLiveData = new MutableLiveData<>();

        if (spotId == null || spotId.trim().isEmpty() || request == null) {
            resultLiveData.setValue(Resource.error("Spot ID and request cannot be null", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));

        apiService.updateSpot(spotId.trim(), request).enqueue(new Callback<ParkingSpotResponse>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpotResponse> call,
                                   @NonNull Response<ParkingSpotResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Failed to update spot " + spotId + ". HTTP status: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpotResponse> call,
                                  @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                Log.e(TAG, "updateSpot network failure for " + spotId + ": " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Deletes (archives) a parking spot by its identifier.
     *
     * @param spotId Unique spot identifier
     * @return LiveData containing Resource with boolean success flag
     */
    public LiveData<Resource<Boolean>> deleteSpot(String spotId) {
        MutableLiveData<Resource<Boolean>> resultLiveData = new MutableLiveData<>();

        if (spotId == null || spotId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Spot ID cannot be empty", false));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(false));

        apiService.deleteSpot(spotId.trim()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    resultLiveData.setValue(Resource.success(true));
                } else {
                    String errorMsg = "Failed to delete spot " + spotId + ". HTTP status: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, false));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network communication error";
                Log.e(TAG, "deleteSpot network failure for " + spotId + ": " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, false));
            }
        });

        return resultLiveData;
    }
}
