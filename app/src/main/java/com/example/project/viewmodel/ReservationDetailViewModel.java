package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.project.R;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.ReservationResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReservationRepository;

/**
 * ViewModel managing reservation details and individual lifecycle operations
 * (cancellation, check-in) for a specific parking reservation.
 * Follows the Single Source of Truth principle via Room and Retrofit.
 */
public class ReservationDetailViewModel extends AndroidViewModel {

    private final ReservationRepository reservationRepository;
    private final AppRepository appRepository;

    private final MutableLiveData<Resource<ReservationResponse>> actionStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<ReservationResponse> updatedReservationLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<String> actionErrorLiveData = new MutableLiveData<>(null);

    /**
     * Default constructor for ReservationDetailViewModel.
     *
     * @param application Application context
     */
    public ReservationDetailViewModel(@NonNull Application application) {
        this(application, ReservationRepository.getInstance(application), AppRepository.getInstance());
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param application           Application context
     * @param reservationRepository ReservationRepository instance
     * @param appRepository         AppRepository instance
     */
    public ReservationDetailViewModel(@NonNull Application application,
                                      @NonNull ReservationRepository reservationRepository,
                                      @NonNull AppRepository appRepository) {
        super(application);
        this.reservationRepository = reservationRepository;
        this.appRepository = appRepository;
    }

    /**
     * Gets a reactive LiveData stream for observing a single reservation by ID from Room.
     *
     * @param id Reservation unique identifier
     * @return LiveData emitting the ReservationEntity from local database
     */
    public LiveData<ReservationEntity> getReservationById(@NonNull String id) {
        return reservationRepository.getReservationByIdLiveData(id);
    }

    /**
     * LiveData tracking the status (Loading, Success, Error) of the latest lifecycle action.
     *
     * @return LiveData containing Resource with ReservationResponse
     */
    public LiveData<Resource<ReservationResponse>> getActionStatusLiveData() {
        return actionStatusLiveData;
    }

    /**
     * LiveData emitting user-facing feedback messages intended to be displayed in a Toast.
     *
     * @return LiveData string message
     */
    public LiveData<String> getToastMessageLiveData() {
        return toastMessageLiveData;
    }

    /**
     * LiveData emitting the updated {@link ReservationResponse} after HTTP 200 success.
     *
     * @return LiveData containing the latest ReservationResponse
     */
    public LiveData<ReservationResponse> getUpdatedReservationLiveData() {
        return updatedReservationLiveData;
    }

    /**
     * LiveData emitting user-facing error descriptions when an action fails.
     *
     * @return LiveData string error message
     */
    public LiveData<String> getActionErrorLiveData() {
        return actionErrorLiveData;
    }

    /**
     * Cancels the reservation via POST /api/reservations/{id}/cancel.
     * On HTTP 200 success, notifies the View via {@link #getUpdatedReservationLiveData()} and
     * {@link #getToastMessageLiveData()} and updates local Room cache.
     * On error (e.g. HTTP 409 when parking has already begun), notifies via {@link #getActionErrorLiveData()}
     * and {@link #getToastMessageLiveData()}.
     *
     * @param id Unique identifier of the reservation to cancel
     */
    public void cancelReservation(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            String validationMsg = getApplication().getString(R.string.toast_cancel_failed);
            actionErrorLiveData.setValue(validationMsg);
            toastMessageLiveData.setValue(validationMsg);
            return;
        }

        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        actionStatusLiveData.setValue(Resource.loading(null));

        LiveData<Resource<ReservationResponse>> source = reservationRepository.cancelReservation(id.trim(), currentEmail);
        source.observeForever(new Observer<Resource<ReservationResponse>>() {
            @Override
            public void onChanged(Resource<ReservationResponse> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    actionStatusLiveData.setValue(resource);

                    if (resource.isSuccess() && resource.getData() != null) {
                        ReservationResponse updated = resource.getData();
                        updatedReservationLiveData.setValue(updated);
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_cancel_success));
                    } else if (resource.isError()) {
                        String errorMsg = resource.getMessage();
                        if (resource.getCode() == 409 || resource.getCode() == 400) {
                            errorMsg = getApplication().getString(R.string.error_cannot_cancel_started);
                        } else if (errorMsg == null || errorMsg.trim().isEmpty()) {
                            errorMsg = getApplication().getString(R.string.toast_cancel_failed);
                        }
                        actionErrorLiveData.setValue(errorMsg);
                        toastMessageLiveData.setValue(errorMsg);
                    }
                }
            }
        });
    }

    /**
     * Performs check-in to begin parking session via POST /api/reservations/{id}/check-in.
     * On HTTP 200 success, notifies the View via {@link #getUpdatedReservationLiveData()} and
     * {@link #getToastMessageLiveData()} and updates local Room cache.
     * On error (e.g. HTTP 400 before start time or 409 if already checked-in), notifies via
     * {@link #getActionErrorLiveData()} and {@link #getToastMessageLiveData()}.
     *
     * @param id Unique identifier of the reservation to check in
     */
    public void checkIn(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            String validationMsg = getApplication().getString(R.string.toast_checkin_failed);
            actionErrorLiveData.setValue(validationMsg);
            toastMessageLiveData.setValue(validationMsg);
            return;
        }

        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        actionStatusLiveData.setValue(Resource.loading(null));

        LiveData<Resource<ReservationResponse>> source = reservationRepository.checkInReservation(id.trim(), currentEmail);
        source.observeForever(new Observer<Resource<ReservationResponse>>() {
            @Override
            public void onChanged(Resource<ReservationResponse> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    actionStatusLiveData.setValue(resource);

                    if (resource.isSuccess() && resource.getData() != null) {
                        ReservationResponse updated = resource.getData();
                        updatedReservationLiveData.setValue(updated);
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_checkin_success));
                    } else if (resource.isError()) {
                        String errorMsg = resource.getMessage();
                        if (resource.getCode() == 400) {
                            errorMsg = getApplication().getString(R.string.error_checkin_not_allowed);
                        } else if (errorMsg == null || errorMsg.trim().isEmpty()) {
                            errorMsg = getApplication().getString(R.string.toast_checkin_failed);
                        }
                        actionErrorLiveData.setValue(errorMsg);
                        toastMessageLiveData.setValue(errorMsg);
                    }
                }
            }
        });
    }
}
