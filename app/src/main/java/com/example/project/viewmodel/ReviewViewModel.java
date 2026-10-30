package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.project.database.entity.ReviewEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.ReviewPageResponse;
import com.example.project.network.model.ReviewResponse;
import com.example.project.repository.ReviewRepository;

import java.util.List;

/**
 * ViewModel responsible for managing spot reviews and driver rating submissions.
 * Follows the Single Source of Truth pattern by observing persistent Room streams
 * and dispatching asynchronous network calls via {@link ReviewRepository}.
 */
public class ReviewViewModel extends AndroidViewModel {

    private final ReviewRepository reviewRepository;

    private final MutableLiveData<String> selectedSpotIdLiveData = new MutableLiveData<>();
    private final LiveData<List<ReviewEntity>> spotReviewsLiveData;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSubmittingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Resource<ReviewResponse>> submitReviewResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);

    /**
     * Parameterized constructor providing application context.
     *
     * @param application Application context
     */
    public ReviewViewModel(@NonNull Application application) {
        super(application);
        this.reviewRepository = ReviewRepository.getInstance(application);

        // SSOT: SwitchMap triggers Room query whenever selectedSpotIdLiveData updates
        this.spotReviewsLiveData = Transformations.switchMap(selectedSpotIdLiveData, spotId -> {
            if (spotId == null || spotId.trim().isEmpty()) {
                return new MutableLiveData<>();
            }
            return reviewRepository.getReviewsFromRoom(spotId.trim());
        });
    }

    /**
     * Selects a parking spot to observe its cached reviews and initiates a fetch.
     *
     * @param spotId Unique parking spot identifier
     */
    public void selectSpot(@Nullable String spotId) {
        if (spotId != null && !spotId.trim().isEmpty()) {
            selectedSpotIdLiveData.setValue(spotId.trim());
            loadSpotReviews(spotId.trim(), 0, 20);
        }
    }

    /**
     * Exposes the observed LiveData list of reviews from Room for the currently selected spot.
     *
     * @return LiveData list of ReviewEntity
     */
    public LiveData<List<ReviewEntity>> getSpotReviewsLiveData() {
        return spotReviewsLiveData;
    }

    /**
     * Gets a direct Room LiveData query stream for a specific parking spot.
     *
     * @param spotId Unique spot identifier
     * @return LiveData list of ReviewEntity
     */
    public LiveData<List<ReviewEntity>> getReviewsForSpot(@NonNull String spotId) {
        return reviewRepository.getReviewsFromRoom(spotId);
    }

    /**
     * Downloads paginated reviews from the backend via GET /api/spots/{spotId}/reviews
     * and synchronizes them into local Room database cache.
     *
     * @param spotId Unique spot identifier
     * @param page   Zero-based page index
     * @param size   Page size
     */
    public void loadSpotReviews(@NonNull String spotId, int page, int size) {
        isLoadingLiveData.setValue(true);
        LiveData<Resource<ReviewPageResponse>> repoSource = reviewRepository.fetchSpotReviews(spotId, page, size);
        repoSource.observeForever(new androidx.lifecycle.Observer<Resource<ReviewPageResponse>>() {
            @Override
            public void onChanged(Resource<ReviewPageResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    isLoadingLiveData.setValue(false);
                    if (resource.status == Resource.Status.ERROR) {
                        errorMessageLiveData.setValue(resource.message);
                    }
                    repoSource.removeObserver(this);
                }
            }
        });
    }

    /**
     * Submits a new driver rating and feedback review via POST /api/reservations/{id}/reviews.
     * Validates input constraints defensive before sending.
     *
     * @param reservationId Unique reservation identifier
     * @param spotId        Associated spot identifier (optional)
     * @param rating        Score between 1 and 5
     * @param comment       Optional review comment text
     */
    public void submitReview(@NonNull String reservationId,
                             @Nullable String spotId,
                             int rating,
                             @Nullable String comment) {
        if (reservationId == null || reservationId.trim().isEmpty()) {
            errorMessageLiveData.setValue("Brak identyfikatora rezerwacji");
            return;
        }

        if (rating < 1 || rating > 5) {
            errorMessageLiveData.setValue("Ocena musi wynosić od 1 do 5 gwiazdek");
            return;
        }

        isSubmittingLiveData.setValue(true);
        submitReviewResultLiveData.setValue(Resource.loading(null));

        LiveData<Resource<ReviewResponse>> repoSource =
                reviewRepository.createReview(reservationId.trim(), spotId, rating, comment);

        repoSource.observeForever(new androidx.lifecycle.Observer<Resource<ReviewResponse>>() {
            @Override
            public void onChanged(Resource<ReviewResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    isSubmittingLiveData.setValue(false);
                    submitReviewResultLiveData.setValue(resource);
                    if (resource.status == Resource.Status.ERROR) {
                        errorMessageLiveData.setValue(resource.message);
                    }
                    repoSource.removeObserver(this);
                }
            }
        });
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<Boolean> getIsSubmittingLiveData() {
        return isSubmittingLiveData;
    }

    public LiveData<Resource<ReviewResponse>> getSubmitReviewResultLiveData() {
        return submitReviewResultLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
}
