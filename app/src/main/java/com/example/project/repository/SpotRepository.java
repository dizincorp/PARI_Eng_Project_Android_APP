package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.Resource;
import com.example.project.network.SpotApiService;
import com.example.project.network.model.ParkingSpot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth (SSOT) pattern for parking spots.
 * Synchronizes remote spots from {@link SpotApiService} into the local Room database
 * and exposes reactive {@link LiveData} directly from {@link ParkingPlaceDao}.
 */
public class SpotRepository {

    private static final String TAG = "SpotRepository";
    private static SpotRepository instance;

    private final SpotApiService apiService;
    private final ParkingPlaceDao parkingPlaceDao;
    private final ExecutorService diskExecutor;

    private final MutableLiveData<Resource<Void>> syncStatusLiveData = new MutableLiveData<>();

    /**
     * Private constructor initializing dependencies from application context.
     *
     * @param context Application context
     */
    private SpotRepository(@NonNull Context context) {
        this(
                ApiClient.getSpotApiService(),
                AppDatabase.getInstance(context).parkingPlaceDao(),
                Executors.newSingleThreadExecutor()
        );
    }

    /**
     * Parameterized constructor allowing custom dependencies injection for testability.
     *
     * @param apiService      Retrofit SpotApiService instance
     * @param parkingPlaceDao Room database ParkingPlaceDao instance
     * @param diskExecutor    Executor for asynchronous Room database writes
     */
    public SpotRepository(@NonNull SpotApiService apiService,
                          @NonNull ParkingPlaceDao parkingPlaceDao,
                          @NonNull ExecutorService diskExecutor) {
        this.apiService = apiService;
        this.parkingPlaceDao = parkingPlaceDao;
        this.diskExecutor = diskExecutor;
    }

    /**
     * Gets the singleton instance of SpotRepository.
     *
     * @param context Application context
     * @return Synchronized SpotRepository instance
     */
    public static synchronized SpotRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new SpotRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Exposes all parking places directly from Room Database as the Single Source of Truth.
     *
     * @return LiveData emitting cached list of ParkingPlaceEntity items
     */
    public LiveData<List<ParkingPlaceEntity>> getSpotsFromRoom() {
        return parkingPlaceDao.getAllPlacesLiveData();
    }

    /**
     * Gets the network sync operation status (loading, success, error).
     *
     * @return LiveData of sync status Resource
     */
    public LiveData<Resource<Void>> getSyncStatusLiveData() {
        return syncStatusLiveData;
    }

    /**
     * Downloads parking spots from backend REST API and saves them into Room Database in background.
     *
     * @param lat          Latitude coordinate
     * @param lng          Longitude coordinate
     * @param radiusMeters Search radius in meters (optional)
     */
    public void fetchAndSaveSpots(double lat, double lng, @Nullable Double radiusMeters) {
        syncStatusLiveData.setValue(Resource.loading(null));

        apiService.getSpots(lat, lng, radiusMeters).enqueue(new Callback<List<ParkingSpot>>() {
            @Override
            public void onResponse(@NonNull Call<List<ParkingSpot>> call,
                                   @NonNull Response<List<ParkingSpot>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ParkingSpot> remoteSpots = response.body();
                    diskExecutor.execute(() -> {
                        List<ParkingPlaceEntity> entities = new ArrayList<>();
                        for (ParkingSpot spot : remoteSpots) {
                            if (spot != null) {
                                entities.add(toEntity(spot));
                            }
                        }
                        if (!entities.isEmpty()) {
                            parkingPlaceDao.insertOrUpdateAll(entities);
                        }
                        syncStatusLiveData.postValue(Resource.success(null));
                    });
                } else {
                    String errorMsg = "Błąd pobierania miejsc: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    syncStatusLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ParkingSpot>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "fetchAndSaveSpots network failure: " + errorMsg, t);
                syncStatusLiveData.setValue(Resource.error(errorMsg, null));
            }
        });
    }

    /**
     * Converts network ParkingSpot DTO to local Room ParkingPlaceEntity.
     *
     * @param spot ParkingSpot DTO
     * @return Converted ParkingPlaceEntity instance
     */
    public static ParkingPlaceEntity toEntity(ParkingSpot spot) {
        if (spot == null) return null;
        ParkingPlaceEntity entity = new ParkingPlaceEntity();
        if (spot.getId() != null && !spot.getId().trim().isEmpty()) {
            entity.setId(spot.getId().trim());
        }
        entity.setTitle(spot.getName());
        entity.setStreet(spot.getStreet());
        entity.setCity(spot.getCity());
        entity.setPostalCode(spot.getPostalCode());
        entity.setPrice(spot.getHourlyPrice());
        entity.setLatitude(spot.getLat());
        entity.setLongitude(spot.getLng());
        entity.setOccupied(!spot.isFree());
        entity.setRating(spot.getAvgRating());
        entity.setRatingCount(spot.getReviewCount());
        entity.setAccessTime(spot.getAccessInstructions());
        entity.setOwnerEmail(spot.getOwnerId());

        if (spot.getAmenities() != null && !spot.getAmenities().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < spot.getAmenities().size(); i++) {
                sb.append(spot.getAmenities().get(i));
                if (i < spot.getAmenities().size() - 1) {
                    sb.append(",");
                }
            }
            entity.setAmenitiesCsv(sb.toString());
        }

        if (spot.getPhotoKeys() != null && !spot.getPhotoKeys().isEmpty()) {
            entity.setImageUri(spot.getPhotoKeys().get(0));
        }

        return entity;
    }
}
