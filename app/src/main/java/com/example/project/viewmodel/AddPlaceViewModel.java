package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.R;
import com.example.project.model.ParkingPlace;
import com.example.project.repository.AppRepository;

import java.util.List;

/**
 * ViewModel for adding, editing and deleting parking places.
 */
public class AddPlaceViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<ParkingPlace> editingPlaceLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<Integer> errorMessageResId = new MutableLiveData<>();

    /**
     * Default constructor for AddPlaceViewModel.
     */
    public AddPlaceViewModel() {
        this.repository = AppRepository.getInstance();
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

    /**
     * Loads a place from repository for editing if ID is provided.
     *
     * @param placeId ID of the place to edit
     */
    public void loadPlaceForEditing(String placeId) {
        if (placeId != null && !placeId.isEmpty()) {
            ParkingPlace place = repository.getPlaceById(placeId);
            if (place != null) {
                editingPlaceLiveData.setValue(place);
            }
        }
    }

    /**
     * Deletes a parking place from repository by ID.
     *
     * @param placeId ID of the place to delete
     */
    public void deletePlace(String placeId) {
        if (placeId != null && !placeId.isEmpty()) {
            repository.deletePlace(placeId);
        }
    }

    /**
     * Validates and saves or updates a parking place in the repository.
     *
     * @param placeId    ID if editing, null if creating new
     * @param street     Street name
     * @param city       City name
     * @param postalCode Postal code
     * @param placeNumber Spot number
     * @param floor      Floor level
     * @param accessTime Availability hours
     * @param priceStr   Hourly price string
     * @param amenities  Selected amenities list
     * @param ownerEmail Owner email address
     * @param photoUri   Uri string of selected photo
     */
    public void savePlace(String placeId, String street, String city, String postalCode,
                          String placeNumber, String floor, String accessTime,
                          String priceStr, List<String> amenities, String ownerEmail, String photoUri) {
        if (street == null || street.trim().isEmpty()
                || city == null || city.trim().isEmpty()
                || placeNumber == null || placeNumber.trim().isEmpty()) {
            errorMessageResId.setValue(R.string.msg_validation_error);
            return;
        }

        double parsedPrice = 12.0;
        if (priceStr != null && !priceStr.trim().isEmpty()) {
            try {
                parsedPrice = Double.parseDouble(priceStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String finalId = (placeId != null && !placeId.isEmpty()) ? placeId : "p_" + System.currentTimeMillis();
        String title = "Miejsce " + placeNumber.trim() + " - " + street.trim();
        String finalAccessTime = (accessTime != null && !accessTime.trim().isEmpty()) ? accessTime.trim() : "08:00 - 20:00";

        ParkingPlace place = new ParkingPlace(
                finalId,
                title,
                street.trim(),
                city.trim(),
                postalCode != null ? postalCode.trim() : "",
                placeNumber.trim(),
                floor != null ? floor.trim() : "",
                parsedPrice,
                finalAccessTime,
                amenities,
                52.2297 + (Math.random() - 0.5) * 0.02,
                21.0122 + (Math.random() - 0.5) * 0.02
        );

        place.setOwnerEmail(ownerEmail != null ? ownerEmail : "");
        place.setImageUri(photoUri != null ? photoUri : "");

        repository.addPlace(place);
        saveSuccess.setValue(true);
    }
}