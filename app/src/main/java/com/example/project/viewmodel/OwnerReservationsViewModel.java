package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.example.project.R;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.ReservationResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReservationRepository;
import com.example.project.utils.SessionManager;

import java.util.List;

/**
 * ViewModel managing incoming reservations and approval requests for a parking spot owner.
 * Connects to GET /api/reservations/incoming, POST /api/reservations/{id}/accept,
 * and POST /api/reservations/{id}/reject.
 */
public class OwnerReservationsViewModel extends AndroidViewModel {

    private final ReservationRepository reservationRepository;
    private final AppRepository appRepository;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>(null);

    public OwnerReservationsViewModel(@NonNull Application application) {
        this(application, ReservationRepository.getInstance(application), AppRepository.getInstance());
    }

    public OwnerReservationsViewModel(@NonNull Application application,
                                     @NonNull ReservationRepository reservationRepository,
                                     @NonNull AppRepository appRepository) {
        super(application);
        this.reservationRepository = reservationRepository;
        this.appRepository = appRepository;
        refreshIncomingReservations();
    }

    /**
     * Gets reactive LiveData stream of incoming reservations for the owner's parking spots.
     *
     * @return LiveData list of ReservationEntity
     */
    public LiveData<List<ReservationEntity>> getIncomingReservationsLiveData() {
        return Transformations.switchMap(appRepository.getUserEmailLiveData(), email -> {
            String targetEmail = email;
            if (targetEmail == null || targetEmail.isEmpty()) {
                if (SessionManager.getInstance() != null) {
                    targetEmail = SessionManager.getInstance().getUserEmail();
                }
            }
            return reservationRepository.getOwnerIncomingReservationsLiveData(targetEmail);
        });
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<String> getToastMessageLiveData() {
        return toastMessageLiveData;
    }

    /**
     * Fetches fresh incoming reservations from GET /api/reservations/incoming.
     */
    public void refreshIncomingReservations() {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        if (currentEmail == null || currentEmail.isEmpty()) {
            if (SessionManager.getInstance() != null) {
                currentEmail = SessionManager.getInstance().getUserEmail();
            }
        }

        isLoadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);

        LiveData<Resource<List<ReservationResponse>>> source = reservationRepository.fetchIncomingReservations(currentEmail);
        source.observeForever(new Observer<Resource<List<ReservationResponse>>>() {
            @Override
            public void onChanged(Resource<List<ReservationResponse>> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    isLoadingLiveData.setValue(false);
                    if (resource.isError() && resource.getMessage() != null) {
                        errorMessageLiveData.setValue(resource.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Accepts / confirms a pending reservation request via POST /api/reservations/{id}/accept.
     *
     * @param reservationId Reservation ID
     */
    public void acceptReservation(@NonNull String reservationId) {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        if (currentEmail == null || currentEmail.isEmpty()) {
            if (SessionManager.getInstance() != null) {
                currentEmail = SessionManager.getInstance().getUserEmail();
            }
        }

        isLoadingLiveData.setValue(true);
        LiveData<Resource<ReservationResponse>> source = reservationRepository.acceptReservation(reservationId, currentEmail);
        source.observeForever(new Observer<Resource<ReservationResponse>>() {
            @Override
            public void onChanged(Resource<ReservationResponse> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    isLoadingLiveData.setValue(false);
                    if (resource.isSuccess()) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_accept_success));
                        refreshIncomingReservations();
                    } else if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : getApplication().getString(R.string.toast_accept_failed);
                        toastMessageLiveData.setValue(msg);
                    }
                }
            }
        });
    }

    /**
     * Rejects a pending reservation request via POST /api/reservations/{id}/reject.
     *
     * @param reservationId Reservation ID
     * @param reason        Rejection reason
     */
    public void rejectReservation(@NonNull String reservationId, @Nullable String reason) {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        if (currentEmail == null || currentEmail.isEmpty()) {
            if (SessionManager.getInstance() != null) {
                currentEmail = SessionManager.getInstance().getUserEmail();
            }
        }

        isLoadingLiveData.setValue(true);
        LiveData<Resource<ReservationResponse>> source = reservationRepository.rejectReservation(reservationId, reason, currentEmail);
        source.observeForever(new Observer<Resource<ReservationResponse>>() {
            @Override
            public void onChanged(Resource<ReservationResponse> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    isLoadingLiveData.setValue(false);
                    if (resource.isSuccess()) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_reject_success));
                        refreshIncomingReservations();
                    } else if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : getApplication().getString(R.string.toast_reject_failed);
                        toastMessageLiveData.setValue(msg);
                    }
                }
            }
        });
    }
}
