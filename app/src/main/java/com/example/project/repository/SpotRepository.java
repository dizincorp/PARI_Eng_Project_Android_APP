package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.R;
import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.Resource;
import com.example.project.network.SpotApiService;
import com.example.project.network.model.CreateSpotRequest;
import com.example.project.network.model.ParkingSpot;
import com.example.project.network.model.UpdateSpotRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth (SSOT) pattern for parking spots.
 * Synchronizes remote spots from {@link SpotApiService} into the local Room database,
 * manages CRUD lifecycle operations (activation, deactivation, update, deletion),
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
     * Exposes owned parking places for a specific owner email.
     *
     * @param ownerEmail Owner's email address
     * @return LiveData emitting cached list of ParkingPlaceEntity items
     */
    public LiveData<List<ParkingPlaceEntity>> getMyPlacesFromRoom(@Nullable String ownerEmail) {
        return parkingPlaceDao.getMyPlacesLiveData(ownerEmail != null ? ownerEmail : "");
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
     * Creates a new parking spot on the backend via POST /api/spots and caches it in Room DB.
     *
     * @param request    CreateSpotRequest payload
     * @param ownerEmail Owner email address for local account isolation
     * @return LiveData emitting Resource with created ParkingSpot
     */
    public LiveData<Resource<ParkingSpot>> createSpot(@NonNull CreateSpotRequest request, @Nullable String ownerEmail) {
        MutableLiveData<Resource<ParkingSpot>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.createSpot(request).enqueue(new Callback<ParkingSpot>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpot> call, @NonNull Response<ParkingSpot> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ParkingSpot spot = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity entity = toEntity(spot);
                        if (ownerEmail != null && !ownerEmail.isEmpty()) {
                            entity.setOwnerEmail(ownerEmail);
                        }
                        parkingPlaceDao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(spot));
                    });
                } else {
                    String errorMsg = "Błąd tworzenia miejsca: HTTP " + response.code();
                    if (response.code() == 409) {
                        errorMsg = "Wystąpił konflikt danych podczas tworzenia miejsca (409 Conflict).";
                    } else if (response.code() == 400) {
                        errorMsg = "Niepoprawne dane formularza dodawania miejsca.";
                    }
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpot> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "createSpot network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Activates a parking spot on the backend via POST /api/spots/{id}/activate and updates Room DB.
     *
     * @param id Unique spot identifier
     * @return LiveData emitting Resource with updated ParkingSpot
     */
    public LiveData<Resource<ParkingSpot>> activateSpot(@NonNull String id) {
        MutableLiveData<Resource<ParkingSpot>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (id == null || id.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator miejsca", null, 400));
            return resultLiveData;
        }

        apiService.activateSpot(id.trim()).enqueue(new Callback<ParkingSpot>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpot> call, @NonNull Response<ParkingSpot> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ParkingSpot spot = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity existing = parkingPlaceDao.getPlaceById(id.trim());
                        if (existing != null) {
                            existing.setStatus("ACTIVE");
                            parkingPlaceDao.insertOrUpdate(existing);
                        } else {
                            ParkingPlaceEntity entity = toEntity(spot);
                            entity.setStatus("ACTIVE");
                            parkingPlaceDao.insertOrUpdate(entity);
                        }
                        resultLiveData.postValue(Resource.success(spot));
                    });
                } else {
                    String errorMsg = "Błąd aktywacji miejsca: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpot> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "activateSpot network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Deactivates a parking spot on the backend via POST /api/spots/{id}/deactivate and updates Room DB.
     *
     * @param id Unique spot identifier
     * @return LiveData emitting Resource with updated ParkingSpot
     */
    public LiveData<Resource<ParkingSpot>> deactivateSpot(@NonNull String id) {
        MutableLiveData<Resource<ParkingSpot>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (id == null || id.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator miejsca", null, 400));
            return resultLiveData;
        }

        apiService.deactivateSpot(id.trim()).enqueue(new Callback<ParkingSpot>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpot> call, @NonNull Response<ParkingSpot> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ParkingSpot spot = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity existing = parkingPlaceDao.getPlaceById(id.trim());
                        if (existing != null) {
                            existing.setStatus("INACTIVE");
                            parkingPlaceDao.insertOrUpdate(existing);
                        } else {
                            ParkingPlaceEntity entity = toEntity(spot);
                            entity.setStatus("INACTIVE");
                            parkingPlaceDao.insertOrUpdate(entity);
                        }
                        resultLiveData.postValue(Resource.success(spot));
                    });
                } else {
                    String errorMsg = "Błąd dezaktywacji miejsca: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpot> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "deactivateSpot network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Updates an existing parking spot on the backend via PUT /api/spots/{id} and updates Room DB.
     *
     * @param id      Unique spot identifier
     * @param request UpdateSpotRequest payload
     * @return LiveData emitting Resource with updated ParkingSpot
     */
    public LiveData<Resource<ParkingSpot>> updateSpot(@NonNull String id, @NonNull UpdateSpotRequest request) {
        MutableLiveData<Resource<ParkingSpot>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (id == null || id.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator miejsca", null, 400));
            return resultLiveData;
        }

        apiService.updateSpot(id.trim(), request).enqueue(new Callback<ParkingSpot>() {
            @Override
            public void onResponse(@NonNull Call<ParkingSpot> call, @NonNull Response<ParkingSpot> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ParkingSpot spot = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity entity = toEntity(spot);
                        ParkingPlaceEntity existing = parkingPlaceDao.getPlaceById(id.trim());
                        if (existing != null && (entity.getOwnerEmail() == null || entity.getOwnerEmail().isEmpty())) {
                            entity.setOwnerEmail(existing.getOwnerEmail());
                        }
                        parkingPlaceDao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(spot));
                    });
                } else {
                    String errorMsg = "Błąd aktualizacji miejsca: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ParkingSpot> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "updateSpot network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Deletes a parking spot from the backend via DELETE /api/spots/{id}.
     * Upon successful HTTP response (200 / 204), deletes the spot from Room Database as required.
     *
     * @param id Parking spot unique identifier
     * @return LiveData emitting Resource with void result
     */
    public LiveData<Resource<Void>> deleteSpotRemote(@NonNull String id) {
        MutableLiveData<Resource<Void>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (id == null || id.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator miejsca", null, 400));
            return resultLiveData;
        }

        apiService.deleteSpot(id.trim()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    diskExecutor.execute(() -> {
                        parkingPlaceDao.deleteById(id.trim());
                        resultLiveData.postValue(Resource.success(null));
                    });
                } else {
                    String errorMsg = "Błąd usuwania miejsca: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "deleteSpotRemote network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Deletes a parking spot from Room Database locally.
     *
     * @param id Parking spot ID to delete
     */
    public void deleteSpot(@Nullable String id) {
        if (id != null && !id.isEmpty()) {
            diskExecutor.execute(() -> parkingPlaceDao.deleteById(id));
        }
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
                        String currentUserId = com.example.project.utils.SessionManager.getInstance() != null
                                ? com.example.project.utils.SessionManager.getInstance().getUserId() : "";
                        String currentEmail = com.example.project.utils.SessionManager.getInstance() != null
                                ? com.example.project.utils.SessionManager.getInstance().getUserEmail() : "";

                        List<ParkingPlaceEntity> entities = new ArrayList<>();
                        for (ParkingSpot spot : remoteSpots) {
                            if (spot != null) {
                                ParkingPlaceEntity entity = toEntity(spot);
                                if (currentUserId != null && !currentUserId.isEmpty() && currentUserId.equals(spot.getOwnerId())) {
                                    if (currentEmail != null && !currentEmail.isEmpty()) {
                                        entity.setOwnerEmail(currentEmail);
                                    }
                                }
                                entities.add(entity);
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
                    syncStatusLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ParkingSpot>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "fetchAndSaveSpots network failure: " + errorMsg, t);
                syncStatusLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });
    }

    /**
     * Fetches all spots owned by the authenticated user via GET /api/spots/mine
     * and synchronizes them into local Room database cache.
     *
     * @return LiveData emitting Resource with the list of owned ParkingSpot objects
     */
    public LiveData<Resource<List<ParkingSpot>>> fetchMySpots() {
        MutableLiveData<Resource<List<ParkingSpot>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getMySpots().enqueue(new Callback<List<ParkingSpot>>() {
            @Override
            public void onResponse(@NonNull Call<List<ParkingSpot>> call,
                                   @NonNull Response<List<ParkingSpot>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ParkingSpot> spots = response.body();
                    diskExecutor.execute(() -> {
                        String currentEmail = com.example.project.utils.SessionManager.getInstance() != null
                                ? com.example.project.utils.SessionManager.getInstance().getUserEmail() : "";
                        String currentUserId = com.example.project.utils.SessionManager.getInstance() != null
                                ? com.example.project.utils.SessionManager.getInstance().getUserId() : "";

                        List<ParkingPlaceEntity> entities = new ArrayList<>();
                        for (ParkingSpot s : spots) {
                            if (s == null) continue;
                            ParkingPlaceEntity entity = toEntity(s);
                            if (currentEmail != null && !currentEmail.isEmpty()) {
                                entity.setOwnerEmail(currentEmail);
                            }
                            entities.add(entity);
                        }
                        if (!entities.isEmpty()) {
                            parkingPlaceDao.insertOrUpdateAll(entities);
                        }
                        resultLiveData.postValue(Resource.success(spots));
                    });
                } else {
                    String errorMsg = "BÅ‚Ä…d pobierania moich miejsc: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ParkingSpot>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "BÅ‚Ä…d sieci";
                Log.e(TAG, "fetchMySpots network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
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
        entity.setDescription(spot.getDescription());
        entity.setAccessInstructions(spot.getAccessInstructions());
        entity.setAccessTime("24/7");
        entity.setOwnerEmail(spot.getOwnerId());
        entity.setStatus(spot.getStatus() != null && !spot.getStatus().isEmpty() ? spot.getStatus() : "ACTIVE");

        if (spot.getOccupancy() != null) {
            boolean isOcc = "OCCUPIED".equalsIgnoreCase(spot.getOccupancy());
            boolean isRes = "RESERVED".equalsIgnoreCase(spot.getOccupancy());
            entity.setOccupied(isOcc || isRes);
            entity.setCheckedIn(isOcc);
        }

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
        } else {
            entity.setImageUri("");
            entity.setPhotoResId(R.drawable.default_parking_spot);
        }

        return entity;
    }
}
