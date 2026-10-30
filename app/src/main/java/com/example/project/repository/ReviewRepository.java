package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.dao.ReviewDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.ReviewEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.Resource;
import com.example.project.network.ReviewApiService;
import com.example.project.network.model.CreateReviewRequest;
import com.example.project.network.model.ReviewPageResponse;
import com.example.project.network.model.ReviewResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth (SSOT) pattern for spot reviews and driver ratings.
 * Coordinates network interactions with {@link ReviewApiService} and persistent caching via {@link ReviewDao}.
 */
public class ReviewRepository {

    private static final String TAG = "ReviewRepository";
    private static ReviewRepository instance;

    private final ReviewApiService apiService;
    private final ReviewDao reviewDao;
    private final ParkingPlaceDao parkingPlaceDao;
    private final ExecutorService diskExecutor;

    /**
     * Private constructor initializing dependencies from application context.
     *
     * @param context Application context
     */
    private ReviewRepository(@NonNull Context context) {
        this(
                ApiClient.getReviewApiService(),
                AppDatabase.getInstance(context).reviewDao(),
                AppDatabase.getInstance(context).parkingPlaceDao(),
                Executors.newSingleThreadExecutor()
        );
    }

    /**
     * Parameterized constructor allowing custom dependencies injection for testability.
     *
     * @param apiService      Retrofit ReviewApiService instance
     * @param reviewDao       Room database ReviewDao instance
     * @param parkingPlaceDao Room database ParkingPlaceDao instance
     * @param diskExecutor    Executor for asynchronous Room database writes
     */
    public ReviewRepository(@NonNull ReviewApiService apiService,
                            @NonNull ReviewDao reviewDao,
                            @NonNull ParkingPlaceDao parkingPlaceDao,
                            @NonNull ExecutorService diskExecutor) {
        this.apiService = apiService;
        this.reviewDao = reviewDao;
        this.parkingPlaceDao = parkingPlaceDao;
        this.diskExecutor = diskExecutor;
    }

    /**
     * Gets the singleton instance of ReviewRepository.
     *
     * @param context Application context
     * @return Synchronized ReviewRepository instance
     */
    public static synchronized ReviewRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ReviewRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Observes cached reviews for a specific parking spot directly from Room Database.
     *
     * @param spotId Unique parking spot identifier
     * @return LiveData list of cached ReviewEntity items
     */
    public LiveData<List<ReviewEntity>> getReviewsFromRoom(@NonNull String spotId) {
        return reviewDao.getReviewsBySpotIdLiveData(spotId);
    }

    /**
     * Submits a driver review for a completed parking reservation via POST /api/reservations/{id}/reviews.
     * Upon success, stores the created review in the local Room database cache and updates the spot's rating.
     *
     * @param reservationId Unique reservation identifier
     * @param spotId        Associated spot identifier (optional, for local association)
     * @param rating        Numerical rating between 1 and 5
     * @param comment       Optional feedback comment
     * @return LiveData emitting Resource with the created ReviewResponse
     */
    public LiveData<Resource<ReviewResponse>> createReview(@NonNull String reservationId,
                                                           @Nullable String spotId,
                                                           int rating,
                                                           @Nullable String comment) {
        MutableLiveData<Resource<ReviewResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        CreateReviewRequest request = new CreateReviewRequest(rating, comment);

        apiService.createReview(reservationId.trim(), request).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewResponse review = response.body();
                    diskExecutor.execute(() -> {
                        long timestamp = review.getCreatedAt() != null
                                ? review.getCreatedAt().getTime() : System.currentTimeMillis();
                        ReviewEntity entity = new ReviewEntity(
                                review.getId(),
                                spotId,
                                reservationId.trim(),
                                review.getAuthorName(),
                                review.getRating(),
                                review.getComment(),
                                timestamp
                        );
                        reviewDao.insertOrUpdate(entity);

                        // If spotId is available, recalculate and update spot rating in local cache
                        if (spotId != null && !spotId.trim().isEmpty()) {
                            ParkingPlaceEntity spot = parkingPlaceDao.getPlaceById(spotId.trim());
                            if (spot != null) {
                                int newCount = spot.getRatingCount() + 1;
                                double newRating = ((spot.getRating() * spot.getRatingCount()) + review.getRating()) / newCount;
                                spot.setRatingCount(newCount);
                                spot.setRating(Math.round(newRating * 10.0) / 10.0);
                                parkingPlaceDao.insertOrUpdate(spot);
                            }
                        }

                        resultLiveData.postValue(Resource.success(review));
                    });
                } else {
                    String errorMsg = "Błąd wysyłania opinii: HTTP " + response.code();
                    if (response.code() == 409) {
                        errorMsg = "Ta rezerwacja została już oceniona lub nie została jeszcze zakończona.";
                    } else if (response.code() == 400) {
                        errorMsg = "Niepoprawne dane oceny (wymagana ocena 1-5).";
                    }
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "createReview network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Downloads visible reviews for a parking spot from backend via GET /api/spots/{spotId}/reviews
     * and synchronizes them into local Room database cache.
     *
     * @param spotId Unique parking spot identifier
     * @param page   Zero-based page index (defaults to 0 if null)
     * @param size   Page size (defaults to 10 if null)
     * @return LiveData emitting Resource with paginated ReviewPageResponse
     */
    public LiveData<Resource<ReviewPageResponse>> fetchSpotReviews(@NonNull String spotId,
                                                                   @Nullable Integer page,
                                                                   @Nullable Integer size) {
        MutableLiveData<Resource<ReviewPageResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (spotId == null || spotId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator miejsca", null, 400));
            return resultLiveData;
        }

        int targetPage = page != null ? Math.max(0, page) : 0;
        int targetSize = size != null ? Math.max(1, Math.min(50, size)) : 10;

        apiService.getSpotReviews(spotId.trim(), targetPage, targetSize).enqueue(new Callback<ReviewPageResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewPageResponse> call,
                                   @NonNull Response<ReviewPageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewPageResponse pageResponse = response.body();
                    diskExecutor.execute(() -> {
                        List<ReviewEntity> entities = new ArrayList<>();
                        if (pageResponse.getContent() != null) {
                            for (ReviewResponse r : pageResponse.getContent()) {
                                if (r != null && r.getId() != null) {
                                    long timestamp = r.getCreatedAt() != null
                                            ? r.getCreatedAt().getTime() : System.currentTimeMillis();
                                    entities.add(new ReviewEntity(
                                            r.getId(),
                                            spotId.trim(),
                                            null,
                                            r.getAuthorName(),
                                            r.getRating(),
                                            r.getComment(),
                                            timestamp
                                    ));
                                }
                            }
                        }
                        if (!entities.isEmpty()) {
                            reviewDao.insertOrUpdateAll(entities);
                        }
                        resultLiveData.postValue(Resource.success(pageResponse));
                    });
                } else {
                    String errorMsg = "Błąd pobierania opinii: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewPageResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "fetchSpotReviews network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }
}
