package com.example.project.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Vehicle;
import com.example.project.network.Resource;
import com.example.project.network.model.CreateVehicleRequest;
import com.example.project.network.model.QuoteRequest;
import com.example.project.network.model.QuoteResponse;
import com.example.project.network.model.ReservationRequest;
import com.example.project.network.model.ReservationResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReservationRepository;
import com.example.project.repository.VehicleRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * ViewModel managing the reservation booking screen state, calculating price quotes,
 * validating calendar blocked periods, and dispatching booking requests to {@link ReservationRepository}.
 * Handles HTTP 409 Conflict errors specifically to alert the user about spot unavailability.
 */
public class ReservePlaceViewModel extends AndroidViewModel {

    private static final String TAG = "ReservePlaceViewModel";

    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final AppRepository appRepository;

    private final MediatorLiveData<Resource<ReservationResponse>> reservationStatusLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<QuoteResponse>> quoteLiveData = new MediatorLiveData<>();
    private final MutableLiveData<String> conflictErrorLiveData = new MutableLiveData<>();

    /**
     * Default constructor initializing dependencies using Android Application context.
     *
     * @param application Application instance
     */
    public ReservePlaceViewModel(@NonNull Application application) {
        this(application,
                ReservationRepository.getInstance(application),
                VehicleRepository.getInstance(application),
                AppRepository.getInstance());
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param application           Application instance
     * @param reservationRepository ReservationRepository instance
     * @param vehicleRepository     VehicleRepository instance
     * @param appRepository         AppRepository instance
     */
    public ReservePlaceViewModel(@NonNull Application application,
                                 @NonNull ReservationRepository reservationRepository,
                                 @NonNull VehicleRepository vehicleRepository,
                                 @NonNull AppRepository appRepository) {
        super(application);
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.appRepository = appRepository;
        refreshVehicles();
    }

    /**
     * Refreshes the driver's vehicles from the backend REST API via GET /api/vehicles
     * and persists them in the Room database.
     */
    public void refreshVehicles() {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        if (currentEmail == null || currentEmail.isEmpty()) {
            if (com.example.project.utils.SessionManager.getInstance() != null) {
                currentEmail = com.example.project.utils.SessionManager.getInstance().getUserEmail();
            }
        }
        vehicleRepository.fetchVehicles(currentEmail);
    }

    /**
     * Gets the LiveData stream of reservation operation results (Loading, Success, Error).
     *
     * @return LiveData containing Resource with ReservationResponse
     */
    public LiveData<Resource<ReservationResponse>> getReservationStatusLiveData() {
        return reservationStatusLiveData;
    }

    /**
     * Gets the LiveData stream emitting calculated pricing quotes from the server via POST /api/reservations/quote.
     *
     * @return LiveData containing Resource with QuoteResponse
     */
    public LiveData<Resource<QuoteResponse>> getQuoteLiveData() {
        return quoteLiveData;
    }

    /**
     * Gets dedicated LiveData emitting conflict error messages when a 409 Conflict HTTP status is received.
     *
     * @return LiveData containing conflict description message
     */
    public LiveData<String> getConflictErrorLiveData() {
        return conflictErrorLiveData;
    }

    /**
     * Gets available vehicles for the logged-in user.
     *
     * @return LiveData emitting list of Vehicle items
     */
    public LiveData<List<Vehicle>> getVehiclesLiveData() {
        return appRepository.getVehiclesLiveData();
    }

    /**
     * Gets the current user's email address.
     *
     * @return LiveData containing current user email
     */
    public LiveData<String> getUserEmailLiveData() {
        return appRepository.getUserEmailLiveData();
    }

    /**
     * Retrieves parking place details by ID.
     *
     * @param placeId Parking spot ID
     * @return ParkingPlace instance if found, or null
     */
    public ParkingPlace getPlaceById(String placeId) {
        return appRepository.getPlaceById(placeId);
    }

    /**
     * Observes blocked periods for a specific parking spot from Room DB.
     *
     * @param spotId Unique spot identifier
     * @return LiveData list of {@link BlockedPeriodEntity}
     */
    public LiveData<List<BlockedPeriodEntity>> getBlockedPeriodsLiveData(@NonNull String spotId) {
        return AppDatabase.getInstance(getApplication())
                .blockedPeriodDao()
                .getBlockedPeriodsForSpotLiveData(spotId);
    }

    /**
     * Synchronously checks whether the requested time window overlaps with any blocked period for the spot.
     *
     * @param spotId  Spot identifier
     * @param startMs Start time in milliseconds
     * @param endMs   End time in milliseconds
     * @return Conflicting BlockedPeriodEntity if found, null otherwise
     */
    @Nullable
    public BlockedPeriodEntity findConflictingBlockedPeriod(@NonNull String spotId, long startMs, long endMs) {
        if (spotId.trim().isEmpty()) return null;
        try {
            List<BlockedPeriodEntity> blocks = AppDatabase.getInstance(getApplication())
                    .blockedPeriodDao()
                    .getBlockedPeriodsForSpotSync(spotId);
            if (blocks != null) {
                for (BlockedPeriodEntity b : blocks) {
                    if (b != null && !b.isExpired() && b.isOverlapping(startMs, endMs)) {
                        return b;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking blocked periods for spot: " + spotId, e);
        }
        return null;
    }

    /**
     * Requests preliminary pricing breakdown from POST /api/reservations/quote.
     *
     * @param spotId  Target spot ID
     * @param startMs Start timestamp in milliseconds
     * @param endMs   End timestamp in milliseconds
     */
    public void fetchQuote(@NonNull String spotId, long startMs, long endMs) {
        if (spotId.trim().isEmpty() || startMs >= endMs) return;

        QuoteRequest request = new QuoteRequest(spotId, startMs, endMs);
        LiveData<Resource<QuoteResponse>> source = reservationRepository.calculateQuote(request);
        quoteLiveData.addSource(source, resource -> {
            quoteLiveData.setValue(resource);
            if (resource != null && !resource.isLoading()) {
                quoteLiveData.removeSource(source);
            }
        });
    }

    /**
     * Submits a reservation booking request to the backend REST API via {@link ReservationRepository}.
     * Emits state changes via {@link #getReservationStatusLiveData()} and triggers
     * {@link #getConflictErrorLiveData()} if the spot is already occupied (HTTP 409 Conflict).
     *
     * @param spotId             Target parking spot UUID
     * @param vehicleId          Driver's vehicle UUID
     * @param startMs            Start time in milliseconds
     * @param endMs              End time in milliseconds
     * @param paymentMethodToken Payment method token (defaults to "pm_card_visa")
     */
    public void submitReservation(@NonNull String spotId,
                                  @NonNull String vehicleId,
                                  long startMs,
                                  long endMs,
                                  @Nullable String paymentMethodToken) {
        if (spotId.trim().isEmpty()) {
            reservationStatusLiveData.setValue(Resource.error("Brak identyfikatora miejsca parkingowego", null, 400));
            return;
        }

        if (startMs >= endMs) {
            reservationStatusLiveData.setValue(Resource.error("Czas zakończenia musi być późniejszy niż czas rozpoczęcia", null, 400));
            return;
        }

        // Format timestamps to standard ISO-8601 UTC Instant (e.g. 2026-10-25T17:24:55.983Z)
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String startIso = isoFormat.format(new Date(startMs));
        String endIso = isoFormat.format(new Date(endMs));

        String paymentToken = (paymentMethodToken != null && !paymentMethodToken.trim().isEmpty())
                ? paymentMethodToken.trim() : "pm_card_visa";

        ReservationRequest request = new ReservationRequest(
                spotId.trim(),
                vehicleId.trim(),
                startIso,
                endIso,
                paymentToken
        );

        String currentEmail = appRepository.getUserEmailLiveData().getValue();

        LiveData<Resource<ReservationResponse>> repoSource = reservationRepository.createReservation(request, currentEmail);
        reservationStatusLiveData.addSource(repoSource, resource -> {
            reservationStatusLiveData.setValue(resource);
            if (resource != null) {
                // Specifically detect and forward HTTP 409 Conflict state
                if (resource.isError()) {
                    if (resource.getCode() == 409 || (resource.getMessage() != null && resource.getMessage().toLowerCase().contains("zajęte"))) {
                        String conflictMsg = resource.getMessage() != null && !resource.getMessage().isEmpty()
                                ? resource.getMessage()
                                : "To miejsce jest już zajęte lub zablokowane w wybranym terminie!";
                        conflictErrorLiveData.setValue(conflictMsg);
                    }
                }

                if (!resource.isLoading()) {
                    reservationStatusLiveData.removeSource(repoSource);
                }
            }
        });
    }

    /**
     * Adds a newly registered vehicle to the backend REST API via POST /api/vehicles
     * and caches it in local repository and database without duplicating entries.
     *
     * @param model       Vehicle model
     * @param plateNumber License plate number
     */
    public void addVehicle(@NonNull String model, @NonNull String plateNumber) {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        CreateVehicleRequest request = new CreateVehicleRequest(model.trim(), plateNumber.trim());
        vehicleRepository.createVehicle(request, currentEmail);
    }

    /**
     * Adds a newly registered vehicle to the backend REST API via POST /api/vehicles.
     *
     * @param vehicle Vehicle instance
     */
    public void addVehicle(@Nullable Vehicle vehicle) {
        if (vehicle != null) {
            addVehicle(vehicle.getModel(), vehicle.getPlateNumber());
        }
    }
}
