package com.example.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AppRepository {

    private static AppRepository instance;

    private final MutableLiveData<String> userNameLiveData = new MutableLiveData<>("Jan");
    private final MutableLiveData<List<ParkingPlace>> placesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Vehicle>> vehiclesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Reservation> activeReservationLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<List<Reservation>> historyReservationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>(new ArrayList<>());

    private final List<ParkingPlace> places = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Reservation> historyReservations = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    private AppRepository() {
        initMockData();
    }

    public static synchronized AppRepository getInstance() {
        if (instance == null) {
            instance = new AppRepository();
        }
        return instance;
    }

    private void initMockData() {
        // Initial Places (Free initially)
        ParkingPlace place1 = new ParkingPlace(
                "p1",
                "Prywatny Podjazd - Centrum",
                "ul. Nowogrodzka 10",
                "Warszawa",
                "00-001",
                "Sektor B • 42",
                "-1",
                12.00,
                "08:00 - 22:00",
                Arrays.asList("Ładowanie EV", "Zaufany", "Monitoring"),
                52.2297,
                21.0122
        );

        ParkingPlace place2 = new ParkingPlace(
                "p2",
                "Centrum Park A1",
                "ul. Marszałkowska 126",
                "Warszawa",
                "00-008",
                "Sektor A • 12",
                "0",
                10.00,
                "24/7",
                Arrays.asList("Monitoring", "Podziemny", "Oświetlenie"),
                52.2350,
                21.0080
        );

        places.add(place1);
        places.add(place2);
        placesLiveData.setValue(new ArrayList<>(places));

        // Initial Vehicles
        Vehicle v1 = new Vehicle("v1", "Tesla Model 3", "WX 12345", "Czarny");
        Vehicle v2 = new Vehicle("v2", "Toyota Yaris", "WA 12345", "Biały");
        vehicles.add(v1);
        vehicles.add(v2);
        vehiclesLiveData.setValue(new ArrayList<>(vehicles));

        // Initial History Reservation
        Reservation pastR = new Reservation(
                "h1",
                "p2",
                "Centrum Park A1",
                "ul. Marszałkowska 126",
                "Warszawa",
                "Sektor A • 12",
                "10 Października 2024",
                "10:00 - 14:00",
                System.currentTimeMillis() - 86400000L * 5,
                System.currentTimeMillis() - 86400000L * 5 + 14400000L,
                "Tesla Model 3 - WX 12345",
                "ZAKOŃCZONA",
                42.50,
                "4829",
                R.drawable.ic_launcher_background
        );
        historyReservations.add(pastR);
        historyReservationsLiveData.setValue(new ArrayList<>(historyReservations));

        // Initial Transactions
        Transaction t1 = new Transaction("t1", "Centrum Park A1", "Tesla Model 3 (WX 12345)", "4 godz. (10:00 - 14:00)", "2024-10-10", 42.50);
        transactions.add(t1);
        transactionsLiveData.setValue(new ArrayList<>(transactions));
    }

    public LiveData<String> getUserNameLiveData() {
        return userNameLiveData;
    }

    public LiveData<List<ParkingPlace>> getPlacesLiveData() {
        return placesLiveData;
    }

    public LiveData<List<Vehicle>> getVehiclesLiveData() {
        return vehiclesLiveData;
    }

    public LiveData<Reservation> getActiveReservationLiveData() {
        return activeReservationLiveData;
    }

    public LiveData<List<Reservation>> getHistoryReservationsLiveData() {
        return historyReservationsLiveData;
    }

    public LiveData<List<Transaction>> getTransactionsLiveData() {
        return transactionsLiveData;
    }

    public ParkingPlace getPlaceById(String id) {
        if (id == null) return null;
        for (ParkingPlace p : places) {
            if (id.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    public List<Reservation> getReservationsForDate(String dateStr) {
        List<Reservation> list = new ArrayList<>();
        Reservation active = activeReservationLiveData.getValue();
        if (active != null) {
            list.add(active);
        }
        return list;
    }

    public void addReservation(Reservation reservation) {
        if (reservation == null) return;
        historyReservations.add(0, reservation);
        historyReservationsLiveData.setValue(new ArrayList<>(historyReservations));
    }

    public void addPlace(ParkingPlace place) {
        if (place == null) return;

        // Ensure initial status is free
        place.setOccupied(false);
        place.setOccupiedUntil("");

        int existingIndex = -1;
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(place.getId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            places.set(existingIndex, place);
        } else {
            places.add(0, place);
        }
        placesLiveData.setValue(new ArrayList<>(places));
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) return;
        if (vehicle.getId() == null || vehicle.getId().isEmpty()) {
            vehicle.setId("v_" + System.currentTimeMillis());
        }
        vehicles.add(0, vehicle);
        vehiclesLiveData.setValue(new ArrayList<>(vehicles));
    }

    public void createReservation(Reservation reservation) {
        if (reservation == null) return;

        activeReservationLiveData.setValue(reservation);

        // Mark corresponding parking place as occupied
        ParkingPlace place = getPlaceById(reservation.getPlaceId());
        if (place != null) {
            place.setOccupied(true);
            place.setOccupiedUntil(reservation.getTimeRange());
            placesLiveData.setValue(new ArrayList<>(places));
        }

        // Add to transactions log
        Transaction tx = new Transaction(
                "t_" + System.currentTimeMillis(),
                reservation.getPlaceName(),
                reservation.getVehicleInfo(),
                reservation.getTimeRange(),
                getTodayDateString(),
                reservation.getTotalPrice()
        );
        transactions.add(0, tx);
        transactionsLiveData.setValue(new ArrayList<>(transactions));
    }

    public void cancelActiveReservation() {
        Reservation active = activeReservationLiveData.getValue();
        if (active == null) return;

        active.setStatus("ANULOWANA");
        historyReservations.add(0, active);
        historyReservationsLiveData.setValue(new ArrayList<>(historyReservations));

        // Mark place as free again
        ParkingPlace place = getPlaceById(active.getPlaceId());
        if (place != null) {
            place.setOccupied(false);
            place.setOccupiedUntil("");
            placesLiveData.setValue(new ArrayList<>(places));
        }

        activeReservationLiveData.setValue(null);
    }

    public String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }
}