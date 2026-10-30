package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.R;
import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.dao.ReservationDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.ReservationApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.CancelReservationRequest;
import com.example.project.network.model.CheckOutResponse;
import com.example.project.network.model.RejectReservationRequest;
import com.example.project.network.model.QuoteRequest;
import com.example.project.network.model.QuoteResponse;
import com.example.project.network.model.CheckInReservationRequest;
import com.example.project.network.model.ReservationRequest;
import com.example.project.network.model.ReservationResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth (SSOT) pattern for reservations.
 * Sends reservation requests, fetches user reservations, and manages lifecycle transitions
 * (check-in, cancellation) from the backend REST API via {@link ReservationApiService}
 * and synchronizes them into the local Room Database.
 */
public class ReservationRepository {

    private static final String TAG = "ReservationRepository";
    private static ReservationRepository instance;

    private final ReservationApiService apiService;
    private final ReservationDao reservationDao;
    private final ParkingPlaceDao parkingPlaceDao;
    private final ExecutorService diskExecutor;

    /**
     * Private constructor initializing dependencies from application context.
     *
     * @param context Application context
     */
    private ReservationRepository(@NonNull Context context) {
        this(
                ApiClient.getReservationApiService(),
                AppDatabase.getInstance(context).reservationDao(),
                AppDatabase.getInstance(context).parkingPlaceDao(),
                Executors.newSingleThreadExecutor()
        );
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param apiService      ReservationApiService instance
     * @param reservationDao  ReservationDao Room DAO
     * @param parkingPlaceDao ParkingPlaceDao Room DAO
     * @param diskExecutor    Executor for asynchronous Room database writes
     */
    public ReservationRepository(@NonNull ReservationApiService apiService,
                                 @NonNull ReservationDao reservationDao,
                                 @NonNull ParkingPlaceDao parkingPlaceDao,
                                 @NonNull ExecutorService diskExecutor) {
        this.apiService = apiService;
        this.reservationDao = reservationDao;
        this.parkingPlaceDao = parkingPlaceDao;
        this.diskExecutor = diskExecutor;
    }

    /**
     * Gets the singleton instance of ReservationRepository.
     *
     * @param context Application context
     * @return Synchronized ReservationRepository instance
     */
    public static synchronized ReservationRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ReservationRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Gets reactive LiveData stream of incoming / active reservations from local Room DB cache.
     *
     * @param userEmail Current user's email
     * @return LiveData emitting list of ReservationEntity
     */
    public LiveData<List<ReservationEntity>> getIncomingReservationsLiveData(@Nullable String userEmail) {
        return reservationDao.getIncomingReservationsLiveData(userEmail != null ? userEmail : "", System.currentTimeMillis());
    }

    /**
     * Gets reactive LiveData stream of past / history reservations from local Room DB cache.
     *
     * @param userEmail Current user's email
     * @return LiveData emitting list of ReservationEntity
     */
    public LiveData<List<ReservationEntity>> getHistoryReservationsLiveData(@Nullable String userEmail) {
        return reservationDao.getHistoryReservationsListLiveData(userEmail != null ? userEmail : "", System.currentTimeMillis());
    }

    /**
     * Gets reactive LiveData stream of all reservations for the user from local Room DB cache.
     *
     * @param userEmail Current user's email
     * @return LiveData emitting list of ReservationEntity
     */
    public LiveData<List<ReservationEntity>> getAllReservationsLiveData(@Nullable String userEmail) {
        return reservationDao.getAllReservationsListLiveData(userEmail != null ? userEmail : "");
    }

    /**
     * Gets a reactive LiveData stream for a single reservation by ID from the Room database cache.
     *
     * @param reservationId Unique identifier of the reservation
     * @return LiveData emitting the ReservationEntity
     */
    public LiveData<ReservationEntity> getReservationByIdLiveData(@NonNull String reservationId) {
        return reservationDao.getReservationByIdLiveData(reservationId);
    }

    /**
     * Sends a booking request to POST /api/reservations and synchronizes response to Room DB.
     *
     * @param request   ReservationRequest payload
     * @param userEmail Current user's email address for local account isolation
     * @return LiveData emitting Resource with ReservationResponse
     */
    public LiveData<Resource<ReservationResponse>> createReservation(@NonNull ReservationRequest request,
                                                                     @Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.createReservation(request).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse body = response.body();

                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                        if (place != null) {
                            place.setOccupied(true);
                            place.setCheckedIn(false);
                            if (body.getVehiclePlate() != null && !body.getVehiclePlate().isEmpty()) {
                                place.setDriverVehicle(body.getVehiclePlate());
                            }
                            if (body.getPinCode() != null && !body.getPinCode().isEmpty()) {
                                place.setPinCode(body.getPinCode());
                            }
                            parkingPlaceDao.insertOrUpdate(place);
                        }

                        ReservationEntity entity = toEntity(body, userEmail, place);
                        reservationDao.insertOrUpdate(entity);

                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "Reservation failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "Reservation network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Fetches all user reservations from GET /api/reservations and synchronizes them to Room.
     *
     * @param userEmail Current user's email
     * @return LiveData emitting Resource with list of ReservationResponse
     */
    public LiveData<Resource<List<ReservationResponse>>> fetchReservations(@Nullable String userEmail) {
        MutableLiveData<Resource<List<ReservationResponse>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReservationResponse> body = response.body();
                    diskExecutor.execute(() -> {
                        for (ReservationResponse res : body) {
                            if (res == null) continue;
                            ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(res.getSpotId());
                            ReservationEntity entity = toEntity(res, userEmail, place);
                            reservationDao.insertOrUpdate(entity);
                        }
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "getReservations failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "getReservations failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Fetches incoming / active reservations from GET /api/reservations/incoming and synchronizes them to Room.
     *
     * @param userEmail Current user's email
     * @return LiveData emitting Resource with list of incoming ReservationResponse
     */
    public LiveData<Resource<List<ReservationResponse>>> fetchIncomingReservations(@Nullable String userEmail) {
        MutableLiveData<Resource<List<ReservationResponse>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getIncomingReservations().enqueue(new Callback<List<ReservationResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ReservationResponse>> call,
                                   @NonNull Response<List<ReservationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReservationResponse> body = response.body();
                    diskExecutor.execute(() -> {
                        for (ReservationResponse res : body) {
                            if (res == null) continue;
                            ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(res.getSpotId());
                            ReservationEntity entity = toEntity(res, userEmail, place);
                            if (res.getStatus() != null && !res.getStatus().isEmpty()) {
                                entity.setStatus(res.getStatus());
                            } else if (entity.getStatus() == null || entity.getStatus().isEmpty() || entity.getStatus().equalsIgnoreCase("PENDING_PAYMENT")) {
                                entity.setStatus("CONFIRMED");
                            }
                            reservationDao.insertOrUpdate(entity);

                            // Synchronize place state with incoming reservation
                            if (place != null) {
                                String status = entity.getStatus();
                                if ("COMPLETED".equalsIgnoreCase(status)
                                        || "CANCELLED".equalsIgnoreCase(status)
                                        || "REJECTED".equalsIgnoreCase(status)
                                        || "EXPIRED".equalsIgnoreCase(status)) {
                                    ReservationEntity activeOther = reservationDao.getActiveReservationForSpotSync(place.getId());
                                    if (activeOther == null) {
                                        place.setOccupied(false);
                                        place.setCheckedIn(false);
                                        place.setDriverName("");
                                        place.setDriverVehicle("");
                                        parkingPlaceDao.insertOrUpdate(place);
                                    }
                                } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                                    place.setOccupied(true);
                                    place.setCheckedIn(true);
                                    if (res.getVehiclePlate() != null && !res.getVehiclePlate().isEmpty()) {
                                        place.setDriverVehicle(res.getVehiclePlate());
                                    }
                                    if (res.getDriverId() != null && !res.getDriverId().isEmpty()) {
                                        place.setDriverName(res.getDriverId());
                                    }
                                    parkingPlaceDao.insertOrUpdate(place);
                                } else if ("CONFIRMED".equalsIgnoreCase(status)
                                        || "PENDING_APPROVAL".equalsIgnoreCase(status)
                                        || "PENDING_PAYMENT".equalsIgnoreCase(status)
                                        || "AKTYWNA".equalsIgnoreCase(status)) {
                                    place.setOccupied(true);
                                    place.setCheckedIn(false);
                                    if (res.getVehiclePlate() != null && !res.getVehiclePlate().isEmpty()) {
                                        place.setDriverVehicle(res.getVehiclePlate());
                                    }
                                    if (res.getDriverId() != null && !res.getDriverId().isEmpty()) {
                                        place.setDriverName(res.getDriverId());
                                    }
                                    parkingPlaceDao.insertOrUpdate(place);
                                }
                            }
                        }
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "getIncomingReservations failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ReservationResponse>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "getIncomingReservations failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Cancels an existing reservation by its unique identifier via POST /api/reservations/{id}/cancel
     * and synchronizes the updated state to the local Room database cache.
     *
     * @param reservationId Unique identifier of the reservation to cancel
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with the updated {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> cancelReservation(@NonNull String reservationId,
                                                                     @Nullable String userEmail) {
        return cancelReservation(reservationId, null, userEmail);
    }

    /**
     * Cancels an existing reservation by its unique identifier via POST /api/reservations/{id}/cancel
     * with an explanation reason and synchronizes the updated state to the local Room database cache.
     *
     * @param reservationId Unique identifier of the reservation to cancel
     * @param reason        Cancellation explanation reason
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with the updated {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> cancelReservation(@NonNull String reservationId,
                                                                     @Nullable String reason,
                                                                     @Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        CancelReservationRequest req = new CancelReservationRequest(reason != null ? reason : "");
        apiService.cancelReservationWithReason(reservationId.trim(), req).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse body = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                        if (place != null) {
                            place.setOccupied(false);
                            place.setCheckedIn(false);
                            place.setDriverName("");
                            place.setDriverVehicle("");
                            place.setPinCode("");
                            parkingPlaceDao.insertOrUpdate(place);
                        }

                        ReservationEntity entity = toEntity(body, userEmail, place);
                        entity.setStatus(body.getStatus() != null && !body.getStatus().isEmpty() ? body.getStatus() : "CANCELLED");
                        reservationDao.insertOrUpdate(entity);

                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseCancelErrorMessage(response);
                    Log.e(TAG, "cancelReservation failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "cancelReservation network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Performs check-in to begin parking for the reservation via POST /api/reservations/{id}/check-in
     * and synchronizes the active parking state into Room database cache.
     *
     * @param reservationId Unique identifier of the reservation
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with the updated {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> checkInReservation(@NonNull String reservationId,
                                                                      @Nullable String userEmail) {
        return checkInReservation(reservationId, null, null, userEmail);
    }

    /**
     * Performs check-in to begin parking for the reservation via POST /api/reservations/{id}/check-in
     * with optional validation credentials (PIN code or QR token) and synchronizes state to Room.
     *
     * @param reservationId Unique identifier of the reservation
     * @param pinCode       Optional PIN code
     * @param qrToken       Optional QR token
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with the updated {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> checkInReservation(@NonNull String reservationId,
                                                                      @Nullable String pinCode,
                                                                      @Nullable String qrToken,
                                                                      @Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        diskExecutor.execute(() -> {
            String resolvedPin = pinCode;
            String resolvedQr = qrToken;
            if ((resolvedPin == null || resolvedPin.trim().isEmpty()) && (resolvedQr == null || resolvedQr.trim().isEmpty())) {
                ReservationEntity localEntity = reservationDao.getReservationByIdSync(reservationId.trim());
                if (localEntity != null && localEntity.getPinCode() != null && !localEntity.getPinCode().trim().isEmpty()) {
                    resolvedPin = localEntity.getPinCode().trim();
                }
            }

            CheckInReservationRequest requestPayload = new CheckInReservationRequest(
                    resolvedPin != null ? resolvedPin.trim() : "",
                    resolvedQr != null ? resolvedQr.trim() : ""
            );

            Call<ReservationResponse> apiCall = apiService.checkInReservationWithCredentials(reservationId.trim(), requestPayload);
            apiCall.enqueue(new Callback<ReservationResponse>() {
                @Override
                public void onResponse(@NonNull Call<ReservationResponse> call,
                                       @NonNull Response<ReservationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ReservationResponse body = response.body();
                        diskExecutor.execute(() -> {
                            ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                            if (place != null) {
                                place.setOccupied(true);
                                place.setCheckedIn(true);
                                parkingPlaceDao.insertOrUpdate(place);
                            }

                            ReservationEntity entity = toEntity(body, userEmail, place);
                            entity.setStatus(body.getStatus() != null && !body.getStatus().isEmpty() ? body.getStatus() : "IN_PROGRESS");
                            reservationDao.insertOrUpdate(entity);

                            resultLiveData.postValue(Resource.success(body));
                        });
                    } else {
                        String errorMsg = parseCheckInErrorMessage(response);
                        Log.e(TAG, "checkInReservation failed with HTTP " + response.code() + ": " + errorMsg);
                        resultLiveData.postValue(Resource.error(errorMsg, null, response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                    String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                    Log.e(TAG, "checkInReservation network failure: " + errorMsg, t);
                    resultLiveData.postValue(Resource.error(errorMsg, null, 0));
                }
            });
        });

        return resultLiveData;
    }

    /**
     * Fetches detailed information for a specific reservation by ID from GET /api/reservations/{id}
     * and synchronizes the record into the Room database cache.
     *
     * @param reservationId Unique identifier of the reservation
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with the updated {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> fetchReservationById(@NonNull String reservationId,
                                                                        @Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        apiService.getReservationById(reservationId.trim()).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse body = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                        ReservationEntity entity = toEntity(body, userEmail, place);
                        reservationDao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "getReservationById failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "getReservationById network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Fetches the current active parking session for the authenticated driver via GET /api/reservations/active-session
     * and synchronizes active parking state into Room database cache.
     *
     * @param userEmail Current user's email address for local account isolation
     * @return LiveData emitting Resource with active {@link ReservationResponse} or null if no active session
     */
    public LiveData<Resource<ReservationResponse>> fetchActiveSession(@Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.postActiveSession().enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse body = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                        if (place != null) {
                            place.setOccupied(true);
                            place.setCheckedIn(true);
                            parkingPlaceDao.insertOrUpdate(place);
                        }
                        ReservationEntity entity = toEntity(body, userEmail, place);
                        entity.setStatus(body.getStatus() != null && !body.getStatus().isEmpty() ? body.getStatus() : "IN_PROGRESS");
                        reservationDao.insertOrUpdate(entity);
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else if (response.code() == 404 || response.code() == 204) {
                    resultLiveData.setValue(Resource.success(null));
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "getActiveSession failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "getActiveSession network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Completes parking session and checks out from parking spot via POST /api/reservations/{id}/check-out,
     * freeing the spot and updating status to completed in the local Room database cache.
     *
     * @param reservationId Unique identifier of the reservation to check out
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with finalized {@link ReservationResponse}
     */
    public LiveData<Resource<CheckOutResponse>> checkOutReservation(@NonNull String reservationId,
                                                                    @Nullable String userEmail) {
        MutableLiveData<Resource<CheckOutResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        apiService.checkOutReservation(reservationId.trim()).enqueue(new Callback<CheckOutResponse>() {
            @Override
            public void onResponse(@NonNull Call<CheckOutResponse> call,
                                   @NonNull Response<CheckOutResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CheckOutResponse body = response.body();
                    diskExecutor.execute(() -> {
                        ReservationEntity entity = reservationDao.getReservationByIdSync(reservationId.trim());
                        if (entity == null && body.getId() != null) {
                            entity = reservationDao.getReservationByIdSync(body.getId());
                        }
                        String spotId = (entity != null && entity.getPlaceId() != null) ? entity.getPlaceId() : "";

                        if (entity != null) {
                            entity.setStatus(body.getStatus() != null && !body.getStatus().isEmpty() ? body.getStatus() : "COMPLETED");
                            entity.setTotalPrice(body.getTotalFinalAmount());
                            if (body.getActualCheckoutAt() != null) {
                                entity.setEndTimeMs(body.getActualCheckoutAt().getTime());
                            }
                            reservationDao.insertOrUpdate(entity);
                        }

                        if (!spotId.isEmpty()) {
                            ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(spotId);
                            if (place != null) {
                                ReservationEntity activeOther = reservationDao.getActiveReservationForSpotSync(spotId);
                                if (activeOther == null) {
                                    place.setOccupied(false);
                                    place.setCheckedIn(false);
                                    place.setDriverName("");
                                    place.setDriverVehicle("");
                                    place.setPinCode("");
                                    parkingPlaceDao.insertOrUpdate(place);
                                }
                            }
                        }

                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseCheckOutErrorMessage(response);
                    Log.e(TAG, "checkOutReservation failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CheckOutResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "checkOutReservation network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Accepts and confirms a pending reservation by the parking spot owner via POST /api/reservations/{id}/accept
     * and updates the reservation status to CONFIRMED in Room database cache.
     *
     * @param reservationId Unique identifier of the reservation
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with accepted {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> acceptReservation(@NonNull String reservationId,
                                                                     @Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        apiService.acceptReservation(reservationId.trim()).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse body = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                        ReservationEntity entity = toEntity(body, userEmail, place);
                        entity.setStatus(body.getStatus() != null && !body.getStatus().isEmpty() ? body.getStatus() : "CONFIRMED");
                        reservationDao.insertOrUpdate(entity);

                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "acceptReservation failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "acceptReservation network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Rejects a pending reservation request by the parking spot owner via POST /api/reservations/{id}/reject
     * and updates the status to REJECTED in Room database cache.
     *
     * @param reservationId Unique identifier of the reservation
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with rejected {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> rejectReservation(@NonNull String reservationId,
                                                                     @Nullable String userEmail) {
        return rejectReservation(reservationId, null, userEmail);
    }

    /**
     * Rejects a pending reservation request by the parking spot owner via POST /api/reservations/{id}/reject
     * with an explanation reason and updates the status to REJECTED in Room database cache.
     *
     * @param reservationId Unique identifier of the reservation
     * @param reason        Rejection explanation reason
     * @param userEmail     Current user's email address for local account isolation
     * @return LiveData emitting Resource with rejected {@link ReservationResponse}
     */
    public LiveData<Resource<ReservationResponse>> rejectReservation(@NonNull String reservationId,
                                                                     @Nullable String reason,
                                                                     @Nullable String userEmail) {
        MutableLiveData<Resource<ReservationResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (reservationId == null || reservationId.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowy identyfikator rezerwacji", null, 400));
            return resultLiveData;
        }

        RejectReservationRequest req = new RejectReservationRequest(reason != null ? reason : "");
        apiService.rejectReservationWithReason(reservationId.trim(), req).enqueue(new Callback<ReservationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReservationResponse> call,
                                   @NonNull Response<ReservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReservationResponse body = response.body();
                    diskExecutor.execute(() -> {
                        ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(body.getSpotId());
                        if (place != null) {
                            place.setOccupied(false);
                            parkingPlaceDao.insertOrUpdate(place);
                        }

                        ReservationEntity entity = toEntity(body, userEmail, place);
                        entity.setStatus(body.getStatus() != null && !body.getStatus().isEmpty() ? body.getStatus() : "REJECTED");
                        reservationDao.insertOrUpdate(entity);

                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "rejectReservation failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "rejectReservation network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Calculates a preliminary price quote and fee breakdown via POST /api/reservations/quote.
     *
     * @param request QuoteRequest payload containing spotId, startsAt, and endsAt
     * @return LiveData emitting Resource with calculated {@link QuoteResponse}
     */
    public LiveData<Resource<QuoteResponse>> calculateQuote(@NonNull QuoteRequest request) {
        MutableLiveData<Resource<QuoteResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        if (request == null || request.getSpotId() == null || request.getSpotId().trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Nieprawidłowe dane zapytania o kalkulację ceny", null, 400));
            return resultLiveData;
        }

        apiService.calculateQuote(request).enqueue(new Callback<QuoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<QuoteResponse> call,
                                   @NonNull Response<QuoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "calculateQuote failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<QuoteResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "calculateQuote network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Parses backend error response for check-out requests into human-readable messages.
     * Handles HTTP 400/409 (e.g. if check-out attempted before check-in or already completed).
     *
     * @param response Retrofit response
     * @return User-facing error message
     */
    private String parseCheckOutErrorMessage(Response<?> response) {
        String defaultMsg = "Nie można zakończyć parkowania (HTTP " + response.code() + ")";
        if (response.code() == 409) {
            defaultMsg = "Parkowanie dla tej rezerwacji zostało już zakończone.";
        } else if (response.code() == 400) {
            defaultMsg = "Nie można zakończyć parkowania, które nie zostało rozpoczęte (wymagany Check-in).";
        } else if (response.code() == 404) {
            defaultMsg = "Nie znaleziono wybranej rezerwacji.";
        } else if (response.code() == 403) {
            defaultMsg = "Brak uprawnień do zakończenia tej rezerwacji.";
        }

        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                JsonObject obj = JsonParser.parseString(errorBodyStr).getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    return obj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }

    /**
     * Parses backend error response for cancel reservation requests into human-readable messages.
     * Handles HTTP 409/400 (e.g. if reservation is already active or completed and cannot be canceled).
     *
     * @param response Retrofit response
     * @return User-facing error message
     */
    private String parseCancelErrorMessage(Response<?> response) {
        String defaultMsg = "Nie można anulować rezerwacji (HTTP " + response.code() + ")";
        if (response.code() == 409 || response.code() == 400) {
            defaultMsg = "Nie można anulować rezerwacji: parkowanie zostało już rozpoczęte lub zakończone.";
        } else if (response.code() == 404) {
            defaultMsg = "Nie znaleziono wybranej rezerwacji.";
        } else if (response.code() == 403) {
            defaultMsg = "Brak uprawnień do anulowania tej rezerwacji.";
        }

        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                JsonObject obj = JsonParser.parseString(errorBodyStr).getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    String serverMsg = obj.get("message").getAsString();
                    if (response.code() == 409 || response.code() == 400) {
                        return "Nie można anulować rezerwacji (" + serverMsg + ")";
                    }
                    return serverMsg;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }

    /**
     * Parses backend error response for check-in requests into human-readable messages.
     * Handles HTTP 400/409 (e.g. if check-in is attempted before start time or already checked-in).
     *
     * @param response Retrofit response
     * @return User-facing error message
     */
    private String parseCheckInErrorMessage(Response<?> response) {
        String defaultMsg = "Nie można rozpocząć parkowania (HTTP " + response.code() + ")";
        if (response.code() == 409) {
            defaultMsg = "Parkowanie dla tej rezerwacji zostało już wcześniej rozpoczęte.";
        } else if (response.code() == 400) {
            defaultMsg = "Nie można rozpocząć parkowania przed czasem rozpoczęcia rezerwacji.";
        } else if (response.code() == 404) {
            defaultMsg = "Nie znaleziono wybranej rezerwacji.";
        } else if (response.code() == 403) {
            defaultMsg = "Brak uprawnień do zameldowania na tym miejscu.";
        }

        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                JsonObject obj = JsonParser.parseString(errorBodyStr).getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    return obj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }

    /**
     * Parses backend error response body into a human-readable error description.
     *
     * @param response Retrofit response
     * @return User-facing error message
     */
    private String parseErrorMessage(Response<?> response) {
        String defaultMsg = "Błąd rezerwacji: HTTP " + response.code();

        if (response.code() == 409) {
            defaultMsg = "To miejsce jest już zajęte w wybranym terminie!";
        } else if (response.code() == 402) {
            defaultMsg = "Płatność została odrzucona.";
        } else if (response.code() == 403) {
            defaultMsg = "Nie możesz zarezerwować własnego miejsca parkingowego!";
        } else if (response.code() == 404) {
            defaultMsg = "Nie znaleziono wybranego miejsca parkingowego lub pojazdu.";
        }

        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                JsonObject obj = JsonParser.parseString(errorBodyStr).getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    String serverMsg = obj.get("message").getAsString();
                    if (response.code() == 409) {
                        return "To miejsce jest już zajęte w wybranym terminie! (" + serverMsg + ")";
                    }
                    return serverMsg;
                }
            }
        } catch (Exception ignored) {
        }

        return defaultMsg;
    }

    /**
     * Converts network ReservationResponse DTO into local Room ReservationEntity.
     * Maps ISO-8601 startsAt and endsAt timestamps into local millisecond values,
     * human-readable dateString, and timeRange.
     *
     * @param res       ReservationResponse DTO
     * @param userEmail User email
     * @param place     ParkingPlaceEntity instance (optional)
     * @return Converted ReservationEntity
     */
    public static ReservationEntity toEntity(ReservationResponse res, String userEmail, ParkingPlaceEntity place) {
        ReservationEntity entity = new ReservationEntity();
        if (res.getId() != null && !res.getId().isEmpty()) {
            entity.setId(res.getId());
        }
        entity.setUserEmail(userEmail != null ? userEmail : "");
        entity.setPlaceId(res.getSpotId());
        entity.setPlaceName(res.getSpotName() != null && !res.getSpotName().isEmpty()
                ? res.getSpotName() : (place != null ? place.getTitle() : "Miejsce parkingowe"));
        if (place != null) {
            entity.setStreet(place.getStreet());
            entity.setCity(place.getCity());
            entity.setPlaceNumber(place.getPlaceNumber());
            entity.setPhotoResId(place.getPhotoResId() != 0 ? place.getPhotoResId() : R.drawable.default_parking_spot);
        } else {
            entity.setStreet(res.getSpotName() != null && !res.getSpotName().isEmpty() ? res.getSpotName() : "Warszawa");
            entity.setCity("Warszawa");
            entity.setPlaceNumber("");
            entity.setPhotoResId(R.drawable.default_parking_spot);
        }
        entity.setPinCode(res.getPinCode() != null && !res.getPinCode().isEmpty() ? res.getPinCode() : "4829");
        entity.setStatus(res.getStatus() != null && !res.getStatus().isEmpty() ? res.getStatus() : "AKTYWNA");
        entity.setTotalPrice(res.getTotal());
        entity.setCarPlate(res.getVehiclePlate());
        entity.setVehicleInfo(res.getVehiclePlate());
        if (res.getDriverId() != null && !res.getDriverId().isEmpty()) {
            entity.setUserName(res.getDriverId());
        }

        // Accurately map timestamps from backend response
        long startMs = res.getStartsAtMillis() > 0 ? res.getStartsAtMillis() : System.currentTimeMillis();
        long endMs = res.getEndsAtMillis() > 0 ? res.getEndsAtMillis() : (startMs + 3600000L);
        entity.setStartTimeMs(startMs);
        entity.setEndTimeMs(endMs);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        entity.setDateString(dateFormat.format(new Date(startMs)));
        entity.setTimeRange(timeFormat.format(new Date(startMs)) + " - " + timeFormat.format(new Date(endMs)));

        return entity;
    }
}
