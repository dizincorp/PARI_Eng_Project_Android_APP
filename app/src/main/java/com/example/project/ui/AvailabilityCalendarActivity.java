package com.example.project.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityAvailabilityCalendarBinding;
import com.example.project.databinding.ItemCalendarReservationBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Reservation;
import com.example.project.viewmodel.AvailabilityCalendarViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing calendar availability and blocking slots.
 */
public class AvailabilityCalendarActivity extends BaseActivity {

    private ActivityAvailabilityCalendarBinding binding;
    private AvailabilityCalendarViewModel viewModel;
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

        setupClickListeners();
        setupCalendarView();
        observeViewModel();
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

        binding.btnBlockSlot.setOnClickListener(v -> {
            String timeFrom = binding.editTimeFrom.getText() != null ? binding.editTimeFrom.getText().toString() : "12:00";
            String timeTo = binding.editTimeTo.getText() != null ? binding.editTimeTo.getText().toString() : "18:00";

            String selectedPlaceId = "";
            String selectedPlaceName = "Miejsce Parkingowe";

            int selectedPos = binding.spinnerParkingPlaces.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < currentPlacesList.size()) {
                ParkingPlace p = currentPlacesList.get(selectedPos);
                if (p != null) {
                    selectedPlaceId = p.getId();
                    selectedPlaceName = p.getDisplayName();
                }
            }

            viewModel.blockSelectedSlot(selectedPlaceId, selectedPlaceName, timeFrom, timeTo);
        });
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
        });

        viewModel.getSelectedDateLiveData().observe(this, dateStr -> {
            if (binding != null && dateStr != null) {
                binding.textSelectedDateTitle.setText(getString(R.string.format_reservations_for_date, dateStr));
            }
        });

        viewModel.getDateReservationsLiveData().observe(this, this::updateDateReservationsList);

        viewModel.getBlockSlotSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.msg_slot_blocked, Toast.LENGTH_SHORT).show();
            }
        });
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