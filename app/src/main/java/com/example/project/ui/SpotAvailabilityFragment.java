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
import com.example.project.databinding.ItemWeeklyScheduleDayBinding;
import com.example.project.model.DayScheduleConfig;
import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.viewmodel.AddPlaceViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment responsible for configuring parking spot availability:
 * 1. 24/7 (open all day, 7 days a week)
 * 2. Custom weekly schedule with selection of days, custom time intervals, or whole day ("Cały dzień").
 */
public class SpotAvailabilityFragment extends Fragment {

    private FragmentSpotAvailabilityBinding binding;
    private AddPlaceViewModel viewModel;

    private final List<DayScheduleConfig> days = DayScheduleConfig.createDefaultWeek();
    private final List<ItemWeeklyScheduleDayBinding> dayBindings = new ArrayList<>();
    private boolean is247 = true;
    private boolean isUpdatingUI = false;

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
        }

        setupModeSelection();
        setupPresets();
        populateDayViews();
        observeViewModel();
        updateUI();
        notifyViewModel();
    }

    private void setupModeSelection() {
        if (binding == null) return;

        View.OnClickListener select247Listener = v -> selectMode(true);
        binding.containerOpt247.setOnClickListener(select247Listener);
        binding.radioAvailable247.setOnClickListener(select247Listener);

        View.OnClickListener selectCustomListener = v -> selectMode(false);
        binding.containerOptCustom.setOnClickListener(selectCustomListener);
        binding.radioCustomHours.setOnClickListener(selectCustomListener);
    }

    private void selectMode(boolean select247) {
        this.is247 = select247;

        if (!this.is247) {
            // If custom mode chosen and no day is enabled, default to Monday-Friday
            boolean hasAny = false;
            for (DayScheduleConfig d : days) {
                if (d.isEnabled()) {
                    hasAny = true;
                    break;
                }
            }
            if (!hasAny) {
                applyPresetWeekdays();
            }
        }

        updateUI();
        notifyViewModel();
    }

    private void setupPresets() {
        if (binding == null) return;

        binding.btnPresetWeekdays.setOnClickListener(v -> {
            applyPresetWeekdays();
            updateUI();
            notifyViewModel();
        });

        binding.btnPresetAll.setOnClickListener(v -> {
            for (DayScheduleConfig d : days) {
                d.setEnabled(true);
            }
            updateUI();
            notifyViewModel();
        });

        binding.btnPresetClear.setOnClickListener(v -> {
            for (DayScheduleConfig d : days) {
                d.setEnabled(false);
            }
            updateUI();
            notifyViewModel();
        });
    }

    private void applyPresetWeekdays() {
        for (DayScheduleConfig d : days) {
            if (d.getDayOfWeek() >= 1 && d.getDayOfWeek() <= 5) {
                d.setEnabled(true);
            } else {
                d.setEnabled(false);
            }
        }
    }

    private void populateDayViews() {
        if (binding == null || getContext() == null) return;

        binding.containerDays.removeAllViews();
        dayBindings.clear();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < days.size(); i++) {
            final DayScheduleConfig day = days.get(i);
            ItemWeeklyScheduleDayBinding rowBinding = ItemWeeklyScheduleDayBinding.inflate(
                    inflater, binding.containerDays, false
            );

            rowBinding.checkDay.setText(day.getDayName());

            // Checkbox: enable/disable day
            rowBinding.checkDay.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                day.setEnabled(isChecked);
                bindDayRow(rowBinding, day);
                updateSummaryText();
                notifyViewModel();
            });

            // Checkbox: "Cały dzień"
            rowBinding.checkAllDay.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                day.setAllDay(isChecked);
                bindDayRow(rowBinding, day);
                updateSummaryText();
                notifyViewModel();
            });

            // Button: Start time
            rowBinding.btnTimeFrom.setOnClickListener(v -> {
                showTimePicker(day.getStartHour(), day.getStartMinute(), (h, m) -> {
                    day.setStartHour(h);
                    day.setStartMinute(m);
                    if (day.getStartHour() > day.getEndHour() ||
                            (day.getStartHour() == day.getEndHour() && day.getStartMinute() >= day.getEndMinute())) {
                        day.setEndHour(Math.min(23, h + 1));
                        day.setEndMinute(m);
                    }
                    bindDayRow(rowBinding, day);
                    updateSummaryText();
                    notifyViewModel();
                });
            });

            // Button: End time
            rowBinding.btnTimeTo.setOnClickListener(v -> {
                showTimePicker(day.getEndHour(), day.getEndMinute(), (h, m) -> {
                    day.setEndHour(h);
                    day.setEndMinute(m);
                    if (day.getEndHour() < day.getStartHour() ||
                            (day.getEndHour() == day.getStartHour() && day.getEndMinute() <= day.getStartMinute())) {
                        day.setStartHour(Math.max(0, h - 1));
                        day.setStartMinute(m);
                    }
                    bindDayRow(rowBinding, day);
                    updateSummaryText();
                    notifyViewModel();
                });
            });

            bindDayRow(rowBinding, day);
            dayBindings.add(rowBinding);
            binding.containerDays.addView(rowBinding.getRoot());
        }
    }

    private void bindDayRow(ItemWeeklyScheduleDayBinding rowBinding, DayScheduleConfig day) {
        boolean prevUpdating = isUpdatingUI;
        isUpdatingUI = true;
        try {
            rowBinding.checkDay.setChecked(day.isEnabled());
            rowBinding.checkAllDay.setVisibility(day.isEnabled() ? View.VISIBLE : View.GONE);
            rowBinding.checkAllDay.setChecked(day.isAllDay());

            if (!day.isEnabled()) {
                rowBinding.textDayClosed.setVisibility(View.VISIBLE);
                rowBinding.layoutAllDayBadge.setVisibility(View.GONE);
                rowBinding.layoutTimePickers.setVisibility(View.GONE);
            } else {
                rowBinding.textDayClosed.setVisibility(View.GONE);
                if (day.isAllDay()) {
                    rowBinding.layoutAllDayBadge.setVisibility(View.VISIBLE);
                    rowBinding.layoutTimePickers.setVisibility(View.GONE);
                } else {
                    rowBinding.layoutAllDayBadge.setVisibility(View.GONE);
                    rowBinding.layoutTimePickers.setVisibility(View.VISIBLE);
                    rowBinding.textTimeFrom.setText(day.getFormattedStartTime());
                    rowBinding.textTimeTo.setText(day.getFormattedEndTime());
                }
            }
        } finally {
            isUpdatingUI = prevUpdating;
        }
    }

    private void showTimePicker(int initialHour, int initialMinute, OnTimeSelectedListener listener) {
        if (getContext() == null) return;

        TimePickerDialog picker = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    if (listener != null) {
                        listener.onTimeSelected(hourOfDay, minute);
                    }
                },
                initialHour,
                initialMinute,
                true
        );
        picker.show();
    }

    private interface OnTimeSelectedListener {
        void onTimeSelected(int hour, int minute);
    }

    private void observeViewModel() {
        if (viewModel == null) return;

        // Observe rules from ViewModel (populated on spot editing)
        viewModel.getAvailabilityRulesLiveData().observe(getViewLifecycleOwner(), rules -> {
            if (rules != null && !rules.isEmpty()) {
                is247 = false;
                for (DayScheduleConfig d : days) {
                    d.setEnabled(false);
                }
                for (AvailabilityRuleRequest r : rules) {
                    for (DayScheduleConfig d : days) {
                        if (d.getDayOfWeek() == r.getDayOfWeek()) {
                            d.applyRuleRequest(r);
                            break;
                        }
                    }
                }
                updateUI();
            }
        });

        // Observe is247 status
        viewModel.getIs247LiveData().observe(getViewLifecycleOwner(), is247Val -> {
            if (is247Val != null) {
                is247 = is247Val;
                updateUI();
            }
        });

        // Legacy accessTime fallback
        viewModel.getAccessTimeLiveData().observe(getViewLifecycleOwner(), accessTime -> {
            if (accessTime == null || accessTime.trim().isEmpty()) {
                return;
            }
            if ("24/7".equalsIgnoreCase(accessTime.trim()) || "Dostęp 24/7".equalsIgnoreCase(accessTime.trim())) {
                is247 = true;
                updateUI();
            }
        });
    }

    private void updateUI() {
        if (binding == null) return;

        boolean prevUpdating = isUpdatingUI;
        isUpdatingUI = true;
        try {
            if (is247) {
                binding.radioAvailable247.setChecked(true);
                binding.radioCustomHours.setChecked(false);
                binding.layoutCustomSchedule.setVisibility(View.GONE);
            } else {
                binding.radioAvailable247.setChecked(false);
                binding.radioCustomHours.setChecked(true);
                binding.layoutCustomSchedule.setVisibility(View.VISIBLE);
            }

            for (int i = 0; i < days.size() && i < dayBindings.size(); i++) {
                bindDayRow(dayBindings.get(i), days.get(i));
            }

            updateSummaryText();
        } finally {
            isUpdatingUI = prevUpdating;
        }
    }

    private void updateSummaryText() {
        if (binding == null) return;

        StringBuilder sb = new StringBuilder();
        int activeCount = 0;

        for (DayScheduleConfig d : days) {
            if (d.isEnabled()) {
                activeCount++;
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("• ").append(d.getDayName()).append(": ");
                if (d.isAllDay()) {
                    sb.append("Cały dzień (00:00 - 23:59)");
                } else {
                    sb.append(d.getFormattedStartTime()).append(" - ").append(d.getFormattedEndTime());
                }
            }
        }

        if (activeCount == 0) {
            binding.textScheduleSummary.setText(getString(R.string.msg_no_active_schedule_days));
        } else {
            binding.textScheduleSummary.setText(sb.toString());
        }
    }

    private void notifyViewModel() {
        if (viewModel == null) return;

        if (is247) {
            viewModel.setAvailabilitySchedule(true, Collections.emptyList(), "24/7");
        } else {
            List<AvailabilityRuleRequest> rules = new ArrayList<>();
            StringBuilder shortSummary = new StringBuilder();

            for (DayScheduleConfig d : days) {
                if (d.isEnabled()) {
                    AvailabilityRuleRequest req = d.toRuleRequest();
                    if (req != null) {
                        rules.add(req);
                    }
                    if (shortSummary.length() > 0) {
                        shortSummary.append(", ");
                    }
                    shortSummary.append(d.getDayShort()).append(": ");
                    if (d.isAllDay()) {
                        shortSummary.append("Cały dzień");
                    } else {
                        shortSummary.append(d.getFormattedStartTime()).append("-").append(d.getFormattedEndTime());
                    }
                }
            }

            String summaryStr = shortSummary.length() > 0 ? shortSummary.toString() : "Niedostępne";
            viewModel.setAvailabilitySchedule(false, rules, summaryStr);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dayBindings.clear();
        binding = null;
    }
}
