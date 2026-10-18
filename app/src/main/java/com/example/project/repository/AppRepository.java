package com.example.project.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.project.database.AppDatabase;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.database.entity.TransactionEntity;
import com.example.project.database.entity.UserEntity;
import com.example.project.database.entity.VehicleEntity;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.model.Transaction;
import com.example.project.model.UserProfile;
import com.example.project.model.Vehicle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class acting as Single Source of Truth for the application's data layer,
 * backed by Room Database with reactive multi-user isolation via Transformations.switchMap.
 */
public class AppRepository {

    private static AppRepository instance;

    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> userNameLiveData = new MutableLiveData<>("Jan Kowalski");
    private final MutableLiveData<String> userEmailLiveData = new MutableLiveData<>("jan_kowalski@gmail.com");
    private final MutableLiveData<UserProfile> userProfileLiveData = new MutableLiveData<>(new UserProfile());

    private AppRepository() {
    }

    /**
     * Initializes database context for singleton repository.
     *
     * @param context Application context
     */
    public void init(Context context) {
        if (database == null && context != null) {
            database = AppDatabase.getInstance(context);
        }
    }

    /**
     * Gets the singleton instance of AppRepository.
     *
     * @return Synchronized AppRepository instance
     */
    public static synchronized AppRepository getInstance() {
        if (instance == null) {
            instance = new AppRepository();
        }
        return instance;
    }

    public LiveData<String> getUserNameLiveData() {
        return userNameLiveData;
    }

    public LiveData<String> getUserEmailLiveData() {
        return userEmailLiveData;
    }

    public LiveData<UserProfile> getUserProfileLiveData() {
        return userProfileLiveData;
    }

    public void loginUser(String email, String name) {
        String finalEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "jan_kowalski@gmail.com";
        String finalName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Jan Kowalski";

        userEmailLiveData.setValue(finalEmail);
        userNameLiveData.setValue(finalName);

        UserProfile profile = new UserProfile();
        profile.setEmail(finalEmail);
        profile.setName(finalName);
        userProfileLiveData.setValue(profile);

        if (database != null) {
            executor.execute(() -> {
                UserEntity entity = database.userDao().getUserByEmail(finalEmail);
                if (entity == null) {
                    entity = new UserEntity();
                    entity.setEmail(finalEmail);
                    entity.setName(finalName);
                    entity.setPremium(true);
                    entity.setRating(4.9);
                    entity.setRatingCount(12);
                    database.userDao().insertOrUpdate(entity);
                }
            });
        }
    }

    public void updateUserName(String newName) {
        if (newName == null || newName.trim().isEmpty()) return;

        userNameLiveData.setValue(newName.trim());
        UserProfile profile = userProfileLiveData.getValue();
        if (profile != null) {
            profile.setName(newName.trim());
            userProfileLiveData.setValue(profile);
        }

        if (database != null) {
            String currentEmail = userEmailLiveData.getValue();
            executor.execute(() -> {
                UserEntity user = database.userDao().getUserByEmail(currentEmail);
                if (user != null) {
                    user.setName(newName.trim());
                    database.userDao().insertOrUpdate(user);
                }
            });
        }
    }

    public void rateUser(double ratingScore) {
        UserProfile profile = userProfileLiveData.getValue();
        if (profile != null) {
            double currentRating = profile.getRating();
            double newAvg = Math.round(((currentRating + ratingScore) / 2.0) * 10.0) / 10.0;
            profile.setRating(newAvg);
            userProfileLiveData.setValue(profile);
        }
    }

    // Common places on map (shared by ALL users)
    public LiveData<List<ParkingPlace>> getPlacesLiveData() {
        if (database == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        return Transformations.map(database.parkingPlaceDao().getAllPlacesLiveData(), entities -> {
            List<ParkingPlace> list = new ArrayList<>();
            if (entities != null) {
                for (ParkingPlaceEntity e : entities) {
                    list.add(toParkingPlace(e));
                }
            }
            return list;
        });
    }

    // Owner's places ("Moje Miejsca") dynamically updated when userEmailLiveData changes!
    public LiveData<List<ParkingPlace>> getMyPlacesLiveData() {
        if (database == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        return Transformations.switchMap(userEmailLiveData, email ->
                Transformations.map(database.parkingPlaceDao().getMyPlacesLiveData(email != null ? email : ""), entities -> {
                    List<ParkingPlace> list = new ArrayList<>();
                    if (entities != null) {
                        for (ParkingPlaceEntity e : entities) {
                            list.add(toParkingPlace(e));
                        }
                    }
                    return list;
                })
        );
    }

    // Driver's vehicles ("Moje Pojazdy")
    public LiveData<List<Vehicle>> getVehiclesLiveData() {
        if (database == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        return Transformations.switchMap(userEmailLiveData, email ->
                Transformations.map(database.vehicleDao().getUserVehiclesLiveData(email != null ? email : ""), entities -> {
                    List<Vehicle> list = new ArrayList<>();
                    if (entities != null) {
                        for (VehicleEntity e : entities) {
                            list.add(toVehicle(e));
                        }
                    }
                    return list;
                })
        );
    }

    // Driver's active reservation
    public LiveData<Reservation> getActiveReservationLiveData() {
        if (database == null) {
            return new MutableLiveData<>(null);
        }
        return Transformations.switchMap(userEmailLiveData, email ->
                Transformations.map(database.reservationDao().getUserActiveReservationLiveData(email != null ? email : ""), entity -> {
                    if (entity == null) return null;
                    return toReservation(entity);
                })
        );
    }

    // Driver's history reservations
    public LiveData<List<Reservation>> getHistoryReservationsLiveData() {
        if (database == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        return Transformations.switchMap(userEmailLiveData, email ->
                Transformations.map(database.reservationDao().getUserHistoryReservationsLiveData(email != null ? email : ""), entities -> {
                    List<Reservation> list = new ArrayList<>();
                    if (entities != null) {
                        for (ReservationEntity e : entities) {
                            list.add(toReservation(e));
                        }
                    }
                    return list;
                })
        );
    }

    // Owner's earnings & transactions
    public LiveData<List<Transaction>> getTransactionsLiveData() {
        if (database == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        return Transformations.switchMap(userEmailLiveData, email ->
                Transformations.map(database.transactionDao().getUserTransactionsLiveData(email != null ? email : ""), entities -> {
                    List<Transaction> list = new ArrayList<>();
                    if (entities != null) {
                        for (TransactionEntity e : entities) {
                            list.add(toTransaction(e));
                        }
                    }
                    return list;
                })
        );
    }

    public ParkingPlace getPlaceById(String id) {
        if (id == null || database == null) return null;
        try {
            return executor.submit(() -> toParkingPlace(database.parkingPlaceDao().getPlaceById(id))).get();
        } catch (Exception e) {
            return null;
        }
    }

    public List<Reservation> getReservationsForDate(String dateStr) {
        if (dateStr == null || database == null) return new ArrayList<>();
        try {
            return executor.submit(() -> {
                List<ReservationEntity> entities = database.reservationDao().getReservationsForDate(dateStr);
                List<Reservation> resList = new ArrayList<>();
                if (entities != null) {
                    for (ReservationEntity e : entities) {
                        resList.add(toReservation(e));
                    }
                }
                return resList;
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addReservation(Reservation reservation) {
        if (reservation == null || database == null) return;
        if (reservation.getUserEmail() == null || reservation.getUserEmail().isEmpty()) {
            reservation.setUserEmail(userEmailLiveData.getValue() != null ? userEmailLiveData.getValue() : "");
        }
        executor.execute(() -> database.reservationDao().insertOrUpdate(toReservationEntity(reservation)));
    }

    public void addPlace(ParkingPlace place) {
        if (place == null || database == null) return;
        if (place.getOwnerEmail() == null || place.getOwnerEmail().isEmpty()) {
            place.setOwnerEmail(userEmailLiveData.getValue() != null ? userEmailLiveData.getValue() : "");
        }
        place.setOccupied(false);
        place.setOccupiedUntil("");
        executor.execute(() -> database.parkingPlaceDao().insertOrUpdate(toParkingPlaceEntity(place)));
    }

    public void deletePlace(String placeId) {
        if (placeId == null || database == null) return;
        executor.execute(() -> database.parkingPlaceDao().deleteById(placeId));
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null || database == null) return;
        if (vehicle.getOwnerEmail() == null || vehicle.getOwnerEmail().isEmpty()) {
            vehicle.setOwnerEmail(userEmailLiveData.getValue() != null ? userEmailLiveData.getValue() : "");
        }
        executor.execute(() -> database.vehicleDao().insertOrUpdate(toVehicleEntity(vehicle)));
    }

    public void createReservation(Reservation reservation) {
        if (reservation == null || database == null) return;

        String currentEmail = userEmailLiveData.getValue() != null ? userEmailLiveData.getValue() : "";
        reservation.setUserEmail(currentEmail);

        executor.execute(() -> {
            database.reservationDao().insertOrUpdate(toReservationEntity(reservation));

            ParkingPlaceEntity place = database.parkingPlaceDao().getPlaceById(reservation.getPlaceId());
            if (place != null) {
                place.setOccupied(true);
                place.setOccupiedUntil(reservation.getTimeRange());
                database.parkingPlaceDao().insertOrUpdate(place);

                // Add transaction log credited to the OWNER of the spot
                TransactionEntity tx = new TransactionEntity();
                tx.setOwnerEmail(place.getOwnerEmail());
                tx.setPlaceName(reservation.getPlaceName());
                tx.setCarInfo(reservation.getVehicleInfo());
                tx.setDuration(reservation.getTimeRange());
                tx.setDate(getTodayDateString());
                tx.setTotalPrice(reservation.getTotalPrice());
                database.transactionDao().insertOrUpdate(tx);
            }
        });
    }

    public void cancelActiveReservation() {
        if (database == null) return;

        executor.execute(() -> {
            try {
                ReservationEntity active = database.reservationDao().getActiveReservationSync();
                if (active != null) {
                    active.setStatus("ANULOWANA");
                    database.reservationDao().insertOrUpdate(active);

                    ParkingPlaceEntity place = database.parkingPlaceDao().getPlaceById(active.getPlaceId());
                    if (place != null) {
                        place.setOccupied(false);
                        place.setOccupiedUntil("");
                        database.parkingPlaceDao().insertOrUpdate(place);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    public void cancelReservationById(String reservationId) {
        if (reservationId == null || database == null) return;

        executor.execute(() -> {
            try {
                ReservationEntity entity = database.reservationDao().getReservationByIdSync(reservationId);
                if (entity != null) {
                    entity.setStatus("ANULOWANA");
                    database.reservationDao().insertOrUpdate(entity);

                    ParkingPlaceEntity place = database.parkingPlaceDao().getPlaceById(entity.getPlaceId());
                    if (place != null) {
                        place.setOccupied(false);
                        place.setOccupiedUntil("");
                        database.parkingPlaceDao().insertOrUpdate(place);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    public String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    // Mapping Converters
    private ParkingPlace toParkingPlace(ParkingPlaceEntity e) {
        if (e == null) return null;
        List<String> amenities = new ArrayList<>();
        if (e.getAmenitiesCsv() != null && !e.getAmenitiesCsv().isEmpty()) {
            amenities = Arrays.asList(e.getAmenitiesCsv().split(","));
        }
        ParkingPlace p = new ParkingPlace(
                e.getId(), e.getTitle(), e.getStreet(), e.getCity(), e.getPostalCode(),
                e.getPlaceNumber(), e.getFloor(), e.getPrice(), e.getAccessTime(),
                amenities, e.getLatitude(), e.getLongitude()
        );
        p.setOwnerEmail(e.getOwnerEmail() != null ? e.getOwnerEmail() : "");
        p.setImageUri(e.getImageUri() != null ? e.getImageUri() : "");
        p.setRating(e.getRating());
        p.setRatingCount(e.getRatingCount());
        p.setOccupied(e.isOccupied());
        p.setOccupiedUntil(e.getOccupiedUntil());
        p.setPinCode(e.getPinCode());
        p.setPhotoResId(e.getPhotoResId());
        return p;
    }

    private ParkingPlaceEntity toParkingPlaceEntity(ParkingPlace p) {
        if (p == null) return null;
        ParkingPlaceEntity e = new ParkingPlaceEntity();
        e.setId(p.getId());
        e.setOwnerEmail(p.getOwnerEmail() != null ? p.getOwnerEmail() : "");
        e.setTitle(p.getTitle());
        e.setStreet(p.getStreet());
        e.setCity(p.getCity());
        e.setPostalCode(p.getPostalCode());
        e.setPlaceNumber(p.getPlaceNumber());
        e.setFloor(p.getFloor());
        e.setPrice(p.getPrice());
        e.setAccessTime(p.getAccessTime());
        if (p.getAmenities() != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < p.getAmenities().size(); i++) {
                sb.append(p.getAmenities().get(i));
                if (i < p.getAmenities().size() - 1) sb.append(",");
            }
            e.setAmenitiesCsv(sb.toString());
        }
        e.setLatitude(p.getLatitude());
        e.setLongitude(p.getLongitude());
        e.setImageUri(p.getImageUri() != null ? p.getImageUri() : "");
        e.setRating(p.getRating());
        e.setRatingCount(p.getRatingCount());
        e.setOccupied(p.isOccupied());
        e.setOccupiedUntil(p.getOccupiedUntil());
        e.setPinCode(p.getPinCode());
        e.setPhotoResId(p.getPhotoResId());
        return e;
    }

    private Vehicle toVehicle(VehicleEntity e) {
        if (e == null) return null;
        Vehicle v = new Vehicle(e.getId(), e.getMakeAndModel(), e.getLicensePlate(), e.getColor());
        v.setOwnerEmail(e.getOwnerEmail() != null ? e.getOwnerEmail() : "");
        return v;
    }

    private VehicleEntity toVehicleEntity(Vehicle v) {
        if (v == null) return null;
        VehicleEntity e = new VehicleEntity();
        e.setId(v.getId());
        e.setOwnerEmail(v.getOwnerEmail() != null ? v.getOwnerEmail() : "");
        e.setMakeAndModel(v.getMakeAndModel());
        e.setLicensePlate(v.getLicensePlate());
        e.setColor(v.getColor());
        return e;
    }

    private Reservation toReservation(ReservationEntity e) {
        if (e == null) return null;
        Reservation r = new Reservation(
                e.getId(), e.getPlaceId(), e.getPlaceName(), e.getStreet(), e.getCity(),
                e.getPlaceNumber(), e.getDateString(), e.getTimeRange(), e.getStartTimeMs(),
                e.getEndTimeMs(), e.getVehicleInfo(), e.getStatus(), e.getTotalPrice(),
                e.getPinCode(), e.getPhotoResId()
        );
        r.setUserEmail(e.getUserEmail() != null ? e.getUserEmail() : "");
        return r;
    }

    private ReservationEntity toReservationEntity(Reservation r) {
        if (r == null) return null;
        ReservationEntity e = new ReservationEntity();
        e.setId(r.getId());
        e.setUserEmail(r.getUserEmail() != null ? r.getUserEmail() : "");
        e.setPlaceId(r.getPlaceId());
        e.setPlaceName(r.getPlaceName());
        e.setStreet(r.getStreet());
        e.setCity(r.getCity());
        e.setPlaceNumber(r.getPlaceNumber());
        e.setDateString(r.getDateString());
        e.setTimeRange(r.getTimeRange());
        e.setStartTimeMs(r.getStartTimeMs());
        e.setEndTimeMs(r.getEndTimeMs());
        e.setUserName(r.getUserName());
        e.setCarModel(r.getCarModel());
        e.setCarPlate(r.getCarPlate());
        e.setVehicleInfo(r.getVehicleInfo());
        e.setStatus(r.getStatus());
        e.setTotalPrice(r.getTotalPrice());
        e.setPinCode(r.getPinCode());
        e.setPhotoResId(r.getPhotoResId());
        return e;
    }

    private Transaction toTransaction(TransactionEntity e) {
        if (e == null) return null;
        Transaction t = new Transaction(e.getId(), e.getPlaceName(), e.getCarInfo(), e.getDuration(), e.getDate(), e.getTotalPrice());
        t.setOwnerEmail(e.getOwnerEmail() != null ? e.getOwnerEmail() : "");
        return t;
    }
}