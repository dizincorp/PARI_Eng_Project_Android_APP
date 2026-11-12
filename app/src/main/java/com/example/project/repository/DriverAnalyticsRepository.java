package com.example.project.repository;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.DriverAnalyticsDao;
import com.example.project.database.entity.DriverAnalyticsEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.DriverApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.DriverAnalyticsResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating driver analytics discovery and historical parking breakdown.
 * Enforces the Single Source of Truth principle by observing local Room Database cache
 * and updating it from GET /api/driver/analytics.
 */
public class DriverAnalyticsRepository {

    private static final String TAG = "DriverAnalyticsRepo";
    private static DriverAnalyticsRepository instance;

    private final DriverApiService apiService;
    private final DriverAnalyticsDao dao;
    private final ExecutorService diskExecutor;

    private DriverAnalyticsRepository(@NonNull Context context) {
        this.apiService = ApiClient.getDriverApiService();
        this.dao = AppDatabase.getInstance(context.getApplicationContext()).driverAnalyticsDao();
        this.diskExecutor = Executors.newSingleThreadExecutor();
    }

    public static synchronized DriverAnalyticsRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new DriverAnalyticsRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Observes the cached driver analytics singleton entity from local Room Database.
     *
     * @return LiveData stream of DriverAnalyticsEntity
     */
    public LiveData<DriverAnalyticsEntity> getCachedAnalytics() {
        return dao.getDriverAnalyticsLiveData(DriverAnalyticsEntity.SINGLETON_ID);
    }

    /**
     * Fetches driver analytics from the remote backend with custom pagination and syncs to Room DB.
     *
     * @param page Zero-based page index
     * @param size Page size
     * @return LiveData emitting Resource containing DriverAnalyticsResponse
     */
    public LiveData<Resource<DriverAnalyticsResponse>> fetchDriverAnalytics(int page, int size) {
        MutableLiveData<Resource<DriverAnalyticsResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getDriverAnalytics(page, size).enqueue(new Callback<DriverAnalyticsResponse>() {
            @Override
            public void onResponse(@NonNull Call<DriverAnalyticsResponse> call,
                                   @NonNull Response<DriverAnalyticsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DriverAnalyticsResponse body = response.body();
                    diskExecutor.execute(() -> {
                        String summaryJson = body.getSummary() != null
                                ? ApiClient.getGson().toJson(body.getSummary())
                                : "{}";
                        String itemsJson = body.getItems() != null
                                ? ApiClient.getGson().toJson(body.getItems())
                                : "[]";

                        DriverAnalyticsEntity entity = new DriverAnalyticsEntity(
                                DriverAnalyticsEntity.SINGLETON_ID,
                                body.getTotalSpent(),
                                body.getTotalSaved(),
                                body.getTotalBookingsCount(),
                                body.getActiveBookingsCount(),
                                body.getLoyaltyPoints(),
                                body.getCurrency(),
                                summaryJson,
                                itemsJson,
                                System.currentTimeMillis()
                        );
                        dao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = "Błąd pobierania statystyk kierowcy: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.postValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<DriverAnalyticsResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "fetchDriverAnalytics failure: " + errorMsg, t);
                resultLiveData.postValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Fetches driver analytics using default pagination (page=0, size=20).
     *
     * @return LiveData emitting Resource containing DriverAnalyticsResponse
     */
    public LiveData<Resource<DriverAnalyticsResponse>> fetchDriverAnalytics() {
        return fetchDriverAnalytics(0, 20);
    }
}
