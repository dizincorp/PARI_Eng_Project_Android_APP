package com.example.project.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.project.databinding.ItemCalendarReservationBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.viewmodel.AvailabilityCalendarViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing calendar availability, weekly rules, and slot blocking.
 * Employs View Binding and reactive LiveData observation.
 */
public class AvailabilityCalendarActivity extends BaseActivity {

    private ActivityAvailabilityCalendarBinding binding;
    private AvailabilityCalendarViewModel viewModel;
    private BlockedPeriodsAdapter blockedPeriodsAdapter;
    private final List<ParkingPlace> currentPlacesList = new ArrayList<>();

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

        setupBlockedPeriodsRecyclerView();
        setupClickListeners();
        setupCalendarView();
        observeViewModel();
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
            String timeFrom = binding.editTimeFrom.getText() != null ? binding.editTimeFrom.getText().toString() : "12:00";
            String timeTo = binding.editTimeTo.getText() != null ? binding.editTimeTo.getText().toString() : "18:00";
            String reason = binding.editBlockReason.getText() != null ? binding.editBlockReason.getText().toString() : "";

            String selectedPlaceId = getSelectedPlaceId();
            String selectedPlaceName = getSelectedPlaceName();

            viewModel.blockSelectedSlot(selectedPlaceId, selectedPlaceName, timeFrom, timeTo, reason);
        });
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
            boolean hasBlocks = periods != null && !periods.isEmpty();
            blockedPeriodsAdapter.setBlockedPeriods(periods);
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
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
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
