package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.entity.OwnerAnalyticsEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.OwnerAnalyticsResponse;
import com.example.project.repository.OwnerAnalyticsRepository;

/**
 * ViewModel managing the presentation state for owner financial analytics, KPI metrics,
 * and daily revenue breakdowns (GET /api/owner/analytics).
 * Enforces the Single Source of Truth architecture backed by Room Database.
 */
public class EarningsViewModel extends AndroidViewModel {

    private final OwnerAnalyticsRepository repository;

    private final LiveData<OwnerAnalyticsEntity> analyticsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);

    /**
     * Parameterized constructor providing application context.
     *
     * @param application Application context
     */
    public EarningsViewModel(@NonNull Application application) {
        super(application);
        this.repository = OwnerAnalyticsRepository.getInstance(application);
        this.analyticsLiveData = repository.getCachedAnalytics();

        // Initial fetch from remote backend
        refreshAnalytics();
    }

    /**
     * Exposes the observed LiveData from Room for owner analytics (SSOT).
     *
     * @return LiveData stream of OwnerAnalyticsEntity
     */
    public LiveData<OwnerAnalyticsEntity> getAnalyticsLiveData() {
        return analyticsLiveData;
    }

    /**
     * Refreshes analytics data from the remote backend via GET /api/owner/analytics.
     */
    public void refreshAnalytics() {
        isLoadingLiveData.setValue(true);
        LiveData<Resource<OwnerAnalyticsResponse>> remoteSource = repository.fetchOwnerAnalytics();
        remoteSource.observeForever(new androidx.lifecycle.Observer<Resource<OwnerAnalyticsResponse>>() {
            @Override
            public void onChanged(Resource<OwnerAnalyticsResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    isLoadingLiveData.setValue(false);
                    if (resource.status == Resource.Status.ERROR) {
                        errorMessageLiveData.setValue(resource.message);
                    } else {
                        errorMessageLiveData.setValue(null);
                    }
                    remoteSource.removeObserver(this);
                }
            }
        });
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
}
