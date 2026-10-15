package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.model.UserProfile;
import com.example.project.repository.AppRepository;

/**
 * ViewModel for user profile management.
 */
public class ProfileViewModel extends ViewModel {

    private final AppRepository repository;

    /**
     * Default constructor for ProfileViewModel.
     */
    public ProfileViewModel() {
        this.repository = AppRepository.getInstance();
    }

    public LiveData<UserProfile> getUserProfileLiveData() {
        return repository.getUserProfileLiveData();
    }

    public void updateUserName(String newName) {
        repository.updateUserName(newName);
    }
}