package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.R;
import com.example.project.databinding.ActivityReservePlaceBinding;
import com.example.project.databinding.DialogAddVehicleBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.model.Vehicle;
import com.example.project.repository.AppRepository;
import com.example.project.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for booking and paying for a parking spot reservation.
 */
public class ReservePlaceActivity extends BaseActivity {

    public static final String EXTRA_PLACE_ID = "EXTRA_PLACE_ID";

    private ActivityReservePlaceBinding binding;
    private ParkingPlace currentPlace;
    private final List<Vehicle> currentVehiclesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityReservePlaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_search);

        String placeId = getIntent() != null ? getIntent().getStringExtra(EXTRA_PLACE_ID) : null;
        currentPlace = AppRepository.getInstance().getPlaceById(placeId);

        if (currentPlace == null) {
            List<ParkingPlace> places = AppRepository.getInstance().getPlacesLiveData().getValue();
            if (places != null && !places.isEmpty()) {
                currentPlace = places.get(0);
            }
        }

        // Prevent owner from reserving their own place
        String currentEmail = AppRepository.getInstance().getUserEmailLiveData().getValue();
        if (currentPlace != null && currentEmail != null && currentEmail.equalsIgnoreCase(currentPlace.getOwnerEmail())) {
            Toast.makeText(this, R.string.error_owner_cannot_reserve, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupUI();
        setupClickListeners();
        observeVehicles();
    }

    private void setupUI() {
        if (binding == null || currentPlace == null) return;

        binding.imgPlacePhoto.setImageResource(currentPlace.getPhotoResId());
        binding.textPlaceStreet.setText(currentPlace.getStreet() != null ? currentPlace.getStreet() : currentPlace.getTitle());
        binding.textPlaceCity.setText(currentPlace.getCity() != null ? currentPlace.getCity() : "Warszawa");

        double rate = currentPlace.getPrice();
        double hours = 4.0;
        double subtotal = rate * hours;
        double serviceFee = 2.50;
        double total = subtotal + serviceFee;

        binding.textSummaryDuration.setText(R.string.sample_4h);
        binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka (%.2f PLN/h)", rate));
        binding.textSummaryRateValue.setText(String.format(Locale.getDefault(), "%.2f PLN", subtotal));
        binding.textSummaryTotalSum.setText(String.format(Locale.getDefault(), "%.2f PLN", total));

        // Fixed schedule time reflecting access time/range
        String startVal = "Dzisiaj, " + (currentPlace.getAccessTime().contains("-") ? currentPlace.getAccessTime().split("-")[0].trim() : "14:00");
        String endVal = "Dzisiaj, " + (currentPlace.getAccessTime().contains("-") ? currentPlace.getAccessTime().split("-")[1].trim() : "18:00");
        binding.textStartTimeValue.setText(startVal);
        binding.textEndTimeValue.setText(endVal);
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnAddVehicle.setOnClickListener(v -> showAddVehicleDialog());

        binding.btnReserveAndPay.setOnClickListener(v -> {
            if (currentPlace == null) return;

            String selectedVehicleText = "Tesla Model 3 - WX 12345";
            int selectedPos = binding.spinnerVehicles.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < currentVehiclesList.size()) {
                Vehicle selectedV = currentVehiclesList.get(selectedPos);
                if (selectedV != null) {
                    selectedVehicleText = selectedV.getDisplayText();
                }
            }

            Reservation reservation = new Reservation(
                    "r_" + System.currentTimeMillis(),
                    currentPlace.getId(),
                    currentPlace.getTitle(),
                    currentPlace.getStreet() != null ? currentPlace.getStreet() : "",
                    currentPlace.getCity() != null ? currentPlace.getCity() : "Warszawa",
                    currentPlace.getPlaceNumber() != null ? currentPlace.getPlaceNumber() : "1",
                    "Dziś, 14 Października",
                    binding.textStartTimeValue.getText().toString() + " - " + binding.textEndTimeValue.getText().toString(),
                    System.currentTimeMillis(),
                    System.currentTimeMillis() + 14400000L,
                    selectedVehicleText,
                    "AKTYWNA",
                    currentPlace.getPrice() * 4 + 2.50,
                    currentPlace.getPinCode(),
                    currentPlace.getPhotoResId()
            );

            AppRepository.getInstance().createReservation(reservation);
            NotificationHelper.showNotification(this, "ParkShare", getString(R.string.toast_reservation_success));

            Toast.makeText(this, R.string.toast_reservation_success, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ReservePlaceActivity.this, ReservationsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void observeVehicles() {
        AppRepository.getInstance().getVehiclesLiveData().observe(this, vehicles -> {
            if (binding == null || vehicles == null) return;

            currentVehiclesList.clear();
            currentVehiclesList.addAll(vehicles);

            List<String> vehicleStrings = new ArrayList<>();
            for (Vehicle v : vehicles) {
                vehicleStrings.add(v.getDisplayText());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    vehicleStrings
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerVehicles.setAdapter(adapter);
        });
    }

    private void showAddVehicleDialog() {
        DialogAddVehicleBinding dialogBinding = DialogAddVehicleBinding.inflate(LayoutInflater.from(this));

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save_vehicle, (dialog, which) -> {
                    String make = dialogBinding.editVehicleMake.getText() != null ? dialogBinding.editVehicleMake.getText().toString() : "";
                    String plate = dialogBinding.editVehiclePlate.getText() != null ? dialogBinding.editVehiclePlate.getText().toString() : "";
                    String color = dialogBinding.editVehicleColor.getText() != null ? dialogBinding.editVehicleColor.getText().toString() : "";

                    if (!make.trim().isEmpty() && !plate.trim().isEmpty()) {
                        Vehicle newVehicle = new Vehicle("v_" + System.currentTimeMillis(), make.trim(), plate.trim(), color.trim());
                        AppRepository.getInstance().addVehicle(newVehicle);
                        Toast.makeText(this, "Dodano pojazd!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}