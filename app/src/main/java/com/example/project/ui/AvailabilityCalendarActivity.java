package com.example.project.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project.R;
import com.example.project.adapter.BlockedPeriodsAdapter;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.databinding.ActivityAvailabilityCalendarBinding;
import com.example.project.databinding.DialogDateTimePickerBinding;
import com.example.project.databinding.ItemCalendarReservationBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.viewmodel.AvailabilityCalendarViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing calendar availability, weekly rules, and slot blocking.
 * Employs View Binding and reactive LiveData observation with interactive time-range selection
 * supporting both Wheel Pickers and Direct Text Input matching reservation behavior.
 */
public class AvailabilityCalendarActivity extends BaseActivity {

    private ActivityAvailabilityCalendarBinding binding;
    private AvailabilityCalendarViewModel viewModel;
    private BlockedPeriodsAdapter blockedPeriodsAdapter;
    private final List<ParkingPlace> currentPlacesList = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshBlockedPeriods();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAvailabilityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_places);

        viewModel = new ViewModelProvider(this).get(AvailabilityCalendarViewModel.class);

        // Initialize default hours with current actual time
        initDefaultTimes();

        setupBlockedPeriodsRecyclerView();
        setupClickListeners();
        setupCalendarView();
        observeViewModel();
    }

    /**
     * Initializes default blocking time fields to actual current time and +2 hours.
     */
    private void initDefaultTimes() {
        if (binding == null) return;
        Calendar initCal = Calendar.getInstance();
        initCal.set(Calendar.SECOND, 0);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        binding.editTimeFrom.setText(format.format(initCal.getTime()));
        initCal.add(Calendar.HOUR_OF_DAY, 2);
        binding.editTimeTo.setText(format.format(initCal.getTime()));
    }

    private void setupBlockedPeriodsRecyclerView() {
        if (binding == null) {
            return;
        }

        blockedPeriodsAdapter = new BlockedPeriodsAdapter();
        blockedPeriodsAdapter.setOnUnblockClickListener(this::showUnblockConfirmationDialog);

        binding.recyclerBlockedPeriods.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBlockedPeriods.setAdapter(blockedPeriodsAdapter);
    }

    private void showUnblockConfirmationDialog(@NonNull BlockedPeriodEntity block) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_unblock_title)
                .setMessage(R.string.confirm_unblock_message)
                .setPositiveButton(R.string.btn_unblock, (dialog, which) -> {
                    String selectedPlaceId = getSelectedPlaceId();
                    viewModel.unblockPeriod(selectedPlaceId, block.getId());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupCalendarView() {
        if (binding == null) {
            return;
        }

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            viewModel.setSelectedDate(selectedDateStr);
        });
    }

    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnReturnToPlaces.setOnClickListener(v -> finish());

        // Interactive time cards and buttons triggering the picker dialog
        binding.cardStartDatetime.setOnClickListener(v -> showTimeRangePickerDialog(true));
        binding.cardEndDatetime.setOnClickListener(v -> showTimeRangePickerDialog(false));
        binding.btnChangeTime.setOnClickListener(v -> showTimeRangePickerDialog(true));

        // Quick action: set to actual current time
        binding.btnQuickSetNow.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.SECOND, 0);
            SimpleDateFormat displayTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            binding.editTimeFrom.setText(displayTimeFormat.format(now.getTime()));
            now.add(Calendar.HOUR_OF_DAY, 2);
            binding.editTimeTo.setText(displayTimeFormat.format(now.getTime()));
            Toast.makeText(this, R.string.btn_set_current_time, Toast.LENGTH_SHORT).show();
        });

        binding.spinnerParkingPlaces.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < currentPlacesList.size()) {
                    ParkingPlace selected = currentPlacesList.get(position);
                    if (selected != null) {
                        viewModel.setSelectedSpotId(selected.getId());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op
            }
        });

        binding.btnBlockSlot.setOnClickListener(v -> {
            String timeFrom = binding.editTimeFrom.getText() != null ? binding.editTimeFrom.getText().toString().trim() : "12:00";
            String timeTo = binding.editTimeTo.getText() != null ? binding.editTimeTo.getText().toString().trim() : "18:00";
            String reason = binding.editBlockReason.getText() != null ? binding.editBlockReason.getText().toString().trim() : "";

            // Validate that from < to
            Calendar checkStart = Calendar.getInstance();
            Calendar checkEnd = Calendar.getInstance();
            parseTimeIntoCalendar(checkStart, timeFrom, 12, 0);
            parseTimeIntoCalendar(checkEnd, timeTo, 18, 0);
            if (checkStart.getTimeInMillis() >= checkEnd.getTimeInMillis()) {
                Toast.makeText(this, R.string.error_invalid_time_range, Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedPlaceId = getSelectedPlaceId();
            String selectedPlaceName = getSelectedPlaceName();

            viewModel.blockSelectedSlot(selectedPlaceId, selectedPlaceName, timeFrom, timeTo, reason);
        });
    }

    /**
     * Shows a flexible Material Dialog enabling exact selection of Year, Month, Day,
     * Hour, Minute, and Second for blocking time range, offering a toggle between Wheel Pickers
     * and Direct Text Input, as well as an automated "Set Current Time" action button matching
     * reservation functionality.
     *
     * @param editStartFirst True if initial target is Start datetime, false for End datetime
     */
    private void showTimeRangePickerDialog(boolean editStartFirst) {
        DialogDateTimePickerBinding dialogBinding = DialogDateTimePickerBinding.inflate(LayoutInflater.from(this));

        Calendar tempStart = Calendar.getInstance();
        Calendar tempEnd = Calendar.getInstance();

        // 1. Determine target date from selected calendar date
        String currentDateStr = viewModel.getSelectedDateLiveData().getValue();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        if (currentDateStr != null && !currentDateStr.trim().isEmpty()) {
            try {
                Date d = dateFormat.parse(currentDateStr.trim());
                if (d != null) {
                    tempStart.setTime(d);
                    tempEnd.setTime(d);
                }
            } catch (Exception ignored) {}
        }

        // 2. Parse times from editTimeFrom and editTimeTo
        String fromStr = binding.editTimeFrom.getText() != null ? binding.editTimeFrom.getText().toString().trim() : "12:00";
        String toStr = binding.editTimeTo.getText() != null ? binding.editTimeTo.getText().toString().trim() : "18:00";

        parseTimeIntoCalendar(tempStart, fromStr, 12, 0);
        parseTimeIntoCalendar(tempEnd, toStr, 18, 0);

        // If today and tempStart is in the past, advance to now
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        if (isSameDay(tempStart, now) && tempStart.before(now)) {
            tempStart.setTimeInMillis(now.getTimeInMillis());
            if (tempEnd.getTimeInMillis() <= tempStart.getTimeInMillis()) {
                tempEnd.setTimeInMillis(tempStart.getTimeInMillis() + 7200000L); // +2h
            }
        }

        final boolean[] isEditingStart = new boolean[]{editStartFirst};
        final boolean[] isWheelsMode = new boolean[]{true};
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
            String dateText = dialogBinding.editTextDate.getText() != null ? dialogBinding.editTextDate.getText().toString().trim() : "";
            String timeText = dialogBinding.editTextTime.getText() != null ? dialogBinding.editTextTime.getText().toString().trim() : "";

            if (!dateText.isEmpty() && !timeText.isEmpty()) {
                try {
                    String fullStr = dateText + " " + (timeText.length() == 5 ? timeText + ":00" : timeText);
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
                } catch (Exception ignored) {}
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

        // Apply active target tab (Start vs End)
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

        // System DatePicker & TimePicker buttons
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

        // "Now" Button
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

        // Toggle Wheels vs Text Input
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

            // Update UI fields
            binding.editTimeFrom.setText(displayTimeFormat.format(tempStart.getTime()));
            binding.editTimeTo.setText(displayTimeFormat.format(tempEnd.getTime()));

            // Update calendar date if user selected a different day in the dialog
            String newDateStr = dateFormat.format(tempStart.getTime());
            viewModel.setSelectedDate(newDateStr);
            binding.calendarView.setDate(tempStart.getTimeInMillis(), true, true);

            dialog.dismiss();
        });

        dialogBinding.btnCancelDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void parseTimeIntoCalendar(Calendar cal, String timeStr, int defHour, int defMin) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            cal.set(Calendar.HOUR_OF_DAY, defHour);
            cal.set(Calendar.MINUTE, defMin);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return;
        }
        String[] parts = timeStr.trim().split(":");
        try {
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int s = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, s);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            cal.set(Calendar.HOUR_OF_DAY, defHour);
            cal.set(Calendar.MINUTE, defMin);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private interface HelperAction<T> {
        void execute(T value);
    }

    private String getSelectedPlaceId() {
        int selectedPos = binding.spinnerParkingPlaces.getSelectedItemPosition();
        if (selectedPos >= 0 && selectedPos < currentPlacesList.size()) {
            ParkingPlace p = currentPlacesList.get(selectedPos);
            if (p != null) {
                return p.getId();
            }
        }
        return "";
    }

    private String getSelectedPlaceName() {
        int selectedPos = binding.spinnerParkingPlaces.getSelectedItemPosition();
        if (selectedPos >= 0 && selectedPos < currentPlacesList.size()) {
            ParkingPlace p = currentPlacesList.get(selectedPos);
            if (p != null) {
                return p.getDisplayName();
            }
        }
        return getString(R.string.select_parking_place);
    }

    private void observeViewModel() {
        viewModel.getPlacesLiveData().observe(this, places -> {
            if (binding == null || places == null) {
                return;
            }

            currentPlacesList.clear();
            currentPlacesList.addAll(places);

            List<String> placeNames = new ArrayList<>();
            for (ParkingPlace p : places) {
                placeNames.add(p.getDisplayName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    placeNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerParkingPlaces.setAdapter(adapter);

            if (!currentPlacesList.isEmpty()) {
                viewModel.setSelectedSpotId(currentPlacesList.get(0).getId());
            }
        });

        viewModel.getSelectedDateLiveData().observe(this, dateStr -> {
            if (binding != null && dateStr != null) {
                binding.textSelectedDateTitle.setText(getString(R.string.format_reservations_for_date, dateStr));
            }
        });

        viewModel.getDateReservationsLiveData().observe(this, this::updateDateReservationsList);

        viewModel.getBlockedPeriodsLiveData().observe(this, periods -> {
            if (binding == null) return;
            List<BlockedPeriodEntity> activePeriods = new ArrayList<>();
            if (periods != null) {
                for (BlockedPeriodEntity b : periods) {
                    if (b != null && !b.isExpired()) {
                        activePeriods.add(b);
                    }
                }
            }
            boolean hasBlocks = !activePeriods.isEmpty();
            blockedPeriodsAdapter.setBlockedPeriods(activePeriods);
            binding.recyclerBlockedPeriods.setVisibility(hasBlocks ? View.VISIBLE : View.GONE);
            binding.textNoBlockedPeriods.setVisibility(hasBlocks ? View.GONE : View.VISIBLE);
        });

        viewModel.getRulesLiveData().observe(this, this::updateWeeklyRulesUI);

        viewModel.getBlockSlotSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.msg_slot_blocked, Toast.LENGTH_SHORT).show();
                if (binding != null) {
                    binding.editBlockReason.setText("");
                }
            }
        });

        viewModel.getUnblockSlotSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.msg_slot_unblocked, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                if (errorMsg.contains("401")) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.title_session_expired)
                            .setMessage(R.string.error_session_expired_401)
                            .setPositiveButton(R.string.btn_relogin, (dialog, which) -> {
                                com.example.project.utils.SessionManager.getInstance(this).logoutUser();
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateWeeklyRulesUI(@Nullable List<AvailabilityRuleEntity> rules) {
        if (binding == null) return;

        if (rules == null || rules.isEmpty()) {
            binding.textWeeklyRulesSummary.setText(R.string.availability_open_24_7);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (AvailabilityRuleEntity rule : rules) {
            String dayName = getDayOfWeekName(rule.getDayOfWeek());
            sb.append(dayName)
                    .append(": ")
                    .append(rule.getStartTime())
                    .append(" - ")
                    .append(rule.getEndTime())
                    .append("\n");
        }
        binding.textWeeklyRulesSummary.setText(sb.toString().trim());
    }

    private String getDayOfWeekName(int day) {
        switch (day) {
            case 1: return getString(R.string.rule_day_monday);
            case 2: return getString(R.string.rule_day_tuesday);
            case 3: return getString(R.string.rule_day_wednesday);
            case 4: return getString(R.string.rule_day_thursday);
            case 5: return getString(R.string.rule_day_friday);
            case 6: return getString(R.string.rule_day_saturday);
            case 7: return getString(R.string.rule_day_sunday);
            default: return "";
        }
    }

    private void updateDateReservationsList(List<Reservation> reservations) {
        if (binding == null) {
            return;
        }

        binding.containerDateReservations.removeAllViews();

        if (reservations == null || reservations.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Reservation r : reservations) {
            if (r == null) {
                continue;
            }

            ItemCalendarReservationBinding itemBinding = ItemCalendarReservationBinding.inflate(inflater, binding.containerDateReservations, false);

            itemBinding.textReservationTime.setText(r.getTimeRange() != null ? r.getTimeRange() : "");
            itemBinding.textPlaceName.setText(r.getPlaceName() != null ? r.getPlaceName() : "");
            itemBinding.textUserName.setText(r.getUserName() != null ? r.getUserName() : "");

            String carModel = r.getCarModel() != null ? r.getCarModel() : "";
            String carPlate = r.getCarPlate() != null ? r.getCarPlate() : "";
            itemBinding.textCarDetails.setText(getString(R.string.format_sub_details, carModel, carPlate));

            binding.containerDateReservations.addView(itemBinding.getRoot());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
