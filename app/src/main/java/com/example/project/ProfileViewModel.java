package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<Integer> titleResId = new MutableLiveData<>();

    public ProfileViewModel() {
        titleResId.setValue(R.string.title_profile);
    }

    public LiveData<Integer> getTitleResId() {
        return titleResId;
    }
}