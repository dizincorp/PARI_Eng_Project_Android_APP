package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.AvailabilityRuleDao;
import com.example.project.database.dao.BlockedPeriodDao;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.AvailabilityApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.network.model.AvailabilityRuleResponse;
import com.example.project.network.model.BlockedPeriodResponse;
import com.example.project.network.model.CreateBlockRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating spot availability rules and calendar blocked periods (F03).
 * Enforces the Single Source of Truth (SSOT) pattern with Room local cache and Retrofit REST API.
 */
public class AvailabilityRepository {

    private static final String TAG = "AvailabilityRepository";
    private static AvailabilityRepository instance;

    private final AvailabilityApiService apiService;
    private final AvailabilityRuleDao ruleDao;
    private final BlockedPeriodDao blockedPeriodDao;
    private final ExecutorService diskExecutor;

    /**
     * Private constructor initializing default singletons.
     *
     * @param context Application context
     */
    private AvailabilityRepository(@NonNull Context context) {
        this(
                ApiClient.getAvailabilityApiService(),
                AppDatabase.getInstance(context).availabilityRuleDao(),
                AppDatabase.getInstance(context).blockedPeriodDao(),
                Executors.newSingleThreadExecutor()
        );
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param apiService       AvailabilityApiService instance
     * @param ruleDao          AvailabilityRuleDao instance
     * @param blockedPeriodDao BlockedPeriodDao instance
     * @param diskExecutor     Executor for background Room database operations
     */
    public AvailabilityRepository(@NonNull AvailabilityApiService apiService,
                                  @NonNull AvailabilityRuleDao ruleDao,
                                  @NonNull BlockedPeriodDao blockedPeriodDao,
                                  @NonNull ExecutorService diskExecutor) {
        this.apiService = apiService;
        this.ruleDao = ruleDao;
        this.blockedPeriodDao = blockedPeriodDao;
        this.diskExecutor = diskExecutor;
    }

    /**
     * Gets or creates the singleton instance of AvailabilityRepository.
     *
     * @param context Application context
     * @return Singleton repository instance
     */
    public static synchronized AvailabilityRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new AvailabilityRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Returns a LiveData stream observing the local Room cached availability rules for a spot.
     *
     * @param spotId Unique spot identifier
     * @return LiveData list of {@link AvailabilityRuleEntity}
     */
    public LiveData<List<AvailabilityRuleEntity>> getRulesLiveData(@NonNull String spotId) {
        return ruleDao.getRulesForSpotLiveData(spotId);
    }

    /**
     * Returns a LiveData stream observing the local Room cached blocked periods for a spot.
     *
     * @param spotId Unique spot identifier
     * @return LiveData list of {@link BlockedPeriodEntity}
     */
    public LiveData<List<BlockedPeriodEntity>> getBlockedPeriodsLiveData(@NonNull String spotId) {
        return blockedPeriodDao.getBlockedPeriodsForSpotLiveData(spotId);
    }

    /**
     * Fetches blocked periods from GET /api/spots/{spotId}/blocked-periods
     * (falling back to GET /api/spots/{spotId}/calendar/blocks)
     * and synchronizes them into local Room database cache.
     *
     * @param spotId Unique spot identifier
     * @return LiveData emitting Resource with list of BlockedPeriodEntity
     */
    public LiveData<Resource<List<BlockedPeriodEntity>>> fetchBlockedPeriods(@NonNull String spotId) {
        MutableLiveData<Resource<List<BlockedPeriodEntity>>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        if (spotId == null || spotId.trim().isEmpty()) {
            result.postValue(Resource.error("Spot ID is empty", null));
            return result;
        }

        String cleanId = spotId.trim();

        apiService.getBlockedPeriods(cleanId).enqueue(new Callback<List<BlockedPeriodResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<BlockedPeriodResponse>> call,
                                   @NonNull Response<List<BlockedPeriodResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched " + response.body().size() + " blocked periods for spot: " + cleanId);
                    saveBlockedResponsesToRoom(cleanId, response.body(), result);
                } else {
                    Log.d(TAG, "GET blocked-periods returned HTTP " + response.code() + ", falling back to calendar/blocks for spot: " + cleanId);
                    fetchCalendarBlocksFallback(cleanId, result);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BlockedPeriodResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "GET blocked-periods failed: " + t.getMessage() + ", falling back to calendar/blocks for spot: " + cleanId);
                fetchCalendarBlocksFallback(cleanId, result);
            }
        });

        return result;
    }

    private void fetchCalendarBlocksFallback(@NonNull String spotId,
                                             @NonNull MutableLiveData<Resource<List<BlockedPeriodEntity>>> result) {
        apiService.getCalendarBlocks(spotId).enqueue(new Callback<List<BlockedPeriodResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<BlockedPeriodResponse>> call,
                                   @NonNull Response<List<BlockedPeriodResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched " + response.body().size() + " calendar blocks for spot: " + spotId);
                    saveBlockedResponsesToRoom(spotId, response.body(), result);
                } else {
                    String msg = "HTTP " + response.code();
                    Log.e(TAG, "GET calendar/blocks failed with HTTP " + response.code() + " for spot: " + spotId);
                    result.postValue(Resource.error(msg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BlockedPeriodResponse>> call, @NonNull Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "GET calendar/blocks network failure: " + msg + " for spot: " + spotId, t);
                result.postValue(Resource.error(msg, null));
            }
        });
    }

    private void saveBlockedResponsesToRoom(@NonNull String spotId,
                                            @NonNull List<BlockedPeriodResponse> responses,
                                            @NonNull MutableLiveData<Resource<List<BlockedPeriodEntity>>> result) {
        diskExecutor.execute(() -> {
            List<BlockedPeriodEntity> entities = new ArrayList<>();
            for (BlockedPeriodResponse resp : responses) {
                if (resp != null) {
                    BlockedPeriodEntity entity = mapResponseToEntity(spotId, resp);
                    if (!entity.isExpired()) {
                        entities.add(entity);
                    }
                }
            }
            blockedPeriodDao.replaceBlockedPeriodsForSpot(spotId, entities);
            result.postValue(Resource.success(entities));
        });
    }

    /**
     * Fetches weekly availability rules from GET /api/spots/{spotId}/availability-rules
     * and updates the local Room database cache.
     *
     * @param spotId Unique spot identifier
     * @return LiveData emitting Resource with the updated rules
     */
    public LiveData<Resource<List<AvailabilityRuleEntity>>> fetchAvailabilityRules(@NonNull String spotId) {
        MutableLiveData<Resource<List<AvailabilityRuleEntity>>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        apiService.getAvailabilityRules(spotId).enqueue(new Callback<List<AvailabilityRuleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilityRuleResponse>> call,
                                   @NonNull Response<List<AvailabilityRuleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AvailabilityRuleEntity> entities = mapResponsesToEntities(spotId, response.body());
                    diskExecutor.execute(() -> {
                        ruleDao.replaceRulesForSpot(spotId, entities);
                        result.postValue(Resource.success(entities));
                    });
                } else {
                    String errorMsg = "Failed to fetch availability rules: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    result.postValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AvailabilityRuleResponse>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error fetching rules";
                Log.e(TAG, errorMsg, t);
                result.postValue(Resource.error(errorMsg, null));
            }
        });

        return result;
    }

    /**
     * Replaces weekly availability rules via PUT /api/spots/{spotId}/availability-rules
     * and persists the updated rules in the local Room cache.
     *
     * @param spotId Unique spot identifier
     * @param rules  List of new {@link AvailabilityRuleRequest} rules
     * @return LiveData emitting Resource with the updated rules
     */
    public LiveData<Resource<List<AvailabilityRuleEntity>>> replaceAvailabilityRules(
            @NonNull String spotId,
            @NonNull List<AvailabilityRuleRequest> rules
    ) {
        MutableLiveData<Resource<List<AvailabilityRuleEntity>>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        apiService.replaceAvailabilityRules(spotId, rules).enqueue(new Callback<List<AvailabilityRuleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AvailabilityRuleResponse>> call,
                                   @NonNull Response<List<AvailabilityRuleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AvailabilityRuleEntity> entities = mapResponsesToEntities(spotId, response.body());
                    diskExecutor.execute(() -> {
                        ruleDao.replaceRulesForSpot(spotId, entities);
                        result.postValue(Resource.success(entities));
                    });
                } else {
                    String errorMsg = "Failed to replace availability rules: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    result.postValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AvailabilityRuleResponse>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error replacing rules";
                Log.e(TAG, errorMsg, t);
                result.postValue(Resource.error(errorMsg, null));
            }
        });

        return result;
    }

    /**
     * Creates a calendar block via POST /api/spots/{spotId}/calendar/blocks
     * and saves it to the local Room cache.
     *
     * @param spotId  Unique spot identifier
     * @param request Block parameters
     * @return LiveData emitting Resource with the created {@link BlockedPeriodEntity}
     */
    public LiveData<Resource<BlockedPeriodEntity>> createCalendarBlock(@NonNull String spotId,
                                                                      @NonNull CreateBlockRequest request) {
        MutableLiveData<Resource<BlockedPeriodEntity>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        apiService.createCalendarBlock(spotId, request).enqueue(new Callback<BlockedPeriodResponse>() {
            @Override
            public void onResponse(@NonNull Call<BlockedPeriodResponse> call,
                                   @NonNull Response<BlockedPeriodResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BlockedPeriodEntity entity = mapResponseToEntity(spotId, response.body());
                    diskExecutor.execute(() -> {
                        blockedPeriodDao.insert(entity);
                        result.postValue(Resource.success(entity));
                    });
                } else if (response.code() == 404 || response.code() == 405) {
                    // Try alternative endpoint POST /api/spots/{spotId}/blocked-periods
                    apiService.createBlockedPeriod(spotId, request).enqueue(new Callback<BlockedPeriodResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<BlockedPeriodResponse> call2,
                                               @NonNull Response<BlockedPeriodResponse> response2) {
                            if (response2.isSuccessful() && response2.body() != null) {
                                BlockedPeriodEntity entity2 = mapResponseToEntity(spotId, response2.body());
                                diskExecutor.execute(() -> {
                                    blockedPeriodDao.insert(entity2);
                                    result.postValue(Resource.success(entity2));
                                });
                            } else {
                                String errorMsg = formatError(response2.code());
                                Log.e(TAG, errorMsg);
                                result.postValue(Resource.error(errorMsg, null, response2.code()));
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<BlockedPeriodResponse> call2, @NonNull Throwable t2) {
                            String errorMsg = t2.getMessage() != null ? t2.getMessage() : "Network error";
                            Log.e(TAG, errorMsg, t2);
                            result.postValue(Resource.error(errorMsg, null, 0));
                        }
                    });
                } else {
                    String errorMsg = formatError(response.code());
                    Log.e(TAG, errorMsg);
                    result.postValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BlockedPeriodResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error creating calendar block";
                Log.e(TAG, errorMsg, t);
                result.postValue(Resource.error(errorMsg, null));
            }
        });

        return result;
    }

    /**
     * Creates a blocked period via POST /api/spots/{spotId}/blocked-periods
     * and saves it to the local Room cache.
     *
     * @param spotId  Unique spot identifier
     * @param request Block parameters
     * @return LiveData emitting Resource with the created {@link BlockedPeriodEntity}
     */
    public LiveData<Resource<BlockedPeriodEntity>> createBlockedPeriod(@NonNull String spotId,
                                                                      @NonNull CreateBlockRequest request) {
        MutableLiveData<Resource<BlockedPeriodEntity>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        apiService.createBlockedPeriod(spotId, request).enqueue(new Callback<BlockedPeriodResponse>() {
            @Override
            public void onResponse(@NonNull Call<BlockedPeriodResponse> call,
                                   @NonNull Response<BlockedPeriodResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BlockedPeriodEntity entity = mapResponseToEntity(spotId, response.body());
                    diskExecutor.execute(() -> {
                        blockedPeriodDao.insert(entity);
                        result.postValue(Resource.success(entity));
                    });
                } else {
                    String errorMsg = formatError(response.code());
                    Log.e(TAG, errorMsg);
                    result.postValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<BlockedPeriodResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error creating blocked period";
                Log.e(TAG, errorMsg, t);
                result.postValue(Resource.error(errorMsg, null, 0));
            }
        });

        return result;
    }

    private String formatError(int code) {
        if (code == 401) {
            return "Twoja sesja wygasła (HTTP 401). Zaloguj się ponownie.";
        } else if (code == 403) {
            return "Brak uprawnień (HTTP 403). Nie jesteś właścicielem tego miejsca.";
        } else {
            return "Błąd serwera: HTTP " + code;
        }
    }

    /**
     * Deletes a calendar block via DELETE /api/spots/{spotId}/calendar/blocks/{periodId}
     * and removes it from the local Room cache.
     *
     * @param spotId   Unique spot identifier
     * @param periodId Block period identifier
     * @return LiveData emitting Resource for operation state
     */
    public LiveData<Resource<Void>> deleteCalendarBlock(@NonNull String spotId, @NonNull String periodId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        apiService.deleteCalendarBlock(spotId, periodId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    diskExecutor.execute(() -> {
                        blockedPeriodDao.deleteById(periodId);
                        result.postValue(Resource.success(null));
                    });
                } else {
                    String errorMsg = "Failed to delete calendar block: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    result.postValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error deleting calendar block";
                Log.e(TAG, errorMsg, t);
                result.postValue(Resource.error(errorMsg, null));
            }
        });

        return result;
    }

    /**
     * Deletes a blocked period via DELETE /api/spots/{spotId}/blocked-periods/{periodId}
     * and removes it from the local Room cache.
     *
     * @param spotId   Unique spot identifier
     * @param periodId Block period identifier
     * @return LiveData emitting Resource for operation state
     */
    public LiveData<Resource<Void>> deleteBlockedPeriod(@NonNull String spotId, @NonNull String periodId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        apiService.deleteBlockedPeriod(spotId, periodId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    diskExecutor.execute(() -> {
                        blockedPeriodDao.deleteById(periodId);
                        result.postValue(Resource.success(null));
                    });
                } else {
                    String errorMsg = "Failed to delete blocked period: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    result.postValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Network error deleting blocked period";
                Log.e(TAG, errorMsg, t);
                result.postValue(Resource.error(errorMsg, null));
            }
        });

        return result;
    }

    /**
     * Helper mapper converting API rule responses into Room entities.
     *
     * @param spotId    Parent spot identifier
     * @param responses List of DTOs
     * @return List of {@link AvailabilityRuleEntity}
     */
    @NonNull
    private List<AvailabilityRuleEntity> mapResponsesToEntities(@NonNull String spotId,
                                                                @Nullable List<AvailabilityRuleResponse> responses) {
        List<AvailabilityRuleEntity> list = new ArrayList<>();
        if (responses == null) {
            return list;
        }
        for (AvailabilityRuleResponse resp : responses) {
            if (resp != null) {
                String id = resp.getId() != null ? resp.getId() : (spotId + "_" + resp.getDayOfWeek());
                list.add(new AvailabilityRuleEntity(
                        id,
                        spotId,
                        resp.getDayOfWeek(),
                        resp.getStartTime(),
                        resp.getEndTime(),
                        resp.getValidFrom(),
                        resp.getValidTo()
                ));
            }
        }
        return list;
    }

    /**
     * Helper mapper converting API blocked period response into Room entity.
     *
     * @param spotId Spot identifier
     * @param resp   DTO from API
     * @return {@link BlockedPeriodEntity}
     */
    @NonNull
    private BlockedPeriodEntity mapResponseToEntity(@NonNull String spotId,
                                                    @NonNull BlockedPeriodResponse resp) {
        return new BlockedPeriodEntity(
                resp.getId() != null ? resp.getId() : "",
                spotId,
                resp.getStart(),
                resp.getEnd(),
                resp.getReason()
        );
    }
}
