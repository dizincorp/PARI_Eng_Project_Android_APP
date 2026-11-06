package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.project.R;
import com.example.project.database.AppDatabase;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.network.Resource;
import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.network.model.CreateBlockRequest;
import com.example.project.repository.AppRepository;
import com.example.project.repository.AvailabilityRepository;
import com.example.project.repository.ReservationRepository;
import com.example.project.repository.SpotRepository;
import com.example.project.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * ViewModel managing parking spot availability, calendar month overview, and slot blocking.
 * Restricts places strictly to the currently logged-in owner and supports both aggregate
 * ("Wszystkie miejsca") and per-spot views.
 */
public class AvailabilityCalendarViewModel extends AndroidViewModel {

    public static final String ALL_SPOTS_ID = "ALL";

    private final AppRepository appRepository;
    private final SpotRepository spotRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ReservationRepository reservationRepository;

    private final MutableLiveData<String> selectedDateLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> selectedSpotIdLiveData = new MutableLiveData<>(ALL_SPOTS_ID);
    private final MutableLiveData<Boolean> blockSlotSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> unblockSlotSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    private final LiveData<List<AvailabilityRuleEntity>> rulesLiveData;
    private final LiveData<List<BlockedPeriodEntity>> blockedPeriodsLiveData;
    private final LiveData<List<ReservationEntity>> reservationsLiveData;

    public AvailabilityCalendarViewModel(@NonNull Application application) {
        super(application);
        this.appRepository = AppRepository.getInstance();
        this.spotRepository = SpotRepository.getInstance(application);
        this.availabilityRepository = AvailabilityRepository.getInstance(application);
        this.reservationRepository = ReservationRepository.getInstance(application);

        setSelectedDate(appRepository.getTodayDateString());

        // Observe availability rules depending on whether ALL or specific spot is selected
        this.rulesLiveData = Transformations.switchMap(selectedSpotIdLiveData, spotId -> {
            if (spotId == null || ALL_SPOTS_ID.equals(spotId)) {
                return availabilityRepository.getAllRulesLiveData();
            }
            availabilityRepository.fetchAvailabilityRules(spotId.trim());
            return availabilityRepository.getRulesLiveData(spotId.trim());
        });

        // Observe blocked periods depending on whether ALL or specific spot is selected
        this.blockedPeriodsLiveData = Transformations.switchMap(selectedSpotIdLiveData, spotId -> {
            if (spotId == null || ALL_SPOTS_ID.equals(spotId)) {
                return availabilityRepository.getAllBlockedPeriodsLiveData();
            }
            availabilityRepository.fetchBlockedPeriods(spotId.trim());
            return availabilityRepository.getBlockedPeriodsLiveData(spotId.trim());
        });

        // Observe all reservations from Room
        this.reservationsLiveData = AppDatabase.getInstance(application).reservationDao().getAllReservationsLiveData();

        // Refresh owner's spots from backend into Room
        spotRepository.fetchMySpots();
        reservationRepository.fetchIncomingReservations(getCurrentUserEmail());

        // Cleanup any legacy fake block reservations from the reservations table
        AppDatabase.getInstance(application).getQueryExecutor().execute(() -> {
            AppDatabase.getInstance(application).reservationDao().deleteBlockReservations();
        });
    }

    private String getCurrentUserEmail() {
        String currentEmail = appRepository.getUserEmailLiveData().getValue();
        if (currentEmail == null || currentEmail.isEmpty()) {
            if (SessionManager.getInstance() != null) {
                currentEmail = SessionManager.getInstance().getUserEmail();
            }
        }
        return currentEmail != null ? currentEmail : "";
    }

    /**
     * Strictly retrieves the logged-in user's owned spots.
     */
    public LiveData<List<ParkingPlace>> getPlacesLiveData() {
        return appRepository.getMyPlacesLiveData();
    }

    public LiveData<String> getSelectedDateLiveData() {
        return selectedDateLiveData;
    }

    public LiveData<String> getSelectedSpotIdLiveData() {
        return selectedSpotIdLiveData;
    }

    public String getSelectedSpotId() {
        return selectedSpotIdLiveData.getValue() != null ? selectedSpotIdLiveData.getValue() : ALL_SPOTS_ID;
    }

    public LiveData<List<AvailabilityRuleEntity>> getRulesLiveData() {
        return rulesLiveData;
    }

    public LiveData<List<BlockedPeriodEntity>> getBlockedPeriodsLiveData() {
        return blockedPeriodsLiveData;
    }

    public LiveData<List<ReservationEntity>> getReservationsLiveData() {
        return reservationsLiveData;
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

    public void setSelectedSpotId(@Nullable String spotId) {
        String target = (spotId != null && !spotId.trim().isEmpty()) ? spotId.trim() : ALL_SPOTS_ID;
        if (!target.equals(selectedSpotIdLiveData.getValue())) {
            selectedSpotIdLiveData.setValue(target);
        }
    }

    public void setSelectedDate(@Nullable String dateStr) {
        if (dateStr != null && !dateStr.equals(selectedDateLiveData.getValue())) {
            selectedDateLiveData.setValue(dateStr);
        }
    }

    /**
     * Refreshes blocked periods and incoming reservations for the owner's spots.
     */
    public void refreshBlockedPeriods() {
        spotRepository.fetchMySpots();
        reservationRepository.fetchIncomingReservations(getCurrentUserEmail());

        String currentId = selectedSpotIdLiveData.getValue();
        if (currentId != null && !ALL_SPOTS_ID.equals(currentId) && !currentId.trim().isEmpty()) {
            availabilityRepository.fetchBlockedPeriods(currentId.trim());
        } else {
            List<ParkingPlace> mySpots = appRepository.getMyPlacesLiveData().getValue();
            if (mySpots != null) {
                for (ParkingPlace p : mySpots) {
                    if (p != null && p.getId() != null) {
                        availabilityRepository.fetchBlockedPeriods(p.getId().trim());
                        availabilityRepository.fetchAvailabilityRules(p.getId().trim());
                    }
                }
            }
        }
    }

    /**
     * Blocks a time slot for a specified spot on the target date.
     */
    public void blockSelectedSlot(@Nullable String placeId,
                                  @Nullable String placeName,
                                  @Nullable String timeFrom,
                                  @Nullable String timeTo,
                                  @Nullable String reason,
                                  @Nullable String targetDate) {
        if (placeId == null || placeId.trim().isEmpty() || ALL_SPOTS_ID.equals(placeId)) {
            errorMessageLiveData.setValue("Wybierz konkretne miejsce parkingowe do zablokowania");
            return;
        }

        String targetDateStr = (targetDate != null && !targetDate.trim().isEmpty())
                ? targetDate.trim()
                : (selectedDateLiveData.getValue() != null ? selectedDateLiveData.getValue() : appRepository.getTodayDateString());

        String from = (timeFrom != null && !timeFrom.trim().isEmpty()) ? timeFrom.trim() : "12:00";
        String to = (timeTo != null && !timeTo.trim().isEmpty()) ? timeTo.trim() : "18:00";
        String blockReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Blokada właściciela";

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

        availabilityRepository.createCalendarBlock(placeId.trim(), request).observeForever(resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                blockSlotSuccess.postValue(true);
                refreshBlockedPeriods();
            } else if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.postValue(resource.message != null ? resource.message : "Błąd blokowania terminu");
            }
        });

        // Offline / immediate UI representation via BlockedPeriodEntity (NOT ReservationEntity)
        String tempBlockId = "block_" + System.currentTimeMillis();
        BlockedPeriodEntity localBlock = new BlockedPeriodEntity(
                tempBlockId,
                placeId.trim(),
                startIso,
                endIso,
                (blockReason != null && !blockReason.isEmpty()) ? blockReason : "Termin zablokowany"
        );
        availabilityRepository.insertBlockedPeriodLocally(localBlock);
    }

    /**
     * Unblocks (deletes) a calendar blocked period for a spot.
     */
    public void unblockPeriod(@Nullable String placeId, @Nullable String periodId) {
        if (placeId == null || periodId == null || placeId.trim().isEmpty() || periodId.trim().isEmpty()) {
            return;
        }

        availabilityRepository.deleteCalendarBlock(placeId.trim(), periodId.trim()).observeForever(resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                unblockSlotSuccess.postValue(true);
                refreshBlockedPeriods();
            } else if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.postValue(resource.message != null ? resource.message : "Błąd odblokowania terminu");
            }
        });
    }

    public void saveWeeklyRules(@NonNull String spotId, @NonNull List<AvailabilityRuleRequest> rules) {
        availabilityRepository.replaceAvailabilityRules(spotId, rules).observeForever(resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.postValue(resource.message != null ? resource.message : "Błąd zapisu harmonogramu");
            }
        });
    }
}
