package com.example.project.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityReservePlaceBinding;
import com.example.project.databinding.DialogAddVehicleBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Vehicle;
import com.example.project.repository.AppRepository;
import com.example.project.utils.AccessTimeParser;
import com.example.project.utils.NotificationHelper;
import com.example.project.viewmodel.ReservePlaceViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity for booking a spot with strict validation against owner's access time limits
 * and reactive reservation dispatch via {@link ReservePlaceViewModel}.
 */
public class ReservePlaceActivity extends BaseActivity {

    public static final String EXTRA_PLACE_ID = "EXTRA_PLACE_ID";

    private ActivityReservePlaceBinding binding;
    private ReservePlaceViewModel viewModel;
    private ParkingPlace currentPlace;
    private final List<Vehicle> currentVehiclesList = new ArrayList<>();

    private int allowedStartHour = 8;
    private int allowedStartMinute = 0;
    private int allowedEndHour = 20;
    private int allowedEndMinute = 0;

    private int selectedStartHour = 8;
    private int selectedStartMinute = 0;
    private int selectedEndHour = 20;
    private int selectedEndMinute = 0;

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

        viewModel = new ViewModelProvider(this).get(ReservePlaceViewModel.class);

        String placeId = getIntent() != null ? getIntent().getStringExtra(EXTRA_PLACE_ID) : null;
        currentPlace = viewModel.getPlaceById(placeId);

        if (currentPlace == null) {
            List<ParkingPlace> places = AppRepository.getInstance().getPlacesLiveData().getValue();
            if (places != null && !places.isEmpty()) {
                currentPlace = places.get(0);
            }
        }

        // Prevent owner from reserving their own place
        String currentEmail = viewModel.getUserEmailLiveData().getValue();
        if (currentPlace != null && currentEmail != null && currentEmail.equalsIgnoreCase(currentPlace.getOwnerEmail())) {
            Toast.makeText(this, R.string.error_owner_cannot_reserve, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (currentPlace != null) {
            String accessTime = currentPlace.getAccessTime();
            allowedStartHour = AccessTimeParser.getStartHour(accessTime);
            allowedStartMinute = AccessTimeParser.getStartMinute(accessTime);
            allowedEndHour = AccessTimeParser.getEndHour(accessTime);
            allowedEndMinute = AccessTimeParser.getEndMinute(accessTime);

            selectedStartHour = allowedStartHour;
            selectedStartMinute = allowedStartMinute;
            selectedEndHour = allowedEndHour;
            selectedEndMinute = allowedEndMinute;
        }

        setupUI();
        setupClickListeners();
        observeViewModel();
    }

    private void setupUI() {
        if (binding == null || currentPlace == null) return;

        if (currentPlace.getImageUri() != null && !currentPlace.getImageUri().trim().isEmpty()) {
            try {
                Uri uri = Uri.parse(currentPlace.getImageUri().trim());
                binding.imgPlacePhoto.setImageURI(uri);
                if (binding.imgPlacePhoto.getDrawable() == null) {
                    binding.imgPlacePhoto.setImageResource(currentPlace.getPhotoResId());
                }
            } catch (Exception e) {
                binding.imgPlacePhoto.setImageResource(currentPlace.getPhotoResId());
            }
        } else {
            binding.imgPlacePhoto.setImageResource(currentPlace.getPhotoResId());
        }
        binding.textPlaceStreet.setText(currentPlace.getStreet() != null ? currentPlace.getStreet() : currentPlace.getTitle());
        binding.textPlaceCity.setText(currentPlace.getCity() != null ? currentPlace.getCity() : "Warszawa");

        updatePriceSummary();
    }

    private void updatePriceSummary() {
        if (binding == null || currentPlace == null) return;

        double rate = currentPlace.getPrice();
        double hours = Math.max(0.5, (double) (selectedEndHour * 60 + selectedEndMinute - (selectedStartHour * 60 + selectedStartMinute)) / 60.0);
        double subtotal = rate * hours;
        double serviceFee = 2.50;
        double total = subtotal + serviceFee;

        binding.textSummaryDuration.setText(String.format(Locale.getDefault(), "%.1fh", hours));
        binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka (%.2f PLN/h)", rate));
        binding.textSummaryRateValue.setText(String.format(Locale.getDefault(), "%.2f PLN", subtotal));
        binding.textSummaryTotalSum.setText(String.format(Locale.getDefault(), "%.2f PLN", total));

        binding.textStartTimeValue.setText(String.format(Locale.getDefault(), "Dzisiaj, %02d:%02d", selectedStartHour, selectedStartMinute));
        binding.textEndTimeValue.setText(String.format(Locale.getDefault(), "Dzisiaj, %02d:%02d", selectedEndHour, selectedEndMinute));
    }

    private boolean validateReservationTimeLimits() {
        int selectedStartTotalMin = selectedStartHour * 60 + selectedStartMinute;
        int selectedEndTotalMin = selectedEndHour * 60 + selectedEndMinute;

        int allowedStartTotalMin = allowedStartHour * 60 + allowedStartMinute;
        int allowedEndTotalMin = allowedEndHour * 60 + allowedEndMinute;

        if (selectedStartTotalMin < allowedStartTotalMin) {
            String allowedStr = String.format(Locale.getDefault(), "%02d:%02d", allowedStartHour, allowedStartMinute);
            Toast.makeText(this, getString(R.string.error_reservation_before_allowed, allowedStr), Toast.LENGTH_LONG).show();
            return false;
        }

        if (selectedEndTotalMin > allowedEndTotalMin) {
            String allowedStr = String.format(Locale.getDefault(), "%02d:%02d", allowedEndHour, allowedEndMinute);
            Toast.makeText(this, getString(R.string.error_reservation_after_allowed, allowedStr), Toast.LENGTH_LONG).show();
            return false;
        }

        if (selectedStartTotalMin >= selectedEndTotalMin) {
            Toast.makeText(this, R.string.error_invalid_time_range, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnChangeTime.setOnClickListener(v -> showTimePickerDialogs());

        binding.btnAddVehicle.setOnClickListener(v -> showAddVehicleDialog());

        binding.btnReserveAndPay.setOnClickListener(v -> {
            if (currentPlace == null) return;

            if (!validateReservationTimeLimits()) {
                return;
            }

            String vehicleId = "00000000-0000-0000-0000-0000000000a1";
            int selectedPos = binding.spinnerVehicles.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < currentVehiclesList.size()) {
                Vehicle selectedV = currentVehiclesList.get(selectedPos);
                if (selectedV != null && selectedV.getId() != null && !selectedV.getId().isEmpty()) {
                    vehicleId = selectedV.getId();
                }
            }

            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.HOUR_OF_DAY, selectedStartHour);
            startCal.set(Calendar.MINUTE, selectedStartMinute);
            startCal.set(Calendar.SECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.set(Calendar.HOUR_OF_DAY, selectedEndHour);
            endCal.set(Calendar.MINUTE, selectedEndMinute);
            endCal.set(Calendar.SECOND, 0);

            // Dispatch reservation via ViewModel to ReservationRepository (POST /api/reservations)
            viewModel.submitReservation(
                    currentPlace.getId(),
                    vehicleId,
                    startCal.getTimeInMillis(),
                    endCal.getTimeInMillis(),
                    "pm_card_visa"
            );
        });
    }

    private void observeViewModel() {
        // Observe user vehicles for spinner dropdown
        viewModel.getVehiclesLiveData().observe(this, vehicles -> {
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

        // Observe HTTP 409 Conflict error specifically to display conflict alert
        viewModel.getConflictErrorLiveData().observe(this, conflictMessage -> {
            if (conflictMessage != null && !conflictMessage.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_reservation_conflict)
                        .setMessage(conflictMessage)
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
            }
        });

        // Observe reactive reservation submission status (loading, success, error)
        viewModel.getReservationStatusLiveData().observe(this, resource -> {
            if (binding == null || resource == null) return;

            if (resource.isLoading()) {
                binding.btnReserveAndPay.setEnabled(false);
            } else {
                binding.btnReserveAndPay.setEnabled(true);

                if (resource.isSuccess()) {
                    NotificationHelper.showNotification(this, "ParkShare", getString(R.string.toast_reservation_success));
                    Toast.makeText(this, R.string.toast_reservation_success, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ReservePlaceActivity.this, ReservationsActivity.class);
                    startActivity(intent);
                    finish();
                } else if (resource.isError()) {
                    if (resource.getCode() != 409) {
                        String errorMsg = resource.getMessage() != null
                                ? resource.getMessage()
                                : getString(R.string.error_booking_failed, "Nieznany błąd");
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void showTimePickerDialogs() {
        TimePickerDialog startPicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            TimePickerDialog endPicker = new TimePickerDialog(this, (view2, endHour, endMinute) -> {
                int startMinTotal = hourOfDay * 60 + minute;
                int endMinTotal = endHour * 60 + endMinute;
                int allowedStartMinTotal = allowedStartHour * 60 + allowedStartMinute;
                int allowedEndMinTotal = allowedEndHour * 60 + allowedEndMinute;

                if (startMinTotal < allowedStartMinTotal) {
                    String allowedStr = String.format(Locale.getDefault(), "%02d:%02d", allowedStartHour, allowedStartMinute);
                    Toast.makeText(this, getString(R.string.error_reservation_before_allowed, allowedStr), Toast.LENGTH_LONG).show();
                    return;
                }

                if (endMinTotal > allowedEndMinTotal) {
                    String allowedStr = String.format(Locale.getDefault(), "%02d:%02d", allowedEndHour, allowedEndMinute);
                    Toast.makeText(this, getString(R.string.error_reservation_after_allowed, allowedStr), Toast.LENGTH_LONG).show();
                    return;
                }

                if (startMinTotal >= endMinTotal) {
                    Toast.makeText(this, R.string.error_invalid_time_range, Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedStartHour = hourOfDay;
                selectedStartMinute = minute;
                selectedEndHour = endHour;
                selectedEndMinute = endMinute;

                updatePriceSummary();

            }, selectedEndHour, selectedEndMinute, true);
            endPicker.setTitle("Wybierz czas zakończenia");
            endPicker.show();

        }, selectedStartHour, selectedStartMinute, true);
        startPicker.setTitle("Wybierz czas rozpoczęcia");
        startPicker.show();
    }

    private void showAddVehicleDialog() {
        DialogAddVehicleBinding dialogBinding = DialogAddVehicleBinding.inflate(LayoutInflater.from(this));

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save_vehicle, (dialog, which) -> {
                    String make = dialogBinding.editVehicleMake.getText() != null ? dialogBinding.editVehicleMake.getText().toString() : "";
                    String plate = dialogBinding.editVehiclePlate.getText() != null ? dialogBinding.editVehiclePlate.getText().toString() : "";

                    if (make.trim().isEmpty() || plate.trim().length() < 2 || plate.trim().length() > 15) {
                        Toast.makeText(this, R.string.error_vehicle_validation, Toast.LENGTH_LONG).show();
                        return;
                    }

                    viewModel.addVehicle(make.trim(), plate.trim());
                    Toast.makeText(this, R.string.msg_vehicle_added, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
