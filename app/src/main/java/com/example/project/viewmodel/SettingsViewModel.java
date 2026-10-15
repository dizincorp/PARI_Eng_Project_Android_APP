package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.model.UserProfile;
import com.example.project.repository.AppRepository;

/**
 * ViewModel for application settings screen.
 */
public class SettingsViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<String> languageLiveData = new MutableLiveData<>("Polski");
    private final MutableLiveData<Boolean> darkModeLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Float> fontSizeLiveData = new MutableLiveData<>(15f);

    /**
     * Default constructor for SettingsViewModel.
     */
    public SettingsViewModel() {
        this.repository = AppRepository.getInstance();
    }

    public LiveData<UserProfile> getUserProfileLiveData() {
        return repository.getUserProfileLiveData();
    }

    public LiveData<String> getLanguageLiveData() {
        return languageLiveData;
    }

    public LiveData<Boolean> getDarkModeLiveData() {
        return darkModeLiveData;
    }

    public LiveData<Float> getFontSizeLiveData() {
        return fontSizeLiveData;
    }

    public void setLanguage(String language) {
        languageLiveData.setValue(language);
    }

    public void setDarkMode(boolean isDark) {
        darkModeLiveData.setValue(isDark);
    }

    public void setFontSize(float spSize) {
        fontSizeLiveData.setValue(spSize);
    }
}