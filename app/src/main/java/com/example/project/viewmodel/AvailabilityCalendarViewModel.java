package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.R;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.repository.AppRepository;

import java.util.List;

/**
 * ViewModel for availability calendar management.
 */
public class AvailabilityCalendarViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<String> selectedDateLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Reservation>> dateReservationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> blockSlotSuccess = new MutableLiveData<>();

    /**
     * Default constructor for AvailabilityCalendarViewModel.
     */
    public AvailabilityCalendarViewModel() {
        this.repository = AppRepository.getInstance();
        setSelectedDate(repository.getTodayDateString());
    }

    public LiveData<List<ParkingPlace>> getPlacesLiveData() {
        return repository.getPlacesLiveData();
    }

    public LiveData<String> getSelectedDateLiveData() {
        return selectedDateLiveData;
    }

    public LiveData<List<Reservation>> getDateReservationsLiveData() {
        return dateReservationsLiveData;
    }

    public LiveData<Boolean> getBlockSlotSuccess() {
        return blockSlotSuccess;
    }

    /**
     * Sets the selected calendar date and loads its reservations.
     *
     * @param dateStr Date formatted string
     */
    public void setSelectedDate(String dateStr) {
        if (dateStr == null) {
            return;
        }
        selectedDateLiveData.setValue(dateStr);
        List<Reservation> reservations = repository.getReservationsForDate(dateStr);
        dateReservationsLiveData.setValue(reservations);
    }

    /**
     * Blocks a time slot for a selected parking spot.
     *
     * @param placeId   ID of spot
     * @param placeName Name of spot
     * @param timeFrom  Start time
     * @param timeTo    End time
     */
    public void blockSelectedSlot(String placeId, String placeName, String timeFrom, String timeTo) {
        String currentDateStr = selectedDateLiveData.getValue();
        String targetDateStr = (currentDateStr != null) ? currentDateStr : repository.getTodayDateString();

        String range = (timeFrom != null && !timeFrom.trim().isEmpty() ? timeFrom.trim() : "12:00")
                + " - "
                + (timeTo != null && !timeTo.trim().isEmpty() ? timeTo.trim() : "18:00");
        String pName = (placeName != null && !placeName.isEmpty()) ? placeName : "Miejsce Parkingowe";

        Reservation blockReservation = new Reservation(
                "block_" + System.currentTimeMillis(),
                placeId,
                pName,
                "ul. Nowogrodzka 10",
                "Warszawa",
                "1",
                targetDateStr,
                range,
                System.currentTimeMillis(),
                System.currentTimeMillis() + 14400000L,
                "Zablokowane przez właściciela",
                "ZABLOKOWANA",
                0.0,
                "4829",
                R.drawable.default_parking_spot
        );

        repository.addReservation(blockReservation);
        setSelectedDate(targetDateStr);
        blockSlotSuccess.setValue(true);
    }
}