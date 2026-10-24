package com.example.project.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.model.UserProfile;
import com.example.project.network.Resource;
import com.example.project.network.model.UserProfileResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.AuthRepository;

/**
 * ViewModel for user profile management supporting local Room cache and
 * remote profile synchronization via {@link AuthRepository}.
 */
public class ProfileViewModel extends ViewModel {

    private final AppRepository appRepository;
    private final AuthRepository authRepository;
    private final MediatorLiveData<Resource<UserProfileResponse>> remoteProfileLiveData = new MediatorLiveData<>();

    /**
     * Default constructor for ProfileViewModel using singleton repositories.
     */
    public ProfileViewModel() {
        this(AppRepository.getInstance(), AuthRepository.getInstance());
    }

    /**
     * Constructor allowing repository injection for testing.
     *
     * @param appRepository  Application central repository
     * @param authRepository Authentication network repository
     */
    public ProfileViewModel(@NonNull AppRepository appRepository, @NonNull AuthRepository authRepository) {
        this.appRepository = appRepository;
        this.authRepository = authRepository;
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
     * Updates user full name in local database and session.
     *
     * @param newName New full name
     */
    public void updateUserName(String newName) {
        appRepository.updateUserName(newName);
    }

    /**
     * Refreshes user profile directly from backend using automatic JWT Bearer authorization from AuthInterceptor.
     */
    public void fetchRemoteProfile() {
        remoteProfileLiveData.setValue(Resource.loading(null));
        LiveData<Resource<UserProfileResponse>> source = authRepository.getCurrentUser();
        remoteProfileLiveData.addSource(source, resource -> {
            remoteProfileLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    appRepository.loginUser(resource.data.getEmail(), resource.data.getFullName());
                }
                remoteProfileLiveData.removeSource(source);
            } else if (resource.status == Resource.Status.ERROR) {
                remoteProfileLiveData.removeSource(source);
            }
        });
    }

    /**
     * Refreshes user profile directly from backend via GET /api/auth/me using JWT Bearer token.
     *
     * @param token JWT authentication token
     */
    public void fetchRemoteProfile(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        remoteProfileLiveData.setValue(Resource.loading(null));
        LiveData<Resource<UserProfileResponse>> source = authRepository.getCurrentUser(token);
        remoteProfileLiveData.addSource(source, resource -> {
            remoteProfileLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    appRepository.loginUser(resource.data.getEmail(), resource.data.getFullName());
                }
                remoteProfileLiveData.removeSource(source);
            } else if (resource.status == Resource.Status.ERROR) {
                remoteProfileLiveData.removeSource(source);
            }
        });
    }
}
