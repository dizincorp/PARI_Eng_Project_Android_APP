package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.project.R;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Transaction;
import com.example.project.network.Resource;
import com.example.project.network.model.ParkingSpot;
import com.example.project.network.model.UpdateSpotRequest;
import com.example.project.repository.AppRepository;
import com.example.project.repository.SpotRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel for the Places dashboard screen managing owner's parking spots list,
 * real-time earnings, and CRUD lifecycle operations (activate, deactivate, update, delete).
 */
public class PlacesViewModel extends AndroidViewModel {

    private final AppRepository appRepository;
    private final SpotRepository spotRepository;

    private final MutableLiveData<String> greetingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> subtitleLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Resource<ParkingSpot>> spotOperationStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);

    /**
     * Default constructor for PlacesViewModel initializing repositories from application context.
     *
     * @param application Application instance
     */
    public PlacesViewModel(@NonNull Application application) {
        this(application, AppRepository.getInstance(), SpotRepository.getInstance(application));
    }

    /**
     * Parameterized constructor for dependency injection during testing.
     *
     * @param application    Application instance
     * @param appRepository  AppRepository instance
     * @param spotRepository SpotRepository instance
     */
    public PlacesViewModel(@NonNull Application application,
                           @NonNull AppRepository appRepository,
                           @NonNull SpotRepository spotRepository) {
        super(application);
        this.appRepository = appRepository;
        this.spotRepository = spotRepository;
        initData();
    }

    private void initData() {
        String userName = appRepository.getUserNameLiveData().getValue();
        String nameToDisplay = (userName != null && !userName.isEmpty()) ? userName : "Jan";
        greetingLiveData.setValue("Cześć, " + nameToDisplay);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("pl", "PL"));
        String monthYear = sdf.format(calendar.getTime());
        if (!monthYear.isEmpty()) {
            monthYear = monthYear.substring(0, 1).toUpperCase(new Locale("pl", "PL")) + monthYear.substring(1);
        }
        subtitleLiveData.setValue("Dashboard • " + monthYear);
    }

    public LiveData<String> getGreetingLiveData() {
        return greetingLiveData;
    }

    public LiveData<String> getSubtitleLiveData() {
        return subtitleLiveData;
    }

    public LiveData<List<ParkingPlace>> getMyPlacesLiveData() {
        return appRepository.getMyPlacesLiveData();
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return appRepository.getTransactionsLiveData();
    }

    public LiveData<String> getToastMessageLiveData() {
        return toastMessageLiveData;
    }

    public LiveData<Resource<ParkingSpot>> getSpotOperationStatusLiveData() {
        return spotOperationStatusLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * Refreshes owner parking spots from REST API (GET /api/spots/mine)
     * and synchronizes them into local Room Database Single Source of Truth.
     */
    public void refreshPlaces() {
        isLoadingLiveData.setValue(true);
        LiveData<Resource<List<ParkingSpot>>> source = spotRepository.fetchMySpots();
        source.observeForever(new Observer<Resource<List<ParkingSpot>>>() {
            @Override
            public void onChanged(Resource<List<ParkingSpot>> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    isLoadingLiveData.setValue(false);
                    if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : "BÅ‚Ä…d pobierania miejsc";
                        errorMessageLiveData.setValue(msg);
                    }
                }
            }
        });
    }

    /**
     * Activates a parking spot on the backend and updates Room Database.
     *
     * @param spotId Unique spot identifier
     */
    public void activateSpot(@Nullable String spotId) {
        if (spotId == null || spotId.trim().isEmpty()) {
            toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, "Brak ID"));
            return;
        }

        LiveData<Resource<ParkingSpot>> source = spotRepository.activateSpot(spotId.trim());
        source.observeForever(new Observer<Resource<ParkingSpot>>() {
            @Override
            public void onChanged(Resource<ParkingSpot> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    spotOperationStatusLiveData.setValue(resource);
                    if (resource.isSuccess()) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_spot_activated));
                    } else if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : "Błąd aktywacji";
                        toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, msg));
                    }
                }
            }
        });
    }

    /**
     * Deactivates a parking spot on the backend and updates Room Database.
     *
     * @param spotId Unique spot identifier
     */
    public void deactivateSpot(@Nullable String spotId) {
        if (spotId == null || spotId.trim().isEmpty()) {
            toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, "Brak ID"));
            return;
        }

        LiveData<Resource<ParkingSpot>> source = spotRepository.deactivateSpot(spotId.trim());
        source.observeForever(new Observer<Resource<ParkingSpot>>() {
            @Override
            public void onChanged(Resource<ParkingSpot> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    spotOperationStatusLiveData.setValue(resource);
                    if (resource.isSuccess()) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_spot_deactivated));
                    } else if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : "Błąd dezaktywacji";
                        toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, msg));
                    }
                }
            }
        });
    }

    /**
     * Updates an existing parking spot on the backend and updates Room Database.
     *
     * @param spotId  Unique spot identifier
     * @param request UpdateSpotRequest payload
     */
    public void updateSpot(@Nullable String spotId, @NonNull UpdateSpotRequest request) {
        if (spotId == null || spotId.trim().isEmpty()) {
            toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, "Brak ID"));
            return;
        }

        LiveData<Resource<ParkingSpot>> source = spotRepository.updateSpot(spotId.trim(), request);
        source.observeForever(new Observer<Resource<ParkingSpot>>() {
            @Override
            public void onChanged(Resource<ParkingSpot> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    spotOperationStatusLiveData.setValue(resource);
                    if (resource.isSuccess()) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_spot_updated));
                    } else if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : "Błąd aktualizacji";
                        toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, msg));
                    }
                }
            }
        });
    }

    /**
     * Deletes a parking spot both remotely via DELETE /api/spots/{id} and from Room DB.
     *
     * @param spotId Parking spot ID to delete
     */
    public void deleteSpot(@Nullable String spotId) {
        if (spotId == null || spotId.trim().isEmpty()) {
            toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, "Brak ID"));
            return;
        }

        LiveData<Resource<Void>> source = spotRepository.deleteSpotRemote(spotId.trim());
        source.observeForever(new Observer<Resource<Void>>() {
            @Override
            public void onChanged(Resource<Void> resource) {
                if (resource != null && !resource.isLoading()) {
                    source.removeObserver(this);
                    if (resource.isSuccess()) {
                        toastMessageLiveData.setValue(getApplication().getString(R.string.toast_spot_deleted));
                    } else if (resource.isError()) {
                        String msg = resource.getMessage() != null ? resource.getMessage() : "Błąd usuwania";
                        toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, msg));
                    }
                }
            }
        });
    }

    /**
     * Legacy method for local deletion compatibility.
     *
     * @param placeId Place ID
     */
    public void deletePlace(String placeId) {
        deleteSpot(placeId);
    }
}
