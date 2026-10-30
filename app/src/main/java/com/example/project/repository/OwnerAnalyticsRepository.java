package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.OwnerAnalyticsDao;
import com.example.project.database.entity.OwnerAnalyticsEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.OwnerApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.OwnerAnalyticsResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth (SSOT) pattern for owner financial analytics
 * and occupancy metrics. Coordinates persistent caching via {@link OwnerAnalyticsDao}
 * and remote synchronization via {@link OwnerApiService}.
 */
public class OwnerAnalyticsRepository {

    private static final String TAG = "OwnerAnalyticsRepo";
    private static OwnerAnalyticsRepository instance;

    private final OwnerApiService apiService;
    private final OwnerAnalyticsDao analyticsDao;
    private final ExecutorService diskExecutor;

    /**
     * Private constructor initializing dependencies from application context.
     *
     * @param context Application context
     */
    private OwnerAnalyticsRepository(@NonNull Context context) {
        this(
                ApiClient.getOwnerApiService(),
                AppDatabase.getInstance(context).ownerAnalyticsDao(),
                Executors.newSingleThreadExecutor()
        );
    }

    /**
     * Parameterized constructor enabling dependency injection for testing.
     *
     * @param apiService   Retrofit OwnerApiService instance
     * @param analyticsDao Room database OwnerAnalyticsDao instance
     * @param diskExecutor Executor for asynchronous Room database writes
     */
    public OwnerAnalyticsRepository(@NonNull OwnerApiService apiService,
                                    @NonNull OwnerAnalyticsDao analyticsDao,
                                    @NonNull ExecutorService diskExecutor) {
        this.apiService = apiService;
        this.analyticsDao = analyticsDao;
        this.diskExecutor = diskExecutor;
    }

    /**
     * Gets the singleton instance of OwnerAnalyticsRepository.
     *
     * @param context Application context
     * @return Synchronized OwnerAnalyticsRepository instance
     */
    public static synchronized OwnerAnalyticsRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new OwnerAnalyticsRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Observes the cached owner analytics directly from Room Database as the Single Source of Truth.
     *
     * @return LiveData stream of OwnerAnalyticsEntity
     */
    public LiveData<OwnerAnalyticsEntity> getCachedAnalytics() {
        return analyticsDao.getAnalyticsLiveData(OwnerAnalyticsEntity.SINGLETON_ID);
    }

    /**
     * Dispatches a network request to GET /api/owner/analytics to fetch the latest
     * financial metrics, and writes the response into the local Room database cache.
     *
     * @return LiveData emitting Resource with the response
     */
    public LiveData<Resource<OwnerAnalyticsResponse>> fetchOwnerAnalytics() {
        MutableLiveData<Resource<OwnerAnalyticsResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getOwnerAnalytics().enqueue(new Callback<OwnerAnalyticsResponse>() {
            @Override
            public void onResponse(@NonNull Call<OwnerAnalyticsResponse> call,
                                   @NonNull Response<OwnerAnalyticsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OwnerAnalyticsResponse analytics = response.body();
                    diskExecutor.execute(() -> {
                        String breakdownJson = ApiClient.getGson().toJson(analytics.getDailyBreakdown());
                        OwnerAnalyticsEntity entity = new OwnerAnalyticsEntity(
                                OwnerAnalyticsEntity.SINGLETON_ID,
                                analytics.getTotalEarnings(),
                                analytics.getGrossEarnings(),
                                analytics.getEarningsChangePercent(),
                                analytics.getOccupancyRatePercent(),
                                analytics.getActiveSpotsCount(),
                                analytics.getTotalSpotsCount(),
                                analytics.getAverageHourlyRate(),
                                analytics.getCurrency(),
                                breakdownJson,
                                System.currentTimeMillis()
                        );
                        analyticsDao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(analytics));
                    });
                } else {
                    String errorMsg = "Błąd pobierania analityki: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<OwnerAnalyticsResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "fetchOwnerAnalytics failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }
}
