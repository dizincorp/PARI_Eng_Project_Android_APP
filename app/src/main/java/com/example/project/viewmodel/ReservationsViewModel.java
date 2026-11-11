package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.example.project.R;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.model.Reservation;
import com.example.project.network.Resource;
import com.example.project.network.model.CheckOutResponse;
import com.example.project.network.model.ReservationResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReservationRepository;
import com.example.project.utils.ReservationLifecycleManager;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel managing the state of the Reservations screen and the lifecycle operations
 * (cancellation, check-in) for user parking reservations.
 * Connects to {@link ReservationRepository} to dispatch REST API requests and exposes
 * Room LiveData streams enforcing the Single Source of Truth principle.
 */
public class ReservationsViewModel extends AndroidViewModel {

    private final ReservationRepository reservationRepository;
    private final AppRepository appRepository;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);

    // LiveData streams for reservation lifecycle actions (Cancel, Check-in)
    private final MutableLiveData<Resource<ReservationResponse>> actionStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<ReservationResponse> updatedReservationLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<String> actionErrorLiveData = new MutableLiveData<>(null);

    private final MutableLiveData<Long> partitionEpochLiveData = new MutableLiveData<>(System.currentTimeMillis());
    private final MediatorLiveData<List<Reservation>> activeReservationsLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<List<Reservation>> historyReservationsLiveData = new MediatorLiveData<>();
    private final List<ReservationEntity> cachedAllEntities = new ArrayList<>();
    private long nextBoundaryTimestampMs = Long.MAX_VALUE;

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
        syncEmailWithSessionIfNeeded();

        LiveData<List<ReservationEntity>> allSource = Transformations.switchMap(
                appRepository.getUserEmailLiveData(),
                reservationRepository::getAllReservationsLiveData
        );

        activeReservationsLiveData.addSource(allSource, entities -> {
            cachedAllEntities.clear();
            if (entities != null) {
                cachedAllEntities.addAll(entities);
            }
            recomputePartitions();
        });
        activeReservationsLiveData.addSource(partitionEpochLiveData, epoch -> recomputePartitions());
        historyReservationsLiveData.addSource(allSource, entities -> {
            if (!activeReservationsLiveData.hasActiveObservers()) {
                cachedAllEntities.clear();
                if (entities != null) {
                    cachedAllEntities.addAll(entities);
                }
                recomputePartitions();
            }
        });
        historyReservationsLiveData.addSource(partitionEpochLiveData, epoch -> {
            if (!activeReservationsLiveData.hasActiveObservers()) {
                recomputePartitions();
            }
        });
    }

    /**
     * LiveData stream of incoming / active reservations for the current user,
     * partitioned dynamically against the current clock time.
     *
     * @return LiveData emitting list of active {@link Reservation}
     */
    public LiveData<List<Reservation>> getIncomingReservationsLiveData() {
        return activeReservationsLiveData;
    }

    /**
     * LiveData stream of past / history reservations for the current user,
     * partitioned dynamically against the current clock time.
     *
     * @return LiveData emitting list of history {@link Reservation}
     */
    public LiveData<List<Reservation>> getHistoryReservationsLiveData() {
        return historyReservationsLiveData;
    }

    /**
     * Called on UI timer ticks to immediately move reservations across time boundaries
     * (end time expiration, no-show, +1h overstay grace, auto-checkout).
     */
    public void onTimeTick() {
        long now = System.currentTimeMillis();
        if (now >= nextBoundaryTimestampMs) {
            ReservationLifecycleManager.getInstance(getApplication()).evaluateAsync();
            partitionEpochLiveData.setValue(now);
        }
    }

    /**
     * LiveData emitting loading status of list refresh requests.
     *
     * @return LiveData boolean
     */
    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    /**
     * LiveData emitting user-facing error messages if a network error occurs during list sync.
     *
     * @return LiveData string
     */
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * LiveData tracking the status (Loading, Success, Error) of the latest lifecycle action (Cancel, Check-in).
     *
     * @return LiveData Resource containing {@link ReservationResponse}
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
     * LiveData emitting the updated {@link ReservationResponse} after a successful server operation (HTTP 200)
     * so that the View can instantly update its displayed state.
     *
     * @return LiveData containing the latest ReservationResponse
     */
    public LiveData<ReservationResponse> getUpdatedReservationLiveData() {
        return updatedReservationLiveData;
    }

    /**
     * LiveData emitting user-facing error descriptions when an action fails (e.g. canceling already started parking).
     *
     * @return LiveData string error message
     */
    public LiveData<String> getActionErrorLiveData() {
        return actionErrorLiveData;
    }

    /**
     * Gets reactive LiveData stream for a single reservation by ID from Room DB cache.
     *
     * @param id Unique reservation ID
     * @return LiveData emitting ReservationEntity
     */
    public LiveData<ReservationEntity> getReservationByIdLiveData(@NonNull String id) {
        return reservationRepository.getReservationByIdLiveData(id);
    }

    private void syncEmailWithSessionIfNeeded() {
        SessionManager sm = SessionManager.getInstance(getApplication());
        if (sm != null && sm.isLoggedIn() && sm.getUserEmail() != null && !sm.getUserEmail().trim().isEmpty()) {
            String currentEmail = appRepository.getUserEmailLiveData().getValue();
            if (currentEmail == null || currentEmail.trim().isEmpty()
                    || "jan_kowalski@gmail.com".equalsIgnoreCase(currentEmail.trim())) {
                appRepository.init(getApplication());
            }
        }
    }

    @NonNull
    private String resolveUserEmail() {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        SessionManager sm = SessionManager.getInstance(getApplication());
        if (sm != null && sm.isLoggedIn() && sm.getUserEmail() != null && !sm.getUserEmail().trim().isEmpty()) {
            if (currentEmail == null || currentEmail.trim().isEmpty()
                    || "jan_kowalski@gmail.com".equalsIgnoreCase(currentEmail.trim())) {
                return sm.getUserEmail().trim();
            }
        }
        if (currentEmail != null && !currentEmail.trim().isEmpty()) {
            return currentEmail.trim();
        }
        return (sm != null && sm.getUserEmail() != null) ? sm.getUserEmail().trim() : "";
    }

    /**
     * Refreshes both incoming reservations (GET /api/reservations/incoming)
     * and all user reservations (GET /api/reservations) from the backend REST API.
     */
    public void refreshReservations() {
        String currentEmail = resolveUserEmail();
        isLoadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);

        // Fetch exclusively driver reservations (GET /api/reservations)
        LiveData<Resource<List<ReservationResponse>>> allSource = reservationRepository.fetchReservations(currentEmail);
        allSource.observeForever(new Observer<Resource<List<ReservationResponse>>>() {
            @Override
            public void onChanged(Resource<List<ReservationResponse>> resource) {
                if (resource != null && !resource.isLoading()) {
                    allSource.removeObserver(this);
                    isLoadingLiveData.setValue(false);
                    if (resource.isError() && resource.getMessage() != null) {
                        errorMessageLiveData.setValue(resource.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Cancels an active reservation via POST /api/reservations/{id}/cancel.
     * Upon HTTP 200 success, emits the updated model to {@link #getUpdatedReservationLiveData()},
     * triggers {@link #getToastMessageLiveData()} with a confirmation message, updates local Room cache,
     * and refreshes the reservation lists.
     * On error (e.g. HTTP 409 when parking has already started), emits the error message to
     * {@link #getActionErrorLiveData()} and {@link #getToastMessageLiveData()}.
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

        String currentEmail = resolveUserEmail();
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
                        // Automatically synchronize lists from backend and Room cache
                        refreshReservations();
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
     * Performs check-in to begin parking for the reservation via POST /api/reservations/{id}/check-in.
     * Upon HTTP 200 success, emits the updated model to {@link #getUpdatedReservationLiveData()},
     * triggers {@link #getToastMessageLiveData()} with a welcome message, updates local Room cache,
     * and refreshes the reservation lists.
     * On error (e.g. before start time or already checked-in), emits error message to
     * {@link #getActionErrorLiveData()} and {@link #getToastMessageLiveData()}.
     *
     * @param id Unique identifier of the reservation to check in
     */
    public void checkIn(@Nullable String id) {
        checkIn(id, null);
    }

    /**
     * Performs check-in to begin parking session via POST /api/reservations/{id}/check-in
     * providing the reservation PIN code.
     *
     * @param id      Unique identifier of the reservation to check in
     * @param pinCode Entry PIN code for access validation
     */
    public void checkIn(@Nullable String id, @Nullable String pinCode) {
        if (id == null || id.trim().isEmpty()) {
            String validationMsg = getApplication().getString(R.string.toast_checkin_failed);
            actionErrorLiveData.setValue(validationMsg);
            toastMessageLiveData.setValue(validationMsg);
            return;
        }

        String currentEmail = resolveUserEmail();
        actionStatusLiveData.setValue(Resource.loading(null));

        LiveData<Resource<ReservationResponse>> source = reservationRepository.checkInReservation(id.trim(), pinCode, null, currentEmail);
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
                        // Automatically synchronize lists from backend and Room cache
                        refreshReservations();
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

    /**
     * Performs check-out to complete parking for the reservation via POST /api/reservations/{id}/check-out.
     * Enforces the business rule that check-out is impossible without check-in.
     * Upon success, updates local Room cache, triggers toast confirmation, and synchronizes list state.
     *
     * @param id Unique identifier of the reservation to check out
     */
    public void checkOut(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            String validationMsg = getApplication().getString(R.string.toast_checkout_failed);
            actionErrorLiveData.setValue(validationMsg);
            toastMessageLiveData.setValue(validationMsg);
            return;
        }

        String currentEmail = resolveUserEmail();
        actionStatusLiveData.setValue(Resource.loading(null));

        LiveData<Resource<CheckOutResponse>> source = reservationRepository.checkOutReservation(id.trim(), currentEmail);
        source.observeForever(new Observer<Resource<CheckOutResponse>>() {
            @Override
            public void onChanged(Resource<CheckOutResponse> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);

                    if (resource.isSuccess() && resource.getData() != null) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_checkout_success));
                        // Automatically synchronize lists from backend and Room cache
                        refreshReservations();
                    } else if (resource.isError()) {
                        String errorMsg = resource.getMessage();
                        if (resource.getCode() == 400) {
                            errorMsg = getApplication().getString(R.string.error_checkout_requires_checkin);
                        } else if (errorMsg == null || errorMsg.trim().isEmpty()) {
                            errorMsg = getApplication().getString(R.string.toast_checkout_failed);
                        }
                        actionErrorLiveData.setValue(errorMsg);
                        toastMessageLiveData.setValue(errorMsg);
                    }
                }
            }
        });
    }

    private void recomputePartitions() {
        long now = System.currentTimeMillis();
        long earliestNextBoundary = Long.MAX_VALUE;
        List<Reservation> active = new ArrayList<>();
        List<Reservation> history = new ArrayList<>();

        ReservationLifecycleManager lifecycle = ReservationLifecycleManager.getInstance(getApplication());

        for (ReservationEntity e : cachedAllEntities) {
            if (e == null) continue;
            Reservation model = mapEntityToModel(e, now, lifecycle);
            boolean isHistory = ReservationLifecycleManager.isHistoryStatus(e.getStatus(), e.getEndTimeMs(), now);

            if (isHistory) {
                history.add(model);
            } else {
                active.add(model);
                long endMs = e.getEndTimeMs();
                String upper = model.getStatus() != null ? model.getStatus().toUpperCase(Locale.ROOT) : "";
                if (upper.contains("IN_PROGRESS") && endMs > 0) {
                    if (now < endMs) {
                        earliestNextBoundary = Math.min(earliestNextBoundary, endMs);
                    } else {
                        long graceEndMs = endMs + ReservationLifecycleManager.ONE_HOUR_MS;
                        if (now < graceEndMs) {
                            earliestNextBoundary = Math.min(earliestNextBoundary, graceEndMs);
                        }
                    }
                } else if (endMs > 0 && now < endMs) {
                    earliestNextBoundary = Math.min(earliestNextBoundary, endMs);
                }
            }
        }

        Collections.sort(active, (a, b) -> Long.compare(a.getStartTimeMs(), b.getStartTimeMs()));
        Collections.sort(history, (a, b) -> Long.compare(b.getStartTimeMs(), a.getStartTimeMs()));

        nextBoundaryTimestampMs = earliestNextBoundary;
        activeReservationsLiveData.setValue(active);
        historyReservationsLiveData.setValue(history);
    }

    private Reservation mapEntityToModel(@NonNull ReservationEntity e,
                                         long now,
                                         @NonNull ReservationLifecycleManager lifecycle) {
        String effectiveStatus = ReservationLifecycleManager.computeEffectiveStatus(e.getStatus(), e.getEndTimeMs(), now);
        double displayPrice = e.getTotalPrice();
        boolean overstayApplied = e.isOverstayApplied() || lifecycle.isOverstayApplied(e.getId());
        if (overstayApplied) {
            displayPrice = Math.max(displayPrice, lifecycle.getStoredPenalizedPrice(e.getId(), displayPrice));
        }

        Reservation r = new Reservation(
                e.getId(), e.getPlaceId(), e.getPlaceName(), e.getStreet(), e.getCity(),
                e.getPlaceNumber(), e.getDateString(), e.getTimeRange(), e.getStartTimeMs(),
                e.getEndTimeMs(), e.getVehicleInfo(), effectiveStatus, displayPrice,
                e.getPinCode(), e.getPhotoResId() != 0 ? e.getPhotoResId() : R.drawable.default_parking_spot
        );
        r.setUserEmail(e.getUserEmail() != null ? e.getUserEmail() : "");
        r.setCarPlate(e.getCarPlate() != null ? e.getCarPlate() : "");
        r.setImageUri(e.getImageUri());
        r.setHourlyRate(e.getHourlyRate());
        r.setQrToken(e.getQrToken());
        r.setOverstayApplied(overstayApplied);
        return r;
    }
}
