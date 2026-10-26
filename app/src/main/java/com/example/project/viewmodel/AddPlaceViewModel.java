package com.example.project.viewmodel;

import android.app.Application;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.project.R;
import com.example.project.model.ParkingPlace;
import com.example.project.network.Resource;
import com.example.project.network.model.CreateSpotRequest;
import com.example.project.network.model.ParkingSpot;
import com.example.project.network.model.UpdateSpotRequest;
import com.example.project.repository.AppRepository;
import com.example.project.repository.SpotRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel managing parking spot creation, edition, and deletion.
 * Fully aligned with REST API endpoint POST /api/spots and resolves geographic
 * coordinates using Google Maps {@link Geocoder}.
 */
public class AddPlaceViewModel extends AndroidViewModel {

    private static final String TAG = "AddPlaceViewModel";

    private final SpotRepository spotRepository;
    private final AppRepository appRepository;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private final MutableLiveData<ParkingPlace> editingPlaceLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<Integer> errorMessageResId = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<Resource<ParkingSpot>> createSpotStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> accessTimeLiveData = new MutableLiveData<>("08:00 - 20:00");

    /**
     * Public constructor initializing dependencies with Android Application context.
     *
     * @param application Application context
     */
    public AddPlaceViewModel(@NonNull Application application) {
        super(application);
        this.spotRepository = SpotRepository.getInstance(application);
        this.appRepository = AppRepository.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Parameterized constructor for dependency injection during testing.
     *
     * @param application     Application context
     * @param spotRepository  Spot repository instance
     * @param appRepository   App local repository instance
     * @param executorService Asynchronous background executor
     */
    public AddPlaceViewModel(@NonNull Application application,
                             @NonNull SpotRepository spotRepository,
                             @NonNull AppRepository appRepository,
                             @NonNull ExecutorService executorService) {
        super(application);
        this.spotRepository = spotRepository;
        this.appRepository = appRepository;
        this.executorService = executorService;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<ParkingPlace> getEditingPlaceLiveData() {
        return editingPlaceLiveData;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<Integer> getErrorMessageResId() {
        return errorMessageResId;
    }

    public LiveData<String> getToastMessageLiveData() {
        return toastMessageLiveData;
    }

    public LiveData<Resource<ParkingSpot>> getCreateSpotStatusLiveData() {
        return createSpotStatusLiveData;
    }

    public LiveData<String> getAccessTimeLiveData() {
        return accessTimeLiveData;
    }

    public void setAccessTime(@Nullable String accessTime) {
        if (accessTime != null && !accessTime.trim().isEmpty()) {
            accessTimeLiveData.setValue(accessTime.trim());
        }
    }

    /**
     * Loads a place from repository for editing if ID is provided.
     *
     * @param placeId ID of the place to edit
     */
    public void loadPlaceForEditing(@Nullable String placeId) {
        if (placeId != null && !placeId.isEmpty()) {
            ParkingPlace place = appRepository.getPlaceById(placeId);
            if (place != null) {
                editingPlaceLiveData.setValue(place);
                if (place.getAccessTime() != null && !place.getAccessTime().trim().isEmpty()) {
                    accessTimeLiveData.setValue(place.getAccessTime().trim());
                }
            }
        }
    }

    /**
     * Activates a parking spot on the backend via POST /api/spots/{id}/activate and updates Room DB.
     *
     * @param spotId Spot ID to activate
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
     * Deactivates a parking spot on the backend via POST /api/spots/{id}/deactivate and updates Room DB.
     *
     * @param spotId Spot ID to deactivate
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
     * Updates an existing parking spot on backend via PUT /api/spots/{id} and updates Room DB.
     *
     * @param spotId  Spot ID
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
     * Deletes a parking spot from backend via DELETE /api/spots/{id} and Room DB.
     *
     * @param spotId Spot ID to delete
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
                        appRepository.deletePlace(spotId.trim());
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
     * Deletes a parking place from both Room Database and local repository.
     *
     * @param placeId ID of the place to delete
     */
    public void deletePlace(@Nullable String placeId) {
        deleteSpot(placeId);
    }

    /**
     * Validates spot details matching POST /api/spots endpoint, resolves coordinates via Geocoding,
     * and submits creation request through {@link SpotRepository}.
     *
     * @param placeId             ID if editing, null if creating new
     * @param name                Custom spot name
     * @param description         Spot detailed description
     * @param street              Street name and house number
     * @param city                City
     * @param postalCode          Postal code (e.g. 20-314)
     * @param placeNumber         Place number / identifier
     * @param floor               Floor level
     * @param lengthCm            Spot length in cm
     * @param widthCm             Spot width in cm
     * @param heightCm            Spot height in cm
     * @param priceStr            Hourly price string
     * @param accessInstructions Entry instructions
     * @param accessTime          Access hours
     * @param autoAccept          Whether to auto-accept bookings
     * @param requestVerification Whether to request admin verification
     * @param ownershipDeclared   Confirmation of ownership (mandatory)
     * @param amenities           List of amenities
     * @param ownerEmail          Current user email
     * @param photoUri            Selected image URI
     */
    public void savePlace(@Nullable String placeId,
                          @Nullable String name,
                          @Nullable String description,
                          @Nullable String street,
                          @Nullable String city,
                          @Nullable String postalCode,
                          @Nullable String placeNumber,
                          @Nullable String floor,
                          int lengthCm,
                          int widthCm,
                          int heightCm,
                          @Nullable String priceStr,
                          @Nullable String accessInstructions,
                          @Nullable String accessTime,
                          boolean autoAccept,
                          boolean requestVerification,
                          boolean ownershipDeclared,
                          @Nullable List<String> amenities,
                          @Nullable String ownerEmail,
                          @Nullable String photoUri) {

        // Validate mandatory ownership declaration required by backend
        if (!ownershipDeclared) {
            errorMessageResId.setValue(R.string.error_ownership_required);
            return;
        }

        // Validate mandatory address and name fields
        if (street == null || street.trim().isEmpty()
                || city == null || city.trim().isEmpty()) {
            errorMessageResId.setValue(R.string.msg_validation_error);
            return;
        }

        // Validate dimensions
        if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            errorMessageResId.setValue(R.string.error_dimensions_invalid);
            return;
        }

        double parsedPrice = 12.0;
        if (priceStr != null && !priceStr.trim().isEmpty()) {
            try {
                parsedPrice = Double.parseDouble(priceStr.trim());
            } catch (NumberFormatException ignored) {
                parsedPrice = 12.0;
            }
        }
        if (parsedPrice <= 0) {
            parsedPrice = 1.0;
        }

        final double finalPrice = parsedPrice;
        final String spotName = (name != null && !name.trim().isEmpty())
                ? name.trim()
                : "Miejsce " + (placeNumber != null ? placeNumber.trim() : "") + " - " + street.trim();

        final String spotDescription = (description != null && !description.trim().isEmpty())
                ? description.trim()
                : "Miejsce parkingowe " + spotName;

        final String finalInstructions = (accessInstructions != null && !accessInstructions.trim().isEmpty()) ? accessInstructions.trim() : "";

        createSpotStatusLiveData.setValue(Resource.loading(null));

        executorService.execute(() -> {
            // Geocode address using Android Google Geocoder
            double lat = 51.2465;
            double lng = 22.5684;
            String addressQuery = street.trim() + ", " + (postalCode != null ? postalCode.trim() : "")
                    + " " + city.trim() + ", Poland";

            try {
                Geocoder geocoder = new Geocoder(getApplication(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(addressQuery, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address found = addresses.get(0);
                    lat = found.getLatitude();
                    lng = found.getLongitude();
                } else {
                    if (city.trim().equalsIgnoreCase("Warszawa") || city.trim().equalsIgnoreCase("Warsaw")) {
                        lat = 52.2297;
                        lng = 21.0122;
                    } else if (city.trim().equalsIgnoreCase("KrakÃ³w") || city.trim().equalsIgnoreCase("Krakow")) {
                        lat = 50.0647;
                        lng = 19.9450;
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                Log.e(TAG, "Geocoding failed for address: " + addressQuery, e);
            }

            // Map UI amenities to backend enum strings
            List<String> backendAmenities = new ArrayList<>();
            if (amenities != null) {
                for (String item : amenities) {
                    if (item == null) continue;
                    String trimmed = item.trim();
                    if ("Monitoring".equalsIgnoreCase(trimmed) || "CCTV".equalsIgnoreCase(trimmed)) {
                        backendAmenities.add("CCTV");
                    } else if ("OÅ›wietlenie".equalsIgnoreCase(trimmed) || "LIGHTING".equalsIgnoreCase(trimmed)) {
                        backendAmenities.add("LIGHTING");
                    } else if ("EV Charging".equalsIgnoreCase(trimmed) || "Åadowanie EV".equalsIgnoreCase(trimmed) || "EV_CHARGER".equalsIgnoreCase(trimmed)) {
                        backendAmenities.add("EV_CHARGER");
                    } else if ("Podziemny".equalsIgnoreCase(trimmed) || "UNDERGROUND".equalsIgnoreCase(trimmed)) {
                        backendAmenities.add("UNDERGROUND");
                    } else if ("Brama automatyczna".equalsIgnoreCase(trimmed) || "GATE".equalsIgnoreCase(trimmed)) {
                        backendAmenities.add("GATE");
                    } else if ("Zadaszone".equalsIgnoreCase(trimmed) || "COVERED".equalsIgnoreCase(trimmed)) {
                        backendAmenities.add("COVERED");
                    }
                }
            }

            // Construct CreateSpotRequest payload matching POST /api/spots
            CreateSpotRequest request = new CreateSpotRequest();
            request.setName(spotName);
            request.setDescription(spotDescription);
            request.setStreet(street.trim());
            request.setCity(city.trim());
            request.setPostalCode(postalCode != null ? postalCode.trim() : "");
            request.setCountry("PL"); // Strictly ISO 2-letter country code for DB VARCHAR(2)
            request.setLat(lat);
            request.setLng(lng);
            request.setLengthCm(lengthCm);
            request.setWidthCm(widthCm);
            request.setHeightCm(heightCm);
            request.setHourlyPrice(finalPrice);
            request.setAccessInstructions(finalInstructions);
            request.setAutoAccept(autoAccept);
            request.setOwnershipDeclared(ownershipDeclared);
            request.setRequestVerification(requestVerification);
            request.setAmenities(backendAmenities);

            List<String> photoKeys = new ArrayList<>();
            if (photoUri != null && !photoUri.isEmpty()) {
                photoKeys.add("spot_" + System.currentTimeMillis() + ".jpg");
            }
            request.setPhotoKeys(photoKeys);

            final double finalLat = lat;
            final double finalLng = lng;

            // Submit network request on main thread through LiveData
            mainHandler.post(() -> {
                if (placeId != null && !placeId.trim().isEmpty()) {
                    // Spot Update flow via PUT /api/spots/{id}
                    UpdateSpotRequest updateRequest = new UpdateSpotRequest();
                    updateRequest.setName(spotName);
                    updateRequest.setDescription(spotDescription);
                    updateRequest.setStreet(street.trim());
                    updateRequest.setCity(city.trim());
                    updateRequest.setPostalCode(postalCode != null ? postalCode.trim() : "");
                    updateRequest.setCountry("PL");
                    updateRequest.setLat(finalLat);
                    updateRequest.setLng(finalLng);
                    updateRequest.setLengthCm(lengthCm);
                    updateRequest.setWidthCm(widthCm);
                    updateRequest.setHeightCm(heightCm);
                    updateRequest.setHourlyPrice(finalPrice);
                    updateRequest.setAccessInstructions(finalInstructions);
                    updateRequest.setAutoAccept(autoAccept);
                    updateRequest.setOwnershipDeclared(ownershipDeclared);
                    updateRequest.setRequestVerification(requestVerification);
                    updateRequest.setAmenities(backendAmenities);
                    updateRequest.setPhotoKeys(photoKeys);

                    LiveData<Resource<ParkingSpot>> repoLiveData = spotRepository.updateSpot(placeId.trim(), updateRequest);
                    repoLiveData.observeForever(new Observer<Resource<ParkingSpot>>() {
                        @Override
                        public void onChanged(Resource<ParkingSpot> resource) {
                            if (resource == null) return;
                            createSpotStatusLiveData.setValue(resource);

                            if (resource.isSuccess()) {
                                repoLiveData.removeObserver(this);
                                ParkingPlace localPlace = new ParkingPlace(
                                        placeId.trim(),
                                        updateRequest.getName(),
                                        updateRequest.getStreet(),
                                        updateRequest.getCity(),
                                        updateRequest.getPostalCode(),
                                        placeNumber != null ? placeNumber.trim() : "1",
                                        floor != null ? floor.trim() : "",
                                        finalPrice,
                                        accessTime != null ? accessTime.trim() : "08:00 - 20:00",
                                        amenities,
                                        finalLat,
                                        finalLng
                                );
                                localPlace.setDescription(spotDescription);
                                localPlace.setAccessInstructions(finalInstructions);
                                localPlace.setOwnerEmail(ownerEmail != null ? ownerEmail : "");
                                localPlace.setImageUri(photoUri != null ? photoUri : "");
                                appRepository.addPlace(localPlace);
                                toastMessageLiveData.setValue(getApplication().getString(R.string.toast_spot_updated));
                                saveSuccess.setValue(true);
                            } else if (resource.isError()) {
                                repoLiveData.removeObserver(this);
                                String msg = resource.getMessage() != null ? resource.getMessage() : "Błąd aktualizacji";
                                toastMessageLiveData.setValue(getApplication().getString(R.string.error_spot_operation_failed, msg));
                                saveSuccess.setValue(false);
                            }
                        }
                    });
                } else {
                    // Spot Creation flow via POST /api/spots
                    LiveData<Resource<ParkingSpot>> repoLiveData = spotRepository.createSpot(request, ownerEmail);
                    repoLiveData.observeForever(new Observer<Resource<ParkingSpot>>() {
                        @Override
                        public void onChanged(Resource<ParkingSpot> resource) {
                            if (resource == null) return;
                            createSpotStatusLiveData.setValue(resource);

                            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                                repoLiveData.removeObserver(this);
                                String finalId = resource.data.getId() != null ? resource.data.getId() : "p_" + System.currentTimeMillis();
                                ParkingPlace localPlace = new ParkingPlace(
                                        finalId,
                                        request.getName(),
                                        request.getStreet(),
                                        request.getCity(),
                                        request.getPostalCode(),
                                        placeNumber != null ? placeNumber.trim() : "1",
                                        floor != null ? floor.trim() : "",
                                        finalPrice,
                                        accessTime != null ? accessTime.trim() : "08:00 - 20:00",
                                        amenities,
                                        finalLat,
                                        finalLng
                                );
                                localPlace.setDescription(spotDescription);
                                localPlace.setAccessInstructions(finalInstructions);
                                localPlace.setOwnerEmail(ownerEmail != null ? ownerEmail : "");
                                localPlace.setImageUri(photoUri != null ? photoUri : "");
                                appRepository.addPlace(localPlace);
                                toastMessageLiveData.setValue(getApplication().getString(R.string.toast_spot_updated));
                                saveSuccess.setValue(true);
                            } else if (resource.status == Resource.Status.ERROR) {
                                repoLiveData.removeObserver(this);
                                saveSuccess.setValue(false);
                            }
                        }
                    });
                }
            });
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
