package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.VehicleDao;
import com.example.project.database.entity.VehicleEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.Resource;
import com.example.project.network.VehicleApiService;
import com.example.project.network.model.CreateVehicleRequest;
import com.example.project.network.model.VehicleResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth pattern for driver vehicles.
 * Synchronizes vehicles between the backend {@link VehicleApiService} and local Room Database.
 */
public class VehicleRepository {

    private static final String TAG = "VehicleRepository";
    private static VehicleRepository instance;

    private final VehicleApiService apiService;
    private final VehicleDao vehicleDao;
    private final ExecutorService diskExecutor;

    private VehicleRepository(@NonNull Context context) {
        this(
                ApiClient.getVehicleApiService(),
                AppDatabase.getInstance(context).vehicleDao(),
                Executors.newSingleThreadExecutor()
        );
    }

    public VehicleRepository(@NonNull VehicleApiService apiService,
                             @NonNull VehicleDao vehicleDao,
                             @NonNull ExecutorService diskExecutor) {
        this.apiService = apiService;
        this.vehicleDao = vehicleDao;
        this.diskExecutor = diskExecutor;
    }

    public static synchronized VehicleRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new VehicleRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Exposes vehicle list from local Room database.
     *
     * @param ownerEmail User email
     * @return LiveData containing vehicle entities
     */
    public LiveData<List<VehicleEntity>> getVehiclesFromRoom(@NonNull String ownerEmail) {
        return vehicleDao.getUserVehiclesLiveData(ownerEmail);
    }

    /**
     * Submits a new vehicle to the backend REST API via POST /api/vehicles
     * and saves it to local Room Database upon success.
     *
     * @param request    CreateVehicleRequest payload containing model and plateNumber
     * @param ownerEmail User email address for local account isolation
     * @return LiveData emitting Resource containing VehicleResponse
     */
    public LiveData<Resource<VehicleResponse>> createVehicle(@NonNull CreateVehicleRequest request,
                                                            @Nullable String ownerEmail) {
        MutableLiveData<Resource<VehicleResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.createVehicle(request).enqueue(new Callback<VehicleResponse>() {
            @Override
            public void onResponse(@NonNull Call<VehicleResponse> call,
                                   @NonNull Response<VehicleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VehicleResponse vehicle = response.body();
                    diskExecutor.execute(() -> {
                        VehicleEntity entity = toEntity(vehicle, ownerEmail);
                        vehicleDao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(vehicle));
                    });
                } else {
                    String errorMsg = "Błąd dodawania pojazdu: HTTP " + response.code();
                    if (response.code() == 400) {
                        errorMsg = "Niepoprawny numer rejestracyjny lub model pojazdu.";
                    }
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VehicleResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "createVehicle network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Deletes a vehicle on the backend via DELETE /api/vehicles/{id} and deletes from Room DB.
     *
     * @param vehicleId Unique vehicle UUID
     */
    public void deleteVehicle(@Nullable String vehicleId) {
        if (vehicleId == null || vehicleId.isEmpty()) return;

        diskExecutor.execute(() -> vehicleDao.deleteById(vehicleId));

        apiService.deleteVehicle(vehicleId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "deleteVehicle backend returned HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "deleteVehicle network failure: " + t.getMessage(), t);
            }
        });
    }

    /**
     * Fetches driver's vehicles from the backend via GET /api/vehicles and caches them in Room DB.
     *
     * @param ownerEmail Owner email address for local account isolation
     */
    public void fetchVehicles(@Nullable String ownerEmail) {
        apiService.getVehicles().enqueue(new Callback<List<VehicleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<VehicleResponse>> call,
                                   @NonNull Response<List<VehicleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VehicleResponse> list = response.body();
                    diskExecutor.execute(() -> {
                        for (VehicleResponse vr : list) {
                            if (vr != null) {
                                vehicleDao.insertOrUpdate(toEntity(vr, ownerEmail));
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VehicleResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchVehicles network failure: " + t.getMessage(), t);
            }
        });
    }

    /**
     * Converts network VehicleResponse DTO to local Room VehicleEntity.
     *
     * @param vr         VehicleResponse DTO
     * @param ownerEmail User email
     * @return Converted VehicleEntity
     */
    public static VehicleEntity toEntity(VehicleResponse vr, String ownerEmail) {
        VehicleEntity entity = new VehicleEntity();
        if (vr.getId() != null && !vr.getId().isEmpty()) {
            entity.setId(vr.getId());
        }
        entity.setOwnerEmail(ownerEmail != null ? ownerEmail : "");
        entity.setMakeAndModel(vr.getModel());
        entity.setLicensePlate(vr.getPlateNumber());
        entity.setColor("");
        return entity;
    }
}
