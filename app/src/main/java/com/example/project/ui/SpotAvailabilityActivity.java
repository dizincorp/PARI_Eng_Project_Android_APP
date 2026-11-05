package com.example.project.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;

import com.example.project.R;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.databinding.ActivitySpotAvailabilityBinding;
import com.example.project.databinding.ItemWeeklyScheduleDayBinding;
import com.example.project.model.DayScheduleConfig;
import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.repository.AvailabilityRepository;
import com.example.project.utils.ViewAnimationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dedicated Activity for configuring parking spot availability (24/7 or custom weekly schedule)
 * following a detailed screen paradigm (similar to reservation details).
 */
public class SpotAvailabilityActivity extends BaseActivity {

    public static final String EXTRA_SPOT_ID = "EXTRA_SPOT_ID";
    public static final String EXTRA_SPOT_TITLE = "EXTRA_SPOT_TITLE";
    public static final String EXTRA_IS_24_7 = "EXTRA_IS_24_7";
    public static final String EXTRA_RULES = "EXTRA_RULES";
    public static final String EXTRA_ACCESS_TIME_SUMMARY = "EXTRA_ACCESS_TIME_SUMMARY";

    private ActivitySpotAvailabilityBinding binding;
    private AvailabilityRepository availabilityRepository;

    private String spotId;
    private boolean is247 = true;
    private boolean isUpdatingUI = false;

    private final List<DayScheduleConfig> days = DayScheduleConfig.createDefaultWeek();
    private final List<ItemWeeklyScheduleDayBinding> dayBindings = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySpotAvailabilityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        availabilityRepository = AvailabilityRepository.getInstance(getApplication());

        parseIntentExtras();
        setupClickListeners();
        setupPresets();
        populateDayViews();
        loadExistingRulesIfNeeded();
        updateUI();
    }

    private void parseIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) return;

        spotId = intent.getStringExtra(EXTRA_SPOT_ID);
        String spotTitle = intent.getStringExtra(EXTRA_SPOT_TITLE);
        if (spotTitle != null && !spotTitle.trim().isEmpty()) {
            binding.textHeaderSubtitle.setText(spotTitle.trim());
        }

        is247 = intent.getBooleanExtra(EXTRA_IS_24_7, true);

        Serializable rulesExtra = intent.getSerializableExtra(EXTRA_RULES);
        if (rulesExtra instanceof List<?>) {
            List<?> rawList = (List<?>) rulesExtra;
            List<AvailabilityRuleRequest> inputRules = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof AvailabilityRuleRequest) {
                    inputRules.add((AvailabilityRuleRequest) obj);
                }
            }

            if (!inputRules.isEmpty()) {
                is247 = false;
                for (DayScheduleConfig d : days) {
                    d.setEnabled(false);
                }
                for (AvailabilityRuleRequest r : inputRules) {
                    for (DayScheduleConfig d : days) {
                        if (d.getDayOfWeek() == r.getDayOfWeek()) {
                            d.applyRuleRequest(r);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void loadExistingRulesIfNeeded() {
        if (spotId == null || spotId.trim().isEmpty()) return;

        availabilityRepository.fetchAvailabilityRules(spotId.trim());
        LiveData<List<AvailabilityRuleEntity>> rulesSource =
                availabilityRepository.getRulesLiveData(spotId.trim());

        rulesSource.observe(this, entities -> {
            if (entities != null && !entities.isEmpty()) {
                is247 = false;
                for (DayScheduleConfig d : days) {
                    d.setEnabled(false);
                }
                for (AvailabilityRuleEntity e : entities) {
                    for (DayScheduleConfig d : days) {
                        if (d.getDayOfWeek() == e.getDayOfWeek()) {
                            d.applyRuleRequest(new AvailabilityRuleRequest(
                                    e.getDayOfWeek(),
                                    e.getStartTime(),
                                    e.getEndTime(),
                                    e.getValidFrom(),
                                    e.getValidTo()
                            ));
                            break;
                        }
                    }
                }
                updateUI();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> {
            ViewAnimationUtils.playBounceAnimation(v, null);
            finish();
        });

        View.OnClickListener click247 = v -> selectMode(true);
        binding.containerOpt247.setOnClickListener(click247);
        binding.radioAvailable247.setOnClickListener(click247);

        View.OnClickListener clickCustom = v -> selectMode(false);
        binding.containerOptCustom.setOnClickListener(clickCustom);
        binding.radioCustomHours.setOnClickListener(clickCustom);

        binding.btnSaveAvailability.setOnClickListener(v -> {
            ViewAnimationUtils.playBounceAnimation(v, () -> saveScheduleAndFinish());
        });
    }

    private void selectMode(boolean select247) {
        this.is247 = select247;

        if (!this.is247) {
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
    }

    private void setupPresets() {
        binding.btnPresetWeekdays.setOnClickListener(v -> {
            applyPresetWeekdays();
            updateUI();
        });

        binding.btnPresetAll.setOnClickListener(v -> {
            for (DayScheduleConfig d : days) {
                d.setEnabled(true);
            }
            updateUI();
        });

        binding.btnPresetClear.setOnClickListener(v -> {
            for (DayScheduleConfig d : days) {
                d.setEnabled(false);
            }
            updateUI();
        });
    }

    private void applyPresetWeekdays() {
        for (DayScheduleConfig d : days) {
            d.setEnabled(d.getDayOfWeek() >= 1 && d.getDayOfWeek() <= 5);
        }
    }

    private void populateDayViews() {
        binding.containerDays.removeAllViews();
        dayBindings.clear();

        LayoutInflater inflater = LayoutInflater.from(this);

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
            });

            // Checkbox: "Cały dzień"
            rowBinding.checkAllDay.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                day.setAllDay(isChecked);
                bindDayRow(rowBinding, day);
                updateSummaryText();
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
                });
            });

            bindDayRow(rowBinding, day);
            dayBindings.add(rowBinding);
            binding.containerDays.addView(rowBinding.getRoot());
        }
    }

    private void bindDayRow(ItemWeeklyScheduleDayBinding rowBinding, DayScheduleConfig day) {
        boolean prev = isUpdatingUI;
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
            isUpdatingUI = prev;
        }
    }

    private void showTimePicker(int initialHour, int initialMinute, OnTimeSelectedListener listener) {
        TimePickerDialog picker = new TimePickerDialog(
                this,
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

    private void updateUI() {
        if (binding == null) return;

        boolean prev = isUpdatingUI;
        isUpdatingUI = true;
        try {
            if (is247) {
                binding.radioAvailable247.setChecked(true);
                binding.radioCustomHours.setChecked(false);
                binding.cardCustomSchedule.setVisibility(View.GONE);
            } else {
                binding.radioAvailable247.setChecked(false);
                binding.radioCustomHours.setChecked(true);
                binding.cardCustomSchedule.setVisibility(View.VISIBLE);
            }

            for (int i = 0; i < days.size() && i < dayBindings.size(); i++) {
                bindDayRow(dayBindings.get(i), days.get(i));
            }

            updateSummaryText();
        } finally {
            isUpdatingUI = prev;
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

    private void saveScheduleAndFinish() {
        List<AvailabilityRuleRequest> rules = new ArrayList<>();
        String summaryStr;

        if (is247) {
            summaryStr = "Całodobowo 24/7";
        } else {
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

            if (rules.isEmpty()) {
                Toast.makeText(this, R.string.msg_no_active_schedule_days, Toast.LENGTH_SHORT).show();
                return;
            }

            summaryStr = shortSummary.toString();
        }

        // If a persistent spotId is present, persist rules directly via backend API
        if (spotId != null && !spotId.trim().isEmpty()) {
            availabilityRepository.replaceAvailabilityRules(spotId.trim(), is247 ? Collections.emptyList() : rules);
        }

        // Return results to calling activity (e.g. AddPlaceActivity or AvailabilityCalendarActivity)
        Intent resultData = new Intent();
        resultData.putExtra(EXTRA_IS_24_7, is247);
        resultData.putExtra(EXTRA_RULES, new ArrayList<>(rules));
        resultData.putExtra(EXTRA_ACCESS_TIME_SUMMARY, summaryStr);
        setResult(RESULT_OK, resultData);

        Toast.makeText(this, R.string.toast_rules_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dayBindings.clear();
        binding = null;
    }
}
