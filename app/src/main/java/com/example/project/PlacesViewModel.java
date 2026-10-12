package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlacesViewModel extends ViewModel {

    private final MutableLiveData<Integer> titleResId = new MutableLiveData<>();

    public PlacesViewModel() {
        titleResId.setValue(R.string.title_places);
    }

    public LiveData<Integer> getTitleResId() {
        return titleResId;
    }
}