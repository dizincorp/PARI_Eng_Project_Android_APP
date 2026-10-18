package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.model.ParkingPlace;
import com.example.project.model.Transaction;
import com.example.project.repository.AppRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel for the Places dashboard screen.
 */
public class PlacesViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<String> greetingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> subtitleLiveData = new MutableLiveData<>();

    /**
     * Default constructor for PlacesViewModel.
     */
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
        return repository.getMyPlacesLiveData();
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return repository.getTransactionsLiveData();
    }

    public void deletePlace(String placeId) {
        repository.deletePlace(placeId);
    }
}