package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.project.model.UserProfile;
import com.example.project.model.Vehicle;
import com.example.project.network.Resource;
import com.example.project.network.model.CreateVehicleRequest;
import com.example.project.network.model.UserProfileResponse;
import com.example.project.network.model.VehicleResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.AuthRepository;
import com.example.project.repository.VehicleRepository;

import java.util.List;
import java.util.UUID;

/**
 * ViewModel for user profile and vehicles management supporting Room DB SSOT caching,
 * remote profile synchronization via /api/auth/me, and vehicles management via /api/vehicles.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final AppRepository appRepository;
    private final AuthRepository authRepository;
    private final VehicleRepository vehicleRepository;

    private final MediatorLiveData<Resource<UserProfileResponse>> remoteProfileLiveData = new MediatorLiveData<>();

    /**
     * Default constructor for ProfileViewModel using Android Application context.
     *
     * @param application Android Application instance
     */
    public ProfileViewModel(@NonNull Application application) {
        this(application,
                AppRepository.getInstance(),
                AuthRepository.getInstance(),
                VehicleRepository.getInstance(application));
    }

    /**
     * Parameterized constructor for dependency injection during testing.
     *
     * @param application       Android Application instance
     * @param appRepository     Application central repository
     * @param authRepository    Authentication network repository
     * @param vehicleRepository Vehicle network & Room repository
     */
    public ProfileViewModel(@NonNull Application application,
                            @NonNull AppRepository appRepository,
                            @NonNull AuthRepository authRepository,
                            @NonNull VehicleRepository vehicleRepository) {
        super(application);
        this.appRepository = appRepository;
        this.authRepository = authRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Gets LiveData of locally cached user profile.
     *
     * @return LiveData containing UserProfile
     */
    public LiveData<UserProfile> getUserProfileLiveData() {
        return appRepository.getUserProfileLiveData();
    }

    /**
     * Gets LiveData of remote profile synchronization state and response.
     *
     * @return LiveData containing Resource of UserProfileResponse
     */
    public LiveData<Resource<UserProfileResponse>> getRemoteProfileLiveData() {
        return remoteProfileLiveData;
    }

    /**
     * Gets reactive LiveData of user's registered vehicles.
     *
     * @return LiveData containing list of Vehicle items
     */
    public LiveData<List<Vehicle>> getVehiclesLiveData() {
        return appRepository.getVehiclesLiveData();
    }

    /**
     * Updates user full name in local database and session.
     *
     * @param newName New full name
     */
    public void updateUserName(String newName) {
        appRepository.updateUserName(newName);
    }

    /**
     * Updates user's first and last name in local database and session.
     *
     * @param firstName User first name
     * @param lastName  User last name
     */
    public void updateUserName(@NonNull String firstName, @NonNull String lastName) {
        appRepository.updateUserName(firstName, lastName);
    }

    /**
     * Updates user's contact phone number in local database and session.
     *
     * @param phone New phone number
     */
    public void updateUserPhone(@NonNull String phone) {
        appRepository.updateUserPhone(phone);
    }

    /**
     * Updates user's avatar image URI in local database and session.
     *
     * @param avatarUri Avatar URI string
     */
    public void updateUserAvatarUri(@NonNull String avatarUri) {
        appRepository.updateUserAvatarUri(avatarUri);
    }

    /**
     * Adds a new vehicle directly to the backend REST API via POST /api/vehicles
     * and caches it in local Room Database.
     *
     * @param model       Vehicle model
     * @param plateNumber License plate number
     * @return LiveData of vehicle creation Resource
     */
    public LiveData<Resource<VehicleResponse>> addVehicle(@NonNull String model, @NonNull String plateNumber) {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        CreateVehicleRequest request = new CreateVehicleRequest(model.trim(), plateNumber.trim());
        // Submit directly to REST API via VehicleRepository, which handles Room caching and prevents duplicates
        return vehicleRepository.createVehicle(request, currentEmail);
    }

    /**
     * Deletes a vehicle by its UUID via DELETE /api/vehicles/{id} and Room DB.
     *
     * @param vehicleId Unique vehicle UUID
     */
    public void deleteVehicle(@NonNull String vehicleId) {
        vehicleRepository.deleteVehicle(vehicleId);
        appRepository.deleteVehicle(vehicleId);
    }

    /**
     * Refreshes user profile directly from backend via GET /api/auth/me using JWT Bearer token.
     *
     * @param token JWT authentication token
     */
    public void fetchRemoteProfile(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        remoteProfileLiveData.setValue(Resource.loading(null));
        LiveData<Resource<UserProfileResponse>> source = authRepository.getCurrentUser(token);
        remoteProfileLiveData.addSource(source, resource -> {
            remoteProfileLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    appRepository.loginUser(resource.data.getEmail(), resource.data.getFullName(), resource.data.getId());
                }
                remoteProfileLiveData.removeSource(source);
            } else if (resource.status == Resource.Status.ERROR) {
                remoteProfileLiveData.removeSource(source);
            }
        });
    }
}
