package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ReservationsViewModel extends ViewModel {

    private final MutableLiveData<Integer> titleResId = new MutableLiveData<>();

    public ReservationsViewModel() {
        titleResId.setValue(R.string.title_reservations);
    }

    public LiveData<Integer> getTitleResId() {
        return titleResId;
    }
}