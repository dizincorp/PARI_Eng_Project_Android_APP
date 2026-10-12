package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AvailabilityCalendarViewModel extends ViewModel {

    private final MutableLiveData<Boolean> blockSlotSuccess = new MutableLiveData<>();

    public LiveData<Boolean> getBlockSlotSuccess() {
        return blockSlotSuccess;
    }

    public void blockSelectedSlot() {
        blockSlotSuccess.setValue(true);
    }
}