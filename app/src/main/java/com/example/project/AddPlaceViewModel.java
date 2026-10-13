package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class AddPlaceViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<ParkingPlace> editingPlaceLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<Integer> errorMessageResId = new MutableLiveData<>();

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

    public void loadPlaceForEditing(String placeId) {
        if (placeId != null && !placeId.isEmpty()) {
            ParkingPlace place = repository.getPlaceById(placeId);
            if (place != null) {
                editingPlaceLiveData.setValue(place);
            }
        }
    }

    public void savePlace(String placeId, String street, String city, String postalCode,
                          String placeNumber, String floor, String accessTime,
                          String priceStr, List<String> amenities) {
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

        repository.addPlace(place); // Saves or updates in repository (initially free / isOccupied = false)
        saveSuccess.setValue(true);
    }
}