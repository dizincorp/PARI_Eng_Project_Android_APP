package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.R;

/**
 * ViewModel for reservations tab.
 */
public class ReservationsViewModel extends ViewModel {

    private final MutableLiveData<Integer> titleResId = new MutableLiveData<>();

    /**
     * Default constructor for ReservationsViewModel.
     */
    public ReservationsViewModel() {
        titleResId.setValue(R.string.title_reservations);
    }

    public LiveData<Integer> getTitleResId() {
        return titleResId;
    }
}