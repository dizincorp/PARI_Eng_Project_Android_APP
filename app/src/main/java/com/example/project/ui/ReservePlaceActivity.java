package com.example.project.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityReservePlaceBinding;
import com.example.project.databinding.DialogAddVehicleBinding;
import com.example.project.databinding.DialogDateTimePickerBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Vehicle;
import com.example.project.repository.AppRepository;
import com.example.project.utils.AccessTimeParser;
import com.example.project.utils.NotificationHelper;
import com.example.project.viewmodel.ReservePlaceViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for booking a parking place with precise date and time selection
 * (Year, Month, Day, Hour, Minute, Second) in a styled Material dialog,
 * strict validation against owner's access limits, and reactive dispatch via {@link ReservePlaceViewModel}.
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

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();

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
        }

        // Initialize default start and end timestamps
        startCalendar.set(Calendar.HOUR_OF_DAY, allowedStartHour);
        startCalendar.set(Calendar.MINUTE, allowedStartMinute);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
        endCalendar.set(Calendar.HOUR_OF_DAY, allowedEndHour);
        endCalendar.set(Calendar.MINUTE, allowedEndMinute);
        endCalendar.set(Calendar.SECOND, 0);
        endCalendar.set(Calendar.MILLISECOND, 0);

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

    /**
     * Updates the UI date-time labels and recalculates total price summary.
     */
    private void updatePriceSummary() {
        if (binding == null || currentPlace == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        binding.textStartDateValue.setText(dateFormat.format(startCalendar.getTime()));
        binding.textStartTimeValue.setText(timeFormat.format(startCalendar.getTime()));

        binding.textEndDateValue.setText(dateFormat.format(endCalendar.getTime()));
        binding.textEndTimeValue.setText(timeFormat.format(endCalendar.getTime()));

        double rate = currentPlace.getPrice();
        long diffMs = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
        double hours = Math.max(0.5, (double) diffMs / (1000.0 * 60.0 * 60.0));
        double subtotal = rate * hours;
        double serviceFee = 2.50;
        double total = subtotal + serviceFee;

        binding.textSummaryDuration.setText(String.format(Locale.getDefault(), "%.1fh", hours));
        binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka (%.2f PLN/h)", rate));
        binding.textSummaryRateValue.setText(String.format(Locale.getDefault(), "%.2f PLN", subtotal));
        binding.textSummaryServiceFee.setText(String.format(Locale.getDefault(), "%.2f PLN", serviceFee));
        binding.textSummaryTotalSum.setText(String.format(Locale.getDefault(), "%.2f PLN", total));
    }

    /**
     * Validates that selected reservation period does not violate parking availability hours.
     *
     * @return True if valid, false otherwise
     */
    private boolean validateReservationTimeLimits() {
        int selectedStartMin = startCalendar.get(Calendar.HOUR_OF_DAY) * 60 + startCalendar.get(Calendar.MINUTE);
        int selectedEndMin = endCalendar.get(Calendar.HOUR_OF_DAY) * 60 + endCalendar.get(Calendar.MINUTE);

        int allowedStartMin = allowedStartHour * 60 + allowedStartMinute;
        int allowedEndMin = allowedEndHour * 60 + allowedEndMinute;

        if (selectedStartMin < allowedStartMin) {
            String allowedStr = String.format(Locale.getDefault(), "%02d:%02d", allowedStartHour, allowedStartMinute);
            Toast.makeText(this, getString(R.string.error_reservation_before_allowed, allowedStr), Toast.LENGTH_LONG).show();
            return false;
        }

        if (selectedEndMin > allowedEndMin) {
            String allowedStr = String.format(Locale.getDefault(), "%02d:%02d", allowedEndHour, allowedEndMinute);
            Toast.makeText(this, getString(R.string.error_reservation_after_allowed, allowedStr), Toast.LENGTH_LONG).show();
            return false;
        }

        if (startCalendar.getTimeInMillis() >= endCalendar.getTimeInMillis()) {
            Toast.makeText(this, R.string.error_invalid_time_range, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardStartDatetime.setOnClickListener(v -> showDateTimePickerDialog(true));
        binding.cardEndDatetime.setOnClickListener(v -> showDateTimePickerDialog(false));
        binding.btnChangeTime.setOnClickListener(v -> showDateTimePickerDialog(true));

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

            // Dispatch reservation via ViewModel to ReservationRepository (POST /api/reservations)
            viewModel.submitReservation(
                    currentPlace.getId(),
                    vehicleId,
                    startCalendar.getTimeInMillis(),
                    endCalendar.getTimeInMillis(),
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

    /**
     * Shows a beautiful Material Dialog enabling exact selection of Year, Month, Day,
     * Hour, Minute, and Second (format: yyyy-MM-dd HH:mm:ss) for start and end times.
     *
     * @param editStartFirst True if initial target is Start datetime, false for End datetime
     */
    private void showDateTimePickerDialog(boolean editStartFirst) {
        DialogDateTimePickerBinding dialogBinding = DialogDateTimePickerBinding.inflate(LayoutInflater.from(this));

        Calendar tempStart = (Calendar) startCalendar.clone();
        Calendar tempEnd = (Calendar) endCalendar.clone();
        final boolean[] isEditingStart = new boolean[]{editStartFirst};

        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        NumberPicker.Formatter twoDigitFormatter = i -> String.format(Locale.US, "%02d", i);

        // Configure NumberPickers
        dialogBinding.pickerHour.setMinValue(0);
        dialogBinding.pickerHour.setMaxValue(23);
        dialogBinding.pickerHour.setFormatter(twoDigitFormatter);

        dialogBinding.pickerMinute.setMinValue(0);
        dialogBinding.pickerMinute.setMaxValue(59);
        dialogBinding.pickerMinute.setFormatter(twoDigitFormatter);

        dialogBinding.pickerSecond.setMinValue(0);
        dialogBinding.pickerSecond.setMaxValue(59);
        dialogBinding.pickerSecond.setFormatter(twoDigitFormatter);

        // Disallow past dates
        dialogBinding.datePickerDialog.setMinDate(System.currentTimeMillis() - 1000);

        Runnable updatePreviews = () -> {
            dialogBinding.textPreviewStartValue.setText(getString(R.string.preview_start_format, fullFormat.format(tempStart.getTime())));
            dialogBinding.textPreviewEndValue.setText(getString(R.string.preview_end_format, fullFormat.format(tempEnd.getTime())));
        };

        // Reads values from DatePicker and NumberPickers into the active working calendar
        Runnable savePickersToTarget = () -> {
            Calendar target = isEditingStart[0] ? tempStart : tempEnd;
            target.set(Calendar.YEAR, dialogBinding.datePickerDialog.getYear());
            target.set(Calendar.MONTH, dialogBinding.datePickerDialog.getMonth());
            target.set(Calendar.DAY_OF_MONTH, dialogBinding.datePickerDialog.getDayOfMonth());
            target.set(Calendar.HOUR_OF_DAY, dialogBinding.pickerHour.getValue());
            target.set(Calendar.MINUTE, dialogBinding.pickerMinute.getValue());
            target.set(Calendar.SECOND, dialogBinding.pickerSecond.getValue());
            target.set(Calendar.MILLISECOND, 0);
            updatePreviews.run();
        };

        // Synchronizes DatePicker and NumberPickers with the given calendar
        HelperAction<Calendar> loadIntoPickers = target -> {
            dialogBinding.datePickerDialog.updateDate(
                    target.get(Calendar.YEAR),
                    target.get(Calendar.MONTH),
                    target.get(Calendar.DAY_OF_MONTH)
            );
            dialogBinding.pickerHour.setValue(target.get(Calendar.HOUR_OF_DAY));
            dialogBinding.pickerMinute.setValue(target.get(Calendar.MINUTE));
            dialogBinding.pickerSecond.setValue(target.get(Calendar.SECOND));
        };

        // Apply current active mode styling and load state
        Runnable applyMode = () -> {
            if (isEditingStart[0]) {
                dialogBinding.radioStartMode.setChecked(true);
                dialogBinding.radioStartMode.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.radioStartMode.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));

                dialogBinding.radioEndMode.setChecked(false);
                dialogBinding.radioEndMode.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                dialogBinding.radioEndMode.setBackgroundTintList(null);

                dialogBinding.textActiveTargetLabel.setText(R.string.label_start_time);
                loadIntoPickers.execute(tempStart);
            } else {
                dialogBinding.radioEndMode.setChecked(true);
                dialogBinding.radioEndMode.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.radioEndMode.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));

                dialogBinding.radioStartMode.setChecked(false);
                dialogBinding.radioStartMode.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                dialogBinding.radioStartMode.setBackgroundTintList(null);

                dialogBinding.textActiveTargetLabel.setText(R.string.label_end_time);
                loadIntoPickers.execute(tempEnd);
            }
        };

        applyMode.run();
        updatePreviews.run();

        // Listeners for user changes in pickers
        Calendar initial = isEditingStart[0] ? tempStart : tempEnd;
        dialogBinding.datePickerDialog.init(
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH),
                (view, year, monthOfYear, dayOfMonth) -> savePickersToTarget.run()
        );
        dialogBinding.pickerHour.setOnValueChangedListener((picker, oldVal, newVal) -> savePickersToTarget.run());
        dialogBinding.pickerMinute.setOnValueChangedListener((picker, oldVal, newVal) -> savePickersToTarget.run());
        dialogBinding.pickerSecond.setOnValueChangedListener((picker, oldVal, newVal) -> savePickersToTarget.run());

        // Switching between Start and End tabs
        dialogBinding.radioStartMode.setOnClickListener(v -> {
            if (!isEditingStart[0]) {
                savePickersToTarget.run();
                isEditingStart[0] = true;
                applyMode.run();
            }
        });

        dialogBinding.radioEndMode.setOnClickListener(v -> {
            if (isEditingStart[0]) {
                savePickersToTarget.run();
                isEditingStart[0] = false;
                applyMode.run();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnApplyDialog.setOnClickListener(v -> {
            savePickersToTarget.run();

            if (tempStart.getTimeInMillis() >= tempEnd.getTimeInMillis()) {
                Toast.makeText(this, R.string.error_invalid_time_range, Toast.LENGTH_SHORT).show();
                return;
            }

            startCalendar.setTimeInMillis(tempStart.getTimeInMillis());
            endCalendar.setTimeInMillis(tempEnd.getTimeInMillis());
            updatePriceSummary();
            dialog.dismiss();
        });

        dialogBinding.btnCancelDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Functional interface for internal dialog helper action.
     *
     * @param <T> Input argument type
     */
    private interface HelperAction<T> {
        void execute(T value);
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
