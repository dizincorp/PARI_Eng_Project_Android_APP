package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.entity.DriverAnalyticsEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.DriverAnalyticsResponse;
import com.example.project.repository.DriverAnalyticsRepository;

/**
 * ViewModel managing driver parking analytics, lifetime statistics, and recent activity
 * fetched from GET /api/driver/analytics and cached in Room Database.
 */
public class DriverAnalyticsViewModel extends AndroidViewModel {

    private final DriverAnalyticsRepository repository;
    private final LiveData<DriverAnalyticsEntity> analyticsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);

    public DriverAnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = DriverAnalyticsRepository.getInstance(application);
        this.analyticsLiveData = repository.getCachedAnalytics();

        // Refresh initially from backend
        refreshAnalytics(0, 20);
    }

    public LiveData<DriverAnalyticsEntity> getAnalyticsLiveData() {
        return analyticsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * Triggers remote analytics sync from GET /api/driver/analytics.
     *
     * @param page Zero-based page index
     * @param size Page size
     */
    public void refreshAnalytics(int page, int size) {
        isLoadingLiveData.setValue(true);
        LiveData<Resource<DriverAnalyticsResponse>> source = repository.fetchDriverAnalytics(page, size);
        source.observeForever(new androidx.lifecycle.Observer<Resource<DriverAnalyticsResponse>>() {
            @Override
            public void onChanged(Resource<DriverAnalyticsResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    isLoadingLiveData.setValue(false);
                    if (resource.status == Resource.Status.ERROR) {
                        errorMessageLiveData.setValue(resource.message);
                    } else {
                        errorMessageLiveData.setValue(null);
                    }
                    source.removeObserver(this);
                }
            }
        });
    }

    public void refreshAnalytics() {
        refreshAnalytics(0, 20);
    }
}
