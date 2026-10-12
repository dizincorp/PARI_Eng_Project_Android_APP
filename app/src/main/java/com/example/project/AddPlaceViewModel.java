package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AddPlaceViewModel extends ViewModel {

    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<Integer> errorMessageResId = new MutableLiveData<>();

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<Integer> getErrorMessageResId() {
        return errorMessageResId;
    }

    public void savePlace(String street, String city, String placeNumber) {
        if (street == null || street.trim().isEmpty()
                || city == null || city.trim().isEmpty()
                || placeNumber == null || placeNumber.trim().isEmpty()) {
            errorMessageResId.setValue(R.string.msg_validation_error);
            return;
        }

        // Simulate successful save
        saveSuccess.setValue(true);
    }
}