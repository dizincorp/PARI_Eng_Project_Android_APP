package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlacesViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<String> greetingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> subtitleLiveData = new MutableLiveData<>();

    public PlacesViewModel() {
        this.repository = AppRepository.getInstance();
        initData();
    }

    private void initData() {
        String userName = repository.getUserNameLiveData().getValue();
        String nameToDisplay = (userName != null && !userName.isEmpty()) ? userName : "Jan";
        greetingLiveData.setValue("Cześć, " + nameToDisplay);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("pl", "PL"));
        String monthYear = sdf.format(calendar.getTime());
        if (!monthYear.isEmpty()) {
            monthYear = monthYear.substring(0, 1).toUpperCase(new Locale("pl", "PL")) + monthYear.substring(1);
        }
        subtitleLiveData.setValue("Dashboard • " + monthYear);
    }

    public LiveData<String> getGreetingLiveData() {
        return greetingLiveData;
    }

    public LiveData<String> getSubtitleLiveData() {
        return subtitleLiveData;
    }

    public LiveData<List<ParkingPlace>> getMyPlacesLiveData() {
        return repository.getPlacesLiveData();
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return repository.getTransactionsLiveData();
    }
}