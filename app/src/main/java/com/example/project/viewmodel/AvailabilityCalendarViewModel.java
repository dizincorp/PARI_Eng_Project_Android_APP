package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.project.R;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.network.Resource;
import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.network.model.CreateBlockRequest;
import com.example.project.repository.AppRepository;
import com.example.project.repository.AvailabilityRepository;
import com.example.project.repository.SpotRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * ViewModel managing parking spot availability, weekly schedules, and calendar block periods.
 * Follows the Single Source of Truth pattern with Room database and Retrofit API.
 */
public class AvailabilityCalendarViewModel extends AndroidViewModel {

    private final AppRepository appRepository;
    private final SpotRepository spotRepository;
    private final AvailabilityRepository availabilityRepository;

    private final MutableLiveData<String> selectedDateLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> selectedSpotIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Reservation>> dateReservationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> blockSlotSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> unblockSlotSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    private final LiveData<List<AvailabilityRuleEntity>> rulesLiveData;
    private final LiveData<List<BlockedPeriodEntity>> blockedPeriodsLiveData;

    /**
     * Parameterized constructor providing Application context for repository initializations.
     *
     * @param application Application context
     */
    public AvailabilityCalendarViewModel(@NonNull Application application) {
        super(application);
        this.appRepository = AppRepository.getInstance();
        this.spotRepository = SpotRepository.getInstance(application);
        this.availabilityRepository = AvailabilityRepository.getInstance(application);

        setSelectedDate(appRepository.getTodayDateString());

        // Reactively observe Room availability rules for the selected spot
        this.rulesLiveData = Transformations.switchMap(selectedSpotIdLiveData, spotId -> {
            if (spotId == null || spotId.trim().isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            availabilityRepository.fetchAvailabilityRules(spotId.trim());
            return availabilityRepository.getRulesLiveData(spotId.trim());
        });

        // Reactively observe Room blocked periods for the selected spot
        this.blockedPeriodsLiveData = Transformations.switchMap(selectedSpotIdLiveData, spotId -> {
            if (spotId == null || spotId.trim().isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            availabilityRepository.fetchBlockedPeriods(spotId.trim());
            return availabilityRepository.getBlockedPeriodsLiveData(spotId.trim());
        });

        // Refresh owner's spots in the background
        spotRepository.fetchMySpots();
    }

    public LiveData<List<ParkingPlace>> getPlacesLiveData() {
        return appRepository.getPlacesLiveData();
    }

    public LiveData<String> getSelectedDateLiveData() {
        return selectedDateLiveData;
    }

    public LiveData<String> getSelectedSpotIdLiveData() {
        return selectedSpotIdLiveData;
    }

    public String getSelectedSpotId() {
        return selectedSpotIdLiveData.getValue();
    }

    /**
     * Refreshes blocked periods for the currently selected spot.
     */
    public void refreshBlockedPeriods() {
        String currentId = selectedSpotIdLiveData.getValue();
        if (currentId != null && !currentId.trim().isEmpty()) {
            availabilityRepository.fetchBlockedPeriods(currentId.trim());
        }
    }

    public LiveData<List<Reservation>> getDateReservationsLiveData() {
        return dateReservationsLiveData;
    }

    public LiveData<Boolean> getBlockSlotSuccess() {
        return blockSlotSuccess;
    }

    public LiveData<Boolean> getUnblockSlotSuccess() {
        return unblockSlotSuccess;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<List<AvailabilityRuleEntity>> getRulesLiveData() {
        return rulesLiveData;
    }

    public LiveData<List<BlockedPeriodEntity>> getBlockedPeriodsLiveData() {
        return blockedPeriodsLiveData;
    }

    /**
     * Updates the currently selected spot identifier.
     *
     * @param spotId Spot identifier
     */
    public void setSelectedSpotId(@Nullable String spotId) {
        if (spotId != null && !spotId.equals(selectedSpotIdLiveData.getValue())) {
            selectedSpotIdLiveData.setValue(spotId);
        }
    }

    /**
     * Sets the selected calendar date and loads its reservations.
     *
     * @param dateStr Date formatted string (YYYY-MM-DD)
     */
    public void setSelectedDate(@Nullable String dateStr) {
        if (dateStr == null) {
            return;
        }
        selectedDateLiveData.setValue(dateStr);
        List<Reservation> reservations = appRepository.getReservationsForDate(dateStr);
        dateReservationsLiveData.setValue(reservations);
    }

    /**
     * Blocks a time slot for the selected parking spot.
     *
     * @param placeId   ID of spot
     * @param placeName Name of spot
     * @param timeFrom  Start time (HH:mm)
     * @param timeTo    End time (HH:mm)
     * @param reason    Reason description for blocking
     */
    public void blockSelectedSlot(@Nullable String placeId,
                                  @Nullable String placeName,
                                  @Nullable String timeFrom,
                                  @Nullable String timeTo,
                                  @Nullable String reason) {
        if (placeId == null || placeId.trim().isEmpty()) {
            errorMessageLiveData.setValue("Brak wybranego miejsca parkingowego");
            return;
        }

        String currentDateStr = selectedDateLiveData.getValue();
        String targetDateStr = (currentDateStr != null) ? currentDateStr : appRepository.getTodayDateString();

        String from = (timeFrom != null && !timeFrom.trim().isEmpty()) ? timeFrom.trim() : "12:00";
        String to = (timeTo != null && !timeTo.trim().isEmpty()) ? timeTo.trim() : "18:00";
        String blockReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Blokada właściciela";

        // Convert user local date & time to standard ISO-8601 UTC Instant (e.g. 2026-11-01T15:00:00.000Z)
        SimpleDateFormat localParserSec = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat localParserMin = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        SimpleDateFormat isoUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoUtc.setTimeZone(TimeZone.getTimeZone("UTC"));

        String startIso;
        String endIso;
        long blockStartMs = 0;
        long blockEndMs = 0;
        try {
            Date startDate = null;
            Date endDate = null;
            try {
                startDate = localParserSec.parse(targetDateStr + " " + from);
            } catch (Exception ignored) {
                startDate = localParserMin.parse(targetDateStr + " " + from);
            }
            try {
                endDate = localParserSec.parse(targetDateStr + " " + to);
            } catch (Exception ignored) {
                endDate = localParserMin.parse(targetDateStr + " " + to);
            }

            if (startDate != null && endDate != null) {
                startIso = isoUtc.format(startDate);
                endIso = isoUtc.format(endDate);
                blockStartMs = startDate.getTime();
                blockEndMs = endDate.getTime();
            } else {
                startIso = targetDateStr + "T" + (from.length() == 5 ? from + ":00.000Z" : from);
                endIso = targetDateStr + "T" + (to.length() == 5 ? to + ":00.000Z" : to);
            }
        } catch (Exception e) {
            startIso = targetDateStr + "T" + (from.length() == 5 ? from + ":00.000Z" : from);
            endIso = targetDateStr + "T" + (to.length() == 5 ? to + ":00.000Z" : to);
        }

        CreateBlockRequest request = new CreateBlockRequest(startIso, endIso, blockReason);

        availabilityRepository.createCalendarBlock(placeId, request).observeForever(resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                blockSlotSuccess.postValue(true);
            } else if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.postValue(resource.message != null ? resource.message : "Błąd blokowania terminu");
            }
        });

        // Add local reservation representation for offline/immediate UI display
        String range = from + " - " + to;
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
                blockStartMs > 0 ? blockStartMs : System.currentTimeMillis(),
                blockEndMs > 0 ? blockEndMs : (System.currentTimeMillis() + 14400000L),
                blockReason,
                "ZABLOKOWANA",
                0.0,
                "4829",
                R.drawable.default_parking_spot
        );

        appRepository.addReservation(blockReservation);
        setSelectedDate(targetDateStr);
    }

    /**
     * Unblocks (deletes) a calendar blocked period for a spot.
     *
     * @param placeId  ID of spot
     * @param periodId ID of blocked period
     */
    public void unblockPeriod(@Nullable String placeId, @Nullable String periodId) {
        if (placeId == null || periodId == null || placeId.trim().isEmpty() || periodId.trim().isEmpty()) {
            return;
        }

        availabilityRepository.deleteCalendarBlock(placeId, periodId).observeForever(resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                unblockSlotSuccess.postValue(true);
            } else if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.postValue(resource.message != null ? resource.message : "Błąd odblokowania terminu");
            }
        });
    }

    /**
     * Replaces weekly availability rules for a spot.
     *
     * @param spotId Unique spot identifier
     * @param rules  List of availability rules
     */
    public void saveWeeklyRules(@NonNull String spotId, @NonNull List<AvailabilityRuleRequest> rules) {
        availabilityRepository.replaceAvailabilityRules(spotId, rules).observeForever(resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.postValue(resource.message != null ? resource.message : "Błąd zapisu harmonogramu");
            }
        });
    }
}
