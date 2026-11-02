package com.example.project.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.databinding.ActivityReservePlaceBinding;
import com.example.project.databinding.DialogAddVehicleBinding;
import com.example.project.databinding.DialogDateTimePickerBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Vehicle;
import com.example.project.network.model.QuoteResponse;
import com.example.project.repository.AppRepository;
import com.example.project.utils.AccessTimeParser;
import com.example.project.utils.AmenityUtils;
import com.example.project.utils.NotificationHelper;
import com.example.project.viewmodel.ReservePlaceViewModel;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for booking a parking place with precise date and time selection
 * supporting both Wheel Pickers and Direct Text Input, live pricing calculation
 * via backend endpoints, strict validation against calendar blocked periods,
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

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();

    @Override
    protected void onResume() {
        super.onResume();
        if (currentPlace != null) {
            updatePriceSummary();
        }
    }

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

        // Initialize default start and end timestamps.
        // If current actual time is already past opening hour, initialize to now.
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        startCalendar.set(Calendar.HOUR_OF_DAY, allowedStartHour);
        startCalendar.set(Calendar.MINUTE, allowedStartMinute);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        if (startCalendar.before(now)) {
            startCalendar.setTimeInMillis(now.getTimeInMillis());
        }

        endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + 7200000L); // Default +2 hours

        setupUI();
        setupClickListeners();
        observeViewModel();

        // Fetch latest blocked periods for this spot directly from backend
        if (currentPlace != null && currentPlace.getId() != null) {
            com.example.project.repository.AvailabilityRepository.getInstance(this)
                    .fetchBlockedPeriods(currentPlace.getId());
        }
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

        if (currentPlace.getTitle() != null && !currentPlace.getTitle().isEmpty()) {
            binding.textPlaceStreet.setText(currentPlace.getTitle());
            String address = String.format("%s, %s",
                    currentPlace.getCity() != null ? currentPlace.getCity() : "",
                    currentPlace.getStreet() != null ? currentPlace.getStreet() : "").trim();
            if (address.startsWith(",")) address = address.substring(1).trim();
            if (address.endsWith(",")) address = address.substring(0, address.length() - 1).trim();
            binding.textPlaceCity.setText(!address.isEmpty() ? address : "Warszawa");
        } else {
            binding.textPlaceStreet.setText(currentPlace.getStreet() != null ? currentPlace.getStreet() : "Miejsce parkingowe");
            binding.textPlaceCity.setText(currentPlace.getCity() != null ? currentPlace.getCity() : "Warszawa");
        }

        // Amenities under title (if present)
        binding.chipGroupPlaceAmenities.removeAllViews();
        boolean hasAmenities = false;
        List<String> amenities = currentPlace.getAmenities();
        if (amenities != null && !amenities.isEmpty()) {
            for (String amenity : amenities) {
                String label = AmenityUtils.formatAmenity(amenity);
                if (!label.isEmpty()) {
                    Chip chip = new Chip(this);
                    chip.setText(label);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    binding.chipGroupPlaceAmenities.addView(chip);
                    hasAmenities = true;
                }
            }
        }
        binding.chipGroupPlaceAmenities.setVisibility(hasAmenities ? View.VISIBLE : View.GONE);

        // Description under amenities (if present)
        String description = currentPlace.getDescription();
        if ((description == null || description.trim().isEmpty()) && currentPlace.getAccessInstructions() != null && !currentPlace.getAccessInstructions().trim().isEmpty()) {
            description = currentPlace.getAccessInstructions();
        }
        if (description != null && !description.trim().isEmpty()) {
            binding.textPlaceDescription.setText(description.trim());
            binding.textPlaceDescription.setVisibility(View.VISIBLE);
        } else {
            binding.textPlaceDescription.setVisibility(View.GONE);
        }

        updatePriceSummary();
    }

    /**
     * Updates the UI date-time labels, validates against calendar blocked periods,
     * displays collision warnings, and recalculates preliminary total price summary
     * while querying the backend quote endpoint for accurate rate calculation.
     */
    private void updatePriceSummary() {
        if (binding == null || currentPlace == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        binding.textStartDateValue.setText(dateFormat.format(startCalendar.getTime()));
        binding.textStartTimeValue.setText(timeFormat.format(startCalendar.getTime()));

        binding.textEndDateValue.setText(dateFormat.format(endCalendar.getTime()));
        binding.textEndTimeValue.setText(timeFormat.format(endCalendar.getTime()));

        // Check if selected timeframe overlaps with any blocked period
        long reqStartMs = startCalendar.getTimeInMillis();
        long reqEndMs = endCalendar.getTimeInMillis();

        BlockedPeriodEntity conflict = viewModel.findConflictingBlockedPeriod(
                currentPlace.getId(),
                reqStartMs,
                reqEndMs
        );

        if (conflict != null) {
            String warningText = getString(R.string.error_spot_blocked_interval, conflict.getFormattedRange());
            binding.textReservationBlockedWarning.setText(warningText);
            binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
            binding.btnReserveAndPay.setEnabled(false);
        } else if (currentPlace.isBlocked()) {
            binding.textReservationBlockedWarning.setText(R.string.warning_spot_currently_blocked);
            binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
            binding.btnReserveAndPay.setEnabled(false);
        } else if (currentPlace.isOccupied()) {
            binding.textReservationBlockedWarning.setText(R.string.warning_spot_currently_occupied);
            binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
            binding.btnReserveAndPay.setEnabled(false);
        } else {
            binding.textReservationBlockedWarning.setVisibility(View.GONE);
            binding.btnReserveAndPay.setEnabled(true);
        }

        // Local pricing calculation fallback
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

        // Request live backend calculation from POST /api/reservations/quote
        viewModel.fetchQuote(currentPlace.getId(), reqStartMs, reqEndMs);
    }

    /**
     * Updates the spot announcement card with formatted blocked period notices.
     * Displays e.g. "⛔ 18:00 - 20:00 zablokowane" so drivers clearly see owner blocks in the announcement.
     *
     * @param blocks List of blocked periods for this spot
     */
    private void updateBlockedAnnouncement(@Nullable List<BlockedPeriodEntity> blocks) {
        if (binding == null) return;
        if (blocks == null || blocks.isEmpty()) {
            binding.textPlaceBlockedAnnouncement.setVisibility(View.GONE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (BlockedPeriodEntity b : blocks) {
            if (b == null || b.isExpired()) continue;
            if (sb.length() > 0) sb.append("\n");
            String reason = (b.getReason() != null && !b.getReason().trim().isEmpty())
                    ? " (" + b.getReason().trim() + ")" : "";
            sb.append("⛔ ").append(b.getFormattedBlockedNotice()).append(reason);
        }

        if (sb.length() > 0) {
            binding.textPlaceBlockedAnnouncement.setText(sb.toString());
            binding.textPlaceBlockedAnnouncement.setVisibility(View.VISIBLE);
        } else {
            binding.textPlaceBlockedAnnouncement.setVisibility(View.GONE);
        }
    }

    /**
     * Validates that selected reservation period does not violate parking availability hours
     * and does not collide with owner calendar blocked periods.
     *
     * @return True if valid, false otherwise
     */
    private boolean validateReservationTimeLimits() {
        if (startCalendar.getTimeInMillis() >= endCalendar.getTimeInMillis()) {
            Toast.makeText(this, R.string.error_invalid_time_range, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Strict validation against calendar blocked periods and real-time status
        if (currentPlace != null) {
            if (currentPlace.isBlocked()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_spot_blocked)
                        .setMessage(R.string.msg_spot_blocked_by_owner)
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
                return false;
            }

            if (currentPlace.isOccupied()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_spot_occupied)
                        .setMessage(R.string.msg_spot_occupied_by_driver)
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
                return false;
            }

            BlockedPeriodEntity conflict = viewModel.findConflictingBlockedPeriod(
                    currentPlace.getId(),
                    startCalendar.getTimeInMillis(),
                    endCalendar.getTimeInMillis()
            );

            if (conflict != null) {
                String rangeStr = conflict.getFormattedRange();
                String reason = (conflict.getReason() != null && !conflict.getReason().isEmpty())
                        ? " (" + conflict.getReason() + ")" : "";
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_spot_blocked)
                        .setMessage(getString(R.string.error_spot_blocked_interval, rangeStr + reason))
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
                return false;
            }
        }

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

        // Observe blocked periods for this spot to update UI dynamically
        if (currentPlace != null) {
            viewModel.getBlockedPeriodsLiveData(currentPlace.getId()).observe(this, blocks -> {
                updateBlockedAnnouncement(blocks);
                updatePriceSummary();
            });
        }

        // Observe backend calculated price quotes from POST /api/reservations/quote
        viewModel.getQuoteLiveData().observe(this, resource -> {
            if (binding == null || resource == null) return;

            if (resource.isSuccess() && resource.getData() != null) {
                QuoteResponse quote = resource.getData();
                String currency = (quote.getCurrency() != null && !quote.getCurrency().isEmpty())
                        ? quote.getCurrency() : "PLN";

                if (quote.getHourlyRate() != null && quote.getHourlyRate() > 0) {
                    binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka (%.2f %s/h)", quote.getHourlyRate(), currency));
                }
                if (quote.getSpotCost() != null) {
                    binding.textSummaryRateValue.setText(String.format(Locale.getDefault(), "%.2f %s", quote.getSpotCost(), currency));
                }
                if (quote.getServiceFee() != null) {
                    binding.textSummaryServiceFee.setText(String.format(Locale.getDefault(), "%.2f %s", quote.getServiceFee(), currency));
                }
                if (quote.getTotal() != null) {
                    binding.textSummaryTotalSum.setText(String.format(Locale.getDefault(), "%.2f %s", quote.getTotal(), currency));
                }
            } else if (resource.isError() && resource.getMessage() != null && !resource.getMessage().isEmpty()) {
                binding.textReservationBlockedWarning.setText(resource.getMessage());
                binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
                binding.btnReserveAndPay.setEnabled(false);
            }
        });

        // Observe HTTP 409 Conflict error specifically to display conflict alert
        viewModel.getConflictErrorLiveData().observe(this, conflictMessage -> {
            if (conflictMessage != null && !conflictMessage.isEmpty()) {
                int titleResId = R.string.title_reservation_conflict;
                if (conflictMessage.contains("⛔") || conflictMessage.toLowerCase(Locale.ROOT).contains("zablokowan")) {
                    titleResId = R.string.title_spot_blocked;
                } else if (conflictMessage.contains("🚗") || conflictMessage.toLowerCase(Locale.ROOT).contains("zajęt") || conflictMessage.toLowerCase(Locale.ROOT).contains("zajet")) {
                    titleResId = R.string.title_spot_occupied;
                }
                new AlertDialog.Builder(this)
                        .setTitle(titleResId)
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
     * Shows a flexible Material Dialog enabling exact selection of Year, Month, Day,
     * Hour, Minute, and Second (format: yyyy-MM-dd HH:mm:ss) for start and end times,
     * offering an instant toggle between Wheel Pickers and Direct Text Input, as well as
     * an automated "Set Current Time" action button.
     *
     * @param editStartFirst True if initial target is Start datetime, false for End datetime
     */
    private void showDateTimePickerDialog(boolean editStartFirst) {
        DialogDateTimePickerBinding dialogBinding = DialogDateTimePickerBinding.inflate(LayoutInflater.from(this));

        Calendar tempStart = (Calendar) startCalendar.clone();
        Calendar tempEnd = (Calendar) endCalendar.clone();

        // Default start to current actual time if past
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        if (tempStart.before(now)) {
            tempStart.setTimeInMillis(now.getTimeInMillis());
            if (tempEnd.getTimeInMillis() <= tempStart.getTimeInMillis()) {
                tempEnd.setTimeInMillis(tempStart.getTimeInMillis() + 7200000L);
            }
        }

        final boolean[] isEditingStart = new boolean[]{editStartFirst};
        final boolean[] isWheelsMode = new boolean[]{true};

        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
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

        // Disallow past dates in DatePicker
        dialogBinding.datePickerDialog.setMinDate(System.currentTimeMillis() - 60000L);

        Runnable updatePreviews = () -> {
            dialogBinding.textPreviewStartValue.setText(getString(R.string.preview_start_format, fullFormat.format(tempStart.getTime())));
            dialogBinding.textPreviewEndValue.setText(getString(R.string.preview_end_format, fullFormat.format(tempEnd.getTime())));
        };

        // Reads values from wheel pickers into the active working calendar
        Runnable savePickersToTarget = () -> {
            Calendar target = isEditingStart[0] ? tempStart : tempEnd;
            target.set(Calendar.YEAR, dialogBinding.datePickerDialog.getYear());
            target.set(Calendar.MONTH, dialogBinding.datePickerDialog.getMonth());
            target.set(Calendar.DAY_OF_MONTH, dialogBinding.datePickerDialog.getDayOfMonth());
            target.set(Calendar.HOUR_OF_DAY, dialogBinding.pickerHour.getValue());
            target.set(Calendar.MINUTE, dialogBinding.pickerMinute.getValue());
            target.set(Calendar.SECOND, dialogBinding.pickerSecond.getValue());
            target.set(Calendar.MILLISECOND, 0);

            // Synchronize text inputs
            dialogBinding.editTextDate.setText(dateFormat.format(target.getTime()));
            dialogBinding.editTextTime.setText(timeFormat.format(target.getTime()));

            updatePreviews.run();
        };

        // Reads values from text inputs into the active working calendar
        Runnable saveTextToTarget = () -> {
            Calendar target = isEditingStart[0] ? tempStart : tempEnd;
            String dateStr = dialogBinding.editTextDate.getText() != null ? dialogBinding.editTextDate.getText().toString().trim() : "";
            String timeStr = dialogBinding.editTextTime.getText() != null ? dialogBinding.editTextTime.getText().toString().trim() : "";

            if (!dateStr.isEmpty() && !timeStr.isEmpty()) {
                try {
                    String fullStr = dateStr + " " + (timeStr.length() == 5 ? timeStr + ":00" : timeStr);
                    Date parsed = fullFormat.parse(fullStr);
                    if (parsed != null) {
                        target.setTimeInMillis(parsed.getTime());
                        // Synchronize wheel pickers
                        dialogBinding.datePickerDialog.updateDate(
                                target.get(Calendar.YEAR),
                                target.get(Calendar.MONTH),
                                target.get(Calendar.DAY_OF_MONTH)
                        );
                        dialogBinding.pickerHour.setValue(target.get(Calendar.HOUR_OF_DAY));
                        dialogBinding.pickerMinute.setValue(target.get(Calendar.MINUTE));
                        dialogBinding.pickerSecond.setValue(target.get(Calendar.SECOND));
                    }
                } catch (Exception ignored) {
                }
            }
            updatePreviews.run();
        };

        // Synchronizes both DatePicker/NumberPickers and Text fields with the given calendar
        HelperAction<Calendar> loadIntoPickers = target -> {
            dialogBinding.datePickerDialog.updateDate(
                    target.get(Calendar.YEAR),
                    target.get(Calendar.MONTH),
                    target.get(Calendar.DAY_OF_MONTH)
            );
            dialogBinding.pickerHour.setValue(target.get(Calendar.HOUR_OF_DAY));
            dialogBinding.pickerMinute.setValue(target.get(Calendar.MINUTE));
            dialogBinding.pickerSecond.setValue(target.get(Calendar.SECOND));

            dialogBinding.editTextDate.setText(dateFormat.format(target.getTime()));
            dialogBinding.editTextTime.setText(timeFormat.format(target.getTime()));
        };

        // Apply current active target tab styling and load state
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

        // Listeners for user changes in wheel pickers
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

        // TextWatcher for text inputs
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isWheelsMode[0]) {
                    saveTextToTarget.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        dialogBinding.editTextDate.addTextChangedListener(textWatcher);
        dialogBinding.editTextTime.addTextChangedListener(textWatcher);

        // System DatePicker and TimePicker launchers from text input mode
        dialogBinding.btnPickCalendar.setOnClickListener(v -> {
            Calendar target = isEditingStart[0] ? tempStart : tempEnd;
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                target.set(Calendar.YEAR, year);
                target.set(Calendar.MONTH, month);
                target.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                loadIntoPickers.execute(target);
                updatePreviews.run();
            }, target.get(Calendar.YEAR), target.get(Calendar.MONTH), target.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 60000L);
            dpd.show();
        });

        dialogBinding.btnPickClock.setOnClickListener(v -> {
            Calendar target = isEditingStart[0] ? tempStart : tempEnd;
            TimePickerDialog tpd = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                target.set(Calendar.HOUR_OF_DAY, hourOfDay);
                target.set(Calendar.MINUTE, minute);
                target.set(Calendar.SECOND, 0);
                loadIntoPickers.execute(target);
                updatePreviews.run();
            }, target.get(Calendar.HOUR_OF_DAY), target.get(Calendar.MINUTE), true);
            tpd.show();
        });

        // "Now" Button: automatically sets the current actual date and time
        dialogBinding.btnSetNow.setOnClickListener(v -> {
            Calendar currentActual = Calendar.getInstance();
            currentActual.set(Calendar.SECOND, 0);
            currentActual.set(Calendar.MILLISECOND, 0);

            Calendar target = isEditingStart[0] ? tempStart : tempEnd;
            target.setTimeInMillis(currentActual.getTimeInMillis());

            if (isEditingStart[0] && tempEnd.getTimeInMillis() <= tempStart.getTimeInMillis()) {
                tempEnd.setTimeInMillis(tempStart.getTimeInMillis() + 7200000L);
            }

            loadIntoPickers.execute(target);
            updatePreviews.run();
        });

        // Switch between Wheels and Text Input modes
        dialogBinding.radioModeWheels.setOnClickListener(v -> {
            if (!isWheelsMode[0]) {
                saveTextToTarget.run();
                isWheelsMode[0] = true;
                dialogBinding.layoutWheelsContainer.setVisibility(View.VISIBLE);
                dialogBinding.layoutTextContainer.setVisibility(View.GONE);

                dialogBinding.radioModeWheels.setChecked(true);
                dialogBinding.radioModeWheels.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.radioModeWheels.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));

                dialogBinding.radioModeText.setChecked(false);
                dialogBinding.radioModeText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                dialogBinding.radioModeText.setBackgroundTintList(null);

                loadIntoPickers.execute(isEditingStart[0] ? tempStart : tempEnd);
            }
        });

        dialogBinding.radioModeText.setOnClickListener(v -> {
            if (isWheelsMode[0]) {
                savePickersToTarget.run();
                isWheelsMode[0] = false;
                dialogBinding.layoutWheelsContainer.setVisibility(View.GONE);
                dialogBinding.layoutTextContainer.setVisibility(View.VISIBLE);

                dialogBinding.radioModeText.setChecked(true);
                dialogBinding.radioModeText.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.radioModeText.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));

                dialogBinding.radioModeWheels.setChecked(false);
                dialogBinding.radioModeWheels.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                dialogBinding.radioModeWheels.setBackgroundTintList(null);

                loadIntoPickers.execute(isEditingStart[0] ? tempStart : tempEnd);
            }
        });

        // Switching between Start and End tabs
        dialogBinding.radioStartMode.setOnClickListener(v -> {
            if (!isEditingStart[0]) {
                if (isWheelsMode[0]) {
                    savePickersToTarget.run();
                } else {
                    saveTextToTarget.run();
                }
                isEditingStart[0] = true;
                applyMode.run();
            }
        });

        dialogBinding.radioEndMode.setOnClickListener(v -> {
            if (isEditingStart[0]) {
                if (isWheelsMode[0]) {
                    savePickersToTarget.run();
                } else {
                    saveTextToTarget.run();
                }
                isEditingStart[0] = false;
                applyMode.run();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnApplyDialog.setOnClickListener(v -> {
            if (isWheelsMode[0]) {
                savePickersToTarget.run();
            } else {
                saveTextToTarget.run();
            }

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
