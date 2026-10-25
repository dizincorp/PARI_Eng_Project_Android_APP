package com.example.project.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.FragmentSpotAvailabilityBinding;
import com.example.project.utils.AccessTimeParser;
import com.example.project.viewmodel.AddPlaceViewModel;

import java.util.Locale;

/**
 * Fragment responsible for managing parking spot availability hours (24/7 or custom time schedule).
 * Communicates reactively with the parent {@link AddPlaceActivity} via {@link AddPlaceViewModel}.
 */
public class SpotAvailabilityFragment extends Fragment {

    private FragmentSpotAvailabilityBinding binding;
    private AddPlaceViewModel viewModel;

    private int startHour = 8;
    private int startMinute = 0;
    private int endHour = 20;
    private int endMinute = 0;
    private boolean is247 = false;

    /**
     * Default constructor for SpotAvailabilityFragment.
     */
    public SpotAvailabilityFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSpotAvailabilityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            viewModel = new ViewModelProvider(requireActivity()).get(AddPlaceViewModel.class);
            observeViewModel();
        }

        setupListeners();
        updateUI();
    }

    /**
     * Sets up click and check listeners for availability selection and TimePicker dialogs.
     */
    private void setupListeners() {
        if (binding == null) return;

        binding.radioGroupAvailability.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_available_24_7) {
                is247 = true;
                binding.layoutCustomHours.setVisibility(View.GONE);
                notifyViewModel();
            } else if (checkedId == R.id.radio_custom_hours) {
                is247 = false;
                binding.layoutCustomHours.setVisibility(View.VISIBLE);
                notifyViewModel();
            }
        });

        binding.btnPickTimeFrom.setOnClickListener(v -> showStartTimePicker());
        binding.btnPickTimeTo.setOnClickListener(v -> showEndTimePicker());
    }

    /**
     * Displays standard TimePickerDialog for start hour selection.
     */
    private void showStartTimePicker() {
        if (getContext() == null) return;

        TimePickerDialog picker = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    startHour = hourOfDay;
                    startMinute = minute;
                    updateUI();
                    notifyViewModel();
                },
                startHour,
                startMinute,
                true
        );
        picker.setTitle(getString(R.string.picker_time_from_title));
        picker.show();
    }

    /**
     * Displays standard TimePickerDialog for end hour selection.
     */
    private void showEndTimePicker() {
        if (getContext() == null) return;

        TimePickerDialog picker = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    endHour = hourOfDay;
                    endMinute = minute;
                    updateUI();
                    notifyViewModel();
                },
                endHour,
                endMinute,
                true
        );
        picker.setTitle(getString(R.string.picker_time_to_title));
        picker.show();
    }

    /**
     * Observes the access time LiveData from the shared ViewModel to populate initial or editing state.
     */
    private void observeViewModel() {
        if (viewModel == null) return;

        viewModel.getAccessTimeLiveData().observe(getViewLifecycleOwner(), accessTime -> {
            if (accessTime == null || accessTime.trim().isEmpty()) {
                return;
            }
            if ("24/7".equalsIgnoreCase(accessTime.trim()) || "Dostęp 24/7".equalsIgnoreCase(accessTime.trim())) {
                is247 = true;
                if (binding != null) {
                    binding.radioAvailable247.setChecked(true);
                    binding.layoutCustomHours.setVisibility(View.GONE);
                }
            } else {
                is247 = false;
                startHour = AccessTimeParser.getStartHour(accessTime);
                startMinute = AccessTimeParser.getStartMinute(accessTime);
                endHour = AccessTimeParser.getEndHour(accessTime);
                endMinute = AccessTimeParser.getEndMinute(accessTime);
                if (binding != null) {
                    binding.radioCustomHours.setChecked(true);
                    binding.layoutCustomHours.setVisibility(View.VISIBLE);
                    updateUI();
                }
            }
        });
    }

    /**
     * Updates TextView representations of selected start and end hours.
     */
    private void updateUI() {
        if (binding == null) return;

        binding.textTimeFrom.setText(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
        binding.textTimeTo.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
    }

    /**
     * Formats current availability schedule and posts it to {@link AddPlaceViewModel}.
     */
    private void notifyViewModel() {
        if (viewModel == null) return;

        String formattedSchedule;
        if (is247) {
            formattedSchedule = "24/7";
        } else {
            formattedSchedule = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d",
                    startHour, startMinute, endHour, endMinute);
        }
        viewModel.setAccessTime(formattedSchedule);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
