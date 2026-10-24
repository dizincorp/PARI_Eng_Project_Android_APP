package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.dao.ReservationDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.ReservationApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.ReservationRequest;
import com.example.project.network.model.ReservationResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository enforcing the Single Source of Truth (SSOT) pattern for reservations.
 * Sends reservation requests to the backend REST API via {@link ReservationApiService}
 * and synchronizes confirmed reservations into the local Room Database.
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
                            parkingPlaceDao.insertOrUpdate(place);
                        }

                        ReservationEntity entity = toEntity(body, userEmail, place);
                        reservationDao.insertOrUpdate(entity);

                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = parseErrorMessage(response);
                    Log.e(TAG, "Reservation failed with HTTP " + response.code() + ": " + errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReservationResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "Reservation network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
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
        entity.setPlaceName(res.getSpotName() != null ? res.getSpotName() : (place != null ? place.getTitle() : ""));
        if (place != null) {
            entity.setStreet(place.getStreet());
            entity.setCity(place.getCity());
            entity.setPlaceNumber(place.getPlaceNumber());
            entity.setPhotoResId(place.getPhotoResId());
        }
        entity.setPinCode(res.getPinCode() != null ? res.getPinCode() : "4829");
        entity.setStatus("AKTYWNA");
        entity.setTotalPrice(res.getTotal());
        entity.setCarPlate(res.getVehiclePlate());
        entity.setVehicleInfo(res.getVehiclePlate());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        entity.setDateString(sdf.format(Calendar.getInstance().getTime()));

        return entity;
    }
}
