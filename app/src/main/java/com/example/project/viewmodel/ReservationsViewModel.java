package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.example.project.R;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.model.Reservation;
import com.example.project.network.Resource;
import com.example.project.network.model.ReservationResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReservationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel managing the state of the Reservations screen.
 * Connects to {@link ReservationRepository} to fetch reservations from REST endpoints
 * (GET /api/reservations and GET /api/reservations/incoming) and exposes Room LiveData streams
 * enforcing the Single Source of Truth principle.
 */
public class ReservationsViewModel extends AndroidViewModel {

    private final ReservationRepository reservationRepository;
    private final AppRepository appRepository;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);

    /**
     * Default constructor for ReservationsViewModel.
     *
     * @param application Application context
     */
    public ReservationsViewModel(@NonNull Application application) {
        this(application, ReservationRepository.getInstance(application), AppRepository.getInstance());
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param application           Application context
     * @param reservationRepository ReservationRepository instance
     * @param appRepository         AppRepository instance
     */
    public ReservationsViewModel(@NonNull Application application,
                                 @NonNull ReservationRepository reservationRepository,
                                 @NonNull AppRepository appRepository) {
        super(application);
        this.reservationRepository = reservationRepository;
        this.appRepository = appRepository;
    }

    /**
     * LiveData stream of incoming / active reservations for the current user.
     *
     * @return LiveData emitting list of active {@link Reservation}
     */
    public LiveData<List<Reservation>> getIncomingReservationsLiveData() {
        return Transformations.switchMap(appRepository.getUserEmailLiveData(), email ->
                Transformations.map(reservationRepository.getIncomingReservationsLiveData(email), this::mapEntitiesToModels)
        );
    }

    /**
     * LiveData stream of past / history reservations for the current user.
     *
     * @return LiveData emitting list of history {@link Reservation}
     */
    public LiveData<List<Reservation>> getHistoryReservationsLiveData() {
        return Transformations.switchMap(appRepository.getUserEmailLiveData(), email ->
                Transformations.map(reservationRepository.getHistoryReservationsLiveData(email), this::mapEntitiesToModels)
        );
    }

    /**
     * LiveData emitting loading status of network requests.
     *
     * @return LiveData boolean
     */
    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    /**
     * LiveData emitting user-facing error messages if a network error occurs.
     *
     * @return LiveData string
     */
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * Refreshes both incoming reservations (GET /api/reservations/incoming)
     * and all user reservations (GET /api/reservations) from the backend REST API.
     */
    public void refreshReservations() {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        isLoadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);

        final int[] remainingCalls = new int[]{2};

        // 1. Fetch incoming reservations
        LiveData<Resource<List<ReservationResponse>>> incomingSource = reservationRepository.fetchIncomingReservations(currentEmail);
        incomingSource.observeForever(new Observer<Resource<List<ReservationResponse>>>() {
            @Override
            public void onChanged(Resource<List<ReservationResponse>> resource) {
                if (resource != null && !resource.isLoading()) {
                    incomingSource.removeObserver(this);
                    if (resource.isError() && resource.getMessage() != null) {
                        errorMessageLiveData.setValue(resource.getMessage());
                    }
                    remainingCalls[0]--;
                    if (remainingCalls[0] <= 0) {
                        isLoadingLiveData.setValue(false);
                    }
                }
            }
        });

        // 2. Fetch all reservations (history)
        LiveData<Resource<List<ReservationResponse>>> allSource = reservationRepository.fetchReservations(currentEmail);
        allSource.observeForever(new Observer<Resource<List<ReservationResponse>>>() {
            @Override
            public void onChanged(Resource<List<ReservationResponse>> resource) {
                if (resource != null && !resource.isLoading()) {
                    allSource.removeObserver(this);
                    if (resource.isError() && resource.getMessage() != null) {
                        errorMessageLiveData.setValue(resource.getMessage());
                    }
                    remainingCalls[0]--;
                    if (remainingCalls[0] <= 0) {
                        isLoadingLiveData.setValue(false);
                    }
                }
            }
        });
    }

    private List<Reservation> mapEntitiesToModels(List<ReservationEntity> entities) {
        List<Reservation> list = new ArrayList<>();
        if (entities != null) {
            for (ReservationEntity e : entities) {
                if (e == null) continue;
                Reservation r = new Reservation(
                        e.getId(), e.getPlaceId(), e.getPlaceName(), e.getStreet(), e.getCity(),
                        e.getPlaceNumber(), e.getDateString(), e.getTimeRange(), e.getStartTimeMs(),
                        e.getEndTimeMs(), e.getVehicleInfo(), e.getStatus(), e.getTotalPrice(),
                        e.getPinCode(), e.getPhotoResId() != 0 ? e.getPhotoResId() : R.drawable.default_parking_spot
                );
                r.setUserEmail(e.getUserEmail() != null ? e.getUserEmail() : "");
                r.setCarPlate(e.getCarPlate() != null ? e.getCarPlate() : "");
                list.add(r);
            }
        }
        return list;
    }
}
