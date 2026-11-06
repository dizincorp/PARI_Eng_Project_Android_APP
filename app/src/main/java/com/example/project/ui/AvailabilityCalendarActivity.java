package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.project.R;
import com.example.project.adapter.CalendarDayAdapter;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.databinding.ActivityAvailabilityCalendarBinding;
import com.example.project.databinding.ItemCalendarBlockBinding;
import com.example.project.databinding.ItemCalendarReservationBinding;
import com.example.project.model.CalendarDay;
import com.example.project.model.ParkingPlace;
import com.example.project.utils.ImageLoader;
import com.example.project.utils.ViewAnimationUtils;
import com.example.project.viewmodel.AvailabilityCalendarViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing calendar availability, visual overview of reservations,
 * access hours, and spot blocking. Restricts spots to the owner's own places,
 * supports both aggregate and individual spot views, and renders a color-coded
 * monthly calendar (Green = Free, Yellow = Reserved, Red = Blocked).
 */
public class AvailabilityCalendarActivity extends BaseActivity {

    private ActivityAvailabilityCalendarBinding binding;
    private AvailabilityCalendarViewModel viewModel;
    private CalendarDayAdapter calendarDayAdapter;

    private final List<ParkingPlace> currentPlacesList = new ArrayList<>();
    private final List<BlockedPeriodEntity> currentBlockedPeriods = new ArrayList<>();
    private final List<ReservationEntity> currentReservations = new ArrayList<>();
    private final List<AvailabilityRuleEntity> currentRules = new ArrayList<>();

    private final Calendar displayedMonthCal = Calendar.getInstance();
    private String selectedDateStr = "";

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

        // Initialize to today
        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        selectedDateStr = sdf.format(today.getTime());
        viewModel.setSelectedDate(selectedDateStr);

        displayedMonthCal.setTime(today.getTime());

        initDefaultTimes();
        setupCalendarView();
        setupClickListeners();
        observeViewModel();
    }

    private void initDefaultTimes() {
        if (binding == null) return;
        Calendar initCal = Calendar.getInstance();
        initCal.set(Calendar.SECOND, 0);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        binding.editTimeFrom.setText(format.format(initCal.getTime()));
        initCal.add(Calendar.HOUR_OF_DAY, 2);
        binding.editTimeTo.setText(format.format(initCal.getTime()));
    }

    private void setupCalendarView() {
        if (binding == null) return;

        binding.recyclerCalendarDays.setLayoutManager(new GridLayoutManager(this, 7));
        calendarDayAdapter = new CalendarDayAdapter();
        calendarDayAdapter.setOnDayClickListener(day -> selectDate(day.getDateString()));
        binding.recyclerCalendarDays.setAdapter(calendarDayAdapter);

        updateMonthHeader();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        // Month Navigation
        binding.btnPrevMonth.setOnClickListener(v -> {
            displayedMonthCal.add(Calendar.MONTH, -1);
            updateMonthHeader();
            renderCalendarGrid();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            displayedMonthCal.add(Calendar.MONTH, 1);
            updateMonthHeader();
            renderCalendarGrid();
        });

        binding.btnToday.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            displayedMonthCal.setTime(today.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            selectDate(sdf.format(today.getTime()));
            updateMonthHeader();
            renderCalendarGrid();
        });

        // Weekly schedule rules edit button (Requirement 5)
        binding.btnEditWeeklyRules.setOnClickListener(v -> {
            ViewAnimationUtils.playBounceAnimation(v, () -> {
                String spotId = viewModel.getSelectedSpotId();
                if (AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(spotId)) {
                    if (!currentPlacesList.isEmpty()) {
                        spotId = currentPlacesList.get(0).getId();
                    } else {
                        Toast.makeText(this, "Brak dodanych miejsc parkingowych", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Intent intent = new Intent(AvailabilityCalendarActivity.this, SpotAvailabilityActivity.class);
                intent.putExtra(SpotAvailabilityActivity.EXTRA_SPOT_ID, spotId);
                startActivity(intent);
            });
        });

        // Quick time presets for blocking form
        binding.chipPresetAllDay.setOnClickListener(v -> {
            binding.editTimeFrom.setText("00:00");
            binding.editTimeTo.setText("23:59");
        });

        binding.chipPresetAfternoon.setOnClickListener(v -> {
            binding.editTimeFrom.setText("16:00");
            binding.editTimeTo.setText("20:00");
        });

        binding.chipPresetNowPlus2.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.SECOND, 0);
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            binding.editTimeFrom.setText(tf.format(now.getTime()));
            now.add(Calendar.HOUR_OF_DAY, 2);
            binding.editTimeTo.setText(tf.format(now.getTime()));
        });

        // Block Slot Button (Requirement 4)
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

            String targetSpotId;
            String targetSpotName;

            if (AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(viewModel.getSelectedSpotId())) {
                int pos = binding.spinnerBlockSpot.getSelectedItemPosition();
                if (pos >= 0 && pos < currentPlacesList.size()) {
                    ParkingPlace p = currentPlacesList.get(pos);
                    targetSpotId = p.getId();
                    targetSpotName = p.getDisplayName();
                } else {
                    Toast.makeText(this, "Wybierz miejsce parkingowe do zablokowania", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                targetSpotId = viewModel.getSelectedSpotId();
                targetSpotName = getPlaceNameById(targetSpotId);
            }

            viewModel.blockSelectedSlot(targetSpotId, targetSpotName, timeFrom, timeTo, reason, selectedDateStr);
        });
    }

    private void observeViewModel() {
        // Observe owner's places strictly (Requirement 1 & 3)
        viewModel.getPlacesLiveData().observe(this, places -> {
            if (places == null) return;
            currentPlacesList.clear();
            currentPlacesList.addAll(places);

            renderPlacesFilterChips();
            updateSpinnerPlacesForBlock();
            renderCalendarGrid();
            updateDayDetails(selectedDateStr);
        });

        // Observe blocked periods
        viewModel.getBlockedPeriodsLiveData().observe(this, periods -> {
            currentBlockedPeriods.clear();
            if (periods != null) {
                currentBlockedPeriods.addAll(periods);
            }
            renderCalendarGrid();
            updateDayDetails(selectedDateStr);
        });

        // Observe reservations
        viewModel.getReservationsLiveData().observe(this, reservations -> {
            currentReservations.clear();
            if (reservations != null) {
                currentReservations.addAll(reservations);
            }
            renderCalendarGrid();
            updateDayDetails(selectedDateStr);
        });

        // Observe availability rules
        viewModel.getRulesLiveData().observe(this, rules -> {
            currentRules.clear();
            if (rules != null) {
                currentRules.addAll(rules);
            }
            updateDayDetails(selectedDateStr);
        });

        // Status messages
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
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void selectDate(@NonNull String dateStr) {
        selectedDateStr = dateStr;
        viewModel.setSelectedDate(dateStr);
        if (calendarDayAdapter != null) {
            calendarDayAdapter.setSelectedDateString(dateStr);
        }
        updateDayDetails(dateStr);
    }

    private void updateMonthHeader() {
        if (binding == null) return;
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("LLLL yyyy", new Locale("pl", "PL"));
        String monthTitle = monthYearFormat.format(displayedMonthCal.getTime());
        if (monthTitle != null && monthTitle.length() > 0) {
            monthTitle = Character.toUpperCase(monthTitle.charAt(0)) + monthTitle.substring(1);
        }
        binding.textMonthYear.setText(monthTitle);
    }

    /**
     * Builds and renders the 7-column calendar grid for the displayed month.
     * Colors days from today onwards as Green (Free), Yellow (Reserved), or Red (Blocked).
     */
    private void renderCalendarGrid() {
        if (calendarDayAdapter == null) return;

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        long todayStartMs = todayCal.getTimeInMillis();

        List<CalendarDay> days = buildMonthDays(
                displayedMonthCal,
                todayStartMs,
                selectedDateStr,
                currentBlockedPeriods,
                currentReservations,
                viewModel.getSelectedSpotId()
        );

        calendarDayAdapter.setDays(days);
    }

    /**
     * Constructs the full day list for the month with 7-column Monday-to-Sunday alignment.
     */
    private List<CalendarDay> buildMonthDays(Calendar monthCal,
                                             long todayStartMs,
                                             String selectedDate,
                                             List<BlockedPeriodEntity> blockedPeriods,
                                             List<ReservationEntity> reservations,
                                             String spotFilterId) {
        List<CalendarDay> result = new ArrayList<>();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Calendar iter = (Calendar) monthCal.clone();
        iter.set(Calendar.DAY_OF_MONTH, 1);
        iter.set(Calendar.HOUR_OF_DAY, 0);
        iter.set(Calendar.MINUTE, 0);
        iter.set(Calendar.SECOND, 0);
        iter.set(Calendar.MILLISECOND, 0);

        int targetMonth = iter.get(Calendar.MONTH);

        // Calculate leading blanks: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
        int firstDayCal = iter.get(Calendar.DAY_OF_WEEK);
        int leadingBlanks = (firstDayCal == Calendar.SUNDAY) ? 6 : (firstDayCal - 2);

        // Previous month padding
        Calendar prevMonthIter = (Calendar) iter.clone();
        prevMonthIter.add(Calendar.DAY_OF_MONTH, -leadingBlanks);
        for (int i = 0; i < leadingBlanks; i++) {
            Date d = prevMonthIter.getTime();
            String dStr = sdfDate.format(d);
            int dayNum = prevMonthIter.get(Calendar.DAY_OF_MONTH);
            result.add(new CalendarDay(d, dStr, dayNum, false, false, CalendarDay.Status.PAST));
            prevMonthIter.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Current month days
        while (iter.get(Calendar.MONTH) == targetMonth) {
            Date d = iter.getTime();
            String dStr = sdfDate.format(d);
            int dayNum = iter.get(Calendar.DAY_OF_MONTH);

            long dayStartMs = iter.getTimeInMillis();
            long dayEndMs = dayStartMs + 86400000L - 1L;
            boolean isToday = (dayStartMs == todayStartMs);

            CalendarDay.Status status;
            if (dayStartMs < todayStartMs) {
                status = CalendarDay.Status.PAST;
            } else {
                // Check if blocked
                boolean hasBlock = false;
                if (blockedPeriods != null) {
                    for (BlockedPeriodEntity b : blockedPeriods) {
                        if (b == null || b.isExpired()) continue;
                        if (!AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(spotFilterId)
                                && !spotFilterId.equals(b.getSpotId())) {
                            continue;
                        }
                        long bStart = b.getStartMillis();
                        long bEnd = b.getEndMillis();
                        if (bStart < dayEndMs && bEnd > dayStartMs) {
                            hasBlock = true;
                            break;
                        }
                    }
                }

                if (hasBlock) {
                    status = CalendarDay.Status.BLOCKED_RED;
                } else {
                    // Check if reserved
                    boolean hasRes = false;
                    if (reservations != null) {
                        for (ReservationEntity r : reservations) {
                            if (r == null) continue;
                            if (!AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(spotFilterId)
                                    && !spotFilterId.equals(r.getPlaceId())) {
                                continue;
                            }
                            String resStatus = r.getStatus();
                            if ("CANCELLED".equalsIgnoreCase(resStatus)
                                    || "REJECTED".equalsIgnoreCase(resStatus)
                                    || "ANULOWANA".equalsIgnoreCase(resStatus)
                                    || "ODRZUCONA".equalsIgnoreCase(resStatus)
                                    || "ZABLOKOWANA".equalsIgnoreCase(resStatus)
                                    || "BLOCKED".equalsIgnoreCase(resStatus)
                                    || (r.getId() != null && r.getId().startsWith("block_"))) {
                                continue;
                            }
                            if (r.getStartTimeMs() > 0 && r.getEndTimeMs() > 0) {
                                if (r.getStartTimeMs() < dayEndMs && r.getEndTimeMs() > dayStartMs) {
                                    hasRes = true;
                                    break;
                                }
                            } else if (dStr.equals(r.getDateString())) {
                                hasRes = true;
                                break;
                            }
                        }
                    }

                    if (hasRes) {
                        status = CalendarDay.Status.RESERVED_YELLOW;
                    } else {
                        status = CalendarDay.Status.FREE_GREEN;
                    }
                }
            }

            CalendarDay calDay = new CalendarDay(d, dStr, dayNum, true, isToday, status);
            if (dStr.equals(selectedDate)) {
                calDay.setSelected(true);
            }
            result.add(calDay);

            iter.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Trailing padding to complete 7 columns
        int remainder = result.size() % 7;
        if (remainder != 0) {
            int trailingBlanks = 7 - remainder;
            for (int i = 0; i < trailingBlanks; i++) {
                Date d = iter.getTime();
                String dStr = sdfDate.format(d);
                int dayNum = iter.get(Calendar.DAY_OF_MONTH);
                result.add(new CalendarDay(d, dStr, dayNum, false, false, CalendarDay.Status.PAST));
                iter.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        return result;
    }

    /**
     * Updates the Day Details card according to the selected date.
     * Displays access hours (Requirement 5), reservations with driver avatar/car info,
     * existing blocks with unblock action, and the blocking form (Requirement 4).
     */
    private void updateDayDetails(@Nullable String dateStr) {
        if (binding == null || dateStr == null || dateStr.trim().isEmpty()) return;

        Calendar targetCal = Calendar.getInstance();
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr);
            if (d != null) targetCal.setTime(d);
        } catch (Exception ignored) {}

        SimpleDateFormat plFullFormat = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("pl", "PL"));
        String dateDisplay = plFullFormat.format(targetCal.getTime());
        if (dateDisplay != null && dateDisplay.length() > 0) {
            dateDisplay = Character.toUpperCase(dateDisplay.charAt(0)) + dateDisplay.substring(1);
        }
        binding.textSelectedDateTitle.setText(dateDisplay);

        long dayStartMs;
        Calendar c = (Calendar) targetCal.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        dayStartMs = c.getTimeInMillis();
        long dayEndMs = dayStartMs + 86400000L - 1L;

        String spotFilter = viewModel.getSelectedSpotId();

        // 1. Evaluate Day Status Badge (Free / Reserved / Blocked)
        boolean hasBlock = false;
        List<BlockedPeriodEntity> dayBlocks = new ArrayList<>();
        for (BlockedPeriodEntity b : currentBlockedPeriods) {
            if (b == null || b.isExpired()) continue;
            if (!AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(spotFilter) && !spotFilter.equals(b.getSpotId())) continue;
            if (b.getStartMillis() < dayEndMs && b.getEndMillis() > dayStartMs) {
                hasBlock = true;
                dayBlocks.add(b);
            }
        }

        boolean hasRes = false;
        List<ReservationEntity> dayReservations = new ArrayList<>();
        for (ReservationEntity r : currentReservations) {
            if (r == null) continue;
            if (!AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(spotFilter) && !spotFilter.equals(r.getPlaceId())) continue;
            String resStatus = r.getStatus();
            if ("CANCELLED".equalsIgnoreCase(resStatus) || "REJECTED".equalsIgnoreCase(resStatus)
                    || "ANULOWANA".equalsIgnoreCase(resStatus) || "ODRZUCONA".equalsIgnoreCase(resStatus)
                    || "ZABLOKOWANA".equalsIgnoreCase(resStatus) || "BLOCKED".equalsIgnoreCase(resStatus)
                    || (r.getId() != null && r.getId().startsWith("block_"))) {
                continue;
            }
            boolean matchesDay = false;
            if (r.getStartTimeMs() > 0 && r.getEndTimeMs() > 0) {
                matchesDay = (r.getStartTimeMs() < dayEndMs && r.getEndTimeMs() > dayStartMs);
            } else if (dateStr.equals(r.getDateString())) {
                matchesDay = true;
            }
            if (matchesDay) {
                hasRes = true;
                dayReservations.add(r);
            }
        }

        if (hasBlock) {
            binding.badgeDayStatus.setText(R.string.status_day_blocked);
            binding.badgeDayStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_rose));
            binding.badgeDayStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_rose_700));
        } else if (hasRes) {
            binding.badgeDayStatus.setText(R.string.status_day_reserved);
            binding.badgeDayStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_amber));
            binding.badgeDayStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_amber_800));
        } else {
            binding.badgeDayStatus.setText(R.string.status_day_free);
            binding.badgeDayStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_emerald));
            binding.badgeDayStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_emerald_800));
        }

        // 2. Access Hours (Requirement 5)
        int calDay = targetCal.get(Calendar.DAY_OF_WEEK);
        int isoDay = (calDay == Calendar.SUNDAY) ? 7 : (calDay - 1);
        String dayName = getDayOfWeekName(isoDay);

        if (!AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(spotFilter)) {
            AvailabilityRuleEntity matchedRule = findRuleForSpotAndDay(spotFilter, isoDay);
            if (matchedRule != null) {
                String hours = (matchedRule.getStartTime().equals("00:00") && matchedRule.getEndTime().equals("23:59"))
                        ? getString(R.string.access_open_all_day)
                        : (matchedRule.getStartTime() + " - " + matchedRule.getEndTime());
                binding.textAccessHoursDetails.setText(String.format(Locale.getDefault(), "%s: %s", dayName, hours));
            } else {
                ParkingPlace p = getPlaceById(spotFilter);
                if (p != null && p.getAccessTime() != null && !p.getAccessTime().isEmpty()) {
                    String timeStr = p.getAccessTime();
                    if ("24/7".equalsIgnoreCase(timeStr) || "Dostęp 24/7".equalsIgnoreCase(timeStr)) {
                        binding.textAccessHoursDetails.setText(getString(R.string.access_24_7));
                    } else {
                        binding.textAccessHoursDetails.setText(String.format(Locale.getDefault(), "%s: %s", dayName, timeStr));
                    }
                } else {
                    binding.textAccessHoursDetails.setText(getString(R.string.access_24_7));
                }
            }
        } else {
            if (currentPlacesList.isEmpty()) {
                binding.textAccessHoursDetails.setText(getString(R.string.access_24_7));
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(getString(R.string.access_all_places_note)).append("\n");
                for (ParkingPlace p : currentPlacesList) {
                    sb.append("• ").append(p.getDisplayName()).append(": ");
                    AvailabilityRuleEntity matched = findRuleForSpotAndDay(p.getId(), isoDay);
                    if (matched != null) {
                        sb.append(matched.getStartTime()).append(" - ").append(matched.getEndTime());
                    } else if (p.getAccessTime() != null && !p.getAccessTime().isEmpty()) {
                        sb.append(p.getAccessTime());
                    } else {
                        sb.append("24/7");
                    }
                    sb.append("\n");
                }
                binding.textAccessHoursDetails.setText(sb.toString().trim());
            }
        }

        // 3. Render Reservations (Requirement 4)
        binding.containerDateReservations.removeAllViews();
        if (dayReservations.isEmpty()) {
            binding.textNoReservations.setVisibility(View.VISIBLE);
        } else {
            binding.textNoReservations.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (ReservationEntity r : dayReservations) {
                ItemCalendarReservationBinding itemBinding = ItemCalendarReservationBinding.inflate(
                        inflater, binding.containerDateReservations, false);

                itemBinding.textReservationTime.setText(r.getTimeRange() != null ? r.getTimeRange() : "");
                itemBinding.textPlaceName.setText(r.getPlaceName() != null ? r.getPlaceName() : getPlaceNameById(r.getPlaceId()));

                String driver = r.getUserName() != null && !r.getUserName().trim().isEmpty() ? r.getUserName().trim() : "Kierowca";
                itemBinding.textUserName.setText(driver);

                String car = r.getCarModel() != null && !r.getCarModel().trim().isEmpty() ? r.getCarModel().trim() : "Pojazd";
                String plate = r.getCarPlate() != null && !r.getCarPlate().trim().isEmpty() ? " • " + r.getCarPlate().trim() : "";
                itemBinding.textCarDetails.setText(car + plate);

                ImageLoader.getInstance(this).loadImage(
                        itemBinding.imgDriverAvatar,
                        r.getImageUri(),
                        R.drawable.ic_avatar
                );

                binding.containerDateReservations.addView(itemBinding.getRoot());
            }
        }

        // 4. Render Blocked Periods (Requirement 4)
        binding.containerDateBlocks.removeAllViews();
        if (dayBlocks.isEmpty()) {
            binding.textNoBlocks.setVisibility(View.VISIBLE);
        } else {
            binding.textNoBlocks.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (BlockedPeriodEntity b : dayBlocks) {
                ItemCalendarBlockBinding blockBinding = ItemCalendarBlockBinding.inflate(
                        inflater, binding.containerDateBlocks, false);

                blockBinding.textBlockTimeRange.setText(formatBlockHours(b));
                blockBinding.textBlockSpotName.setText(getPlaceNameById(b.getSpotId()));
                blockBinding.textBlockReason.setText(
                        (b.getReason() != null && !b.getReason().trim().isEmpty())
                                ? b.getReason().trim()
                                : "Blokada właściciela"
                );

                blockBinding.btnUnblockBlock.setOnClickListener(v -> showUnblockConfirmationDialog(b));

                binding.containerDateBlocks.addView(blockBinding.getRoot());
            }
        }
    }

    private void renderPlacesFilterChips() {
        if (binding == null) return;
        binding.layoutPlacesChips.removeAllViews();

        boolean isAllSelected = AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(viewModel.getSelectedSpotId());
        View allChip = createFilterChip(getString(R.string.tab_all_places), isAllSelected, v -> {
            viewModel.setSelectedSpotId(AvailabilityCalendarViewModel.ALL_SPOTS_ID);
            renderPlacesFilterChips();
            updateSpinnerPlacesForBlock();
            renderCalendarGrid();
            updateDayDetails(selectedDateStr);
        });
        binding.layoutPlacesChips.addView(allChip);

        for (ParkingPlace p : currentPlacesList) {
            boolean isSelected = p.getId().equals(viewModel.getSelectedSpotId());
            View chip = createFilterChip(p.getDisplayName(), isSelected, v -> {
                viewModel.setSelectedSpotId(p.getId());
                renderPlacesFilterChips();
                updateSpinnerPlacesForBlock();
                renderCalendarGrid();
                updateDayDetails(selectedDateStr);
            });
            binding.layoutPlacesChips.addView(chip);
        }
    }

    private View createFilterChip(String label, boolean isSelected, View.OnClickListener listener) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(13f);
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.setClickable(true);
        chip.setFocusable(true);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (14 * density);
        int padV = (int) (8 * density);
        chip.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int) (8 * density));
        chip.setLayoutParams(lp);

        if (isSelected) {
            chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_slate));
            chip.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_input_rounded));
            chip.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_700));
        }

        chip.setOnClickListener(listener);
        return chip;
    }

    private void updateSpinnerPlacesForBlock() {
        if (binding == null) return;

        boolean isAll = AvailabilityCalendarViewModel.ALL_SPOTS_ID.equals(viewModel.getSelectedSpotId());
        binding.layoutSelectBlockSpot.setVisibility(isAll ? View.VISIBLE : View.GONE);

        if (isAll) {
            List<String> names = new ArrayList<>();
            for (ParkingPlace p : currentPlacesList) {
                names.add(p.getDisplayName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    names
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerBlockSpot.setAdapter(adapter);
        }
    }

    private void showUnblockConfirmationDialog(@NonNull BlockedPeriodEntity block) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_unblock_title)
                .setMessage(R.string.confirm_unblock_message)
                .setPositiveButton(R.string.btn_unblock, (dialog, which) -> {
                    viewModel.unblockPeriod(block.getSpotId(), block.getId());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String formatBlockHours(BlockedPeriodEntity block) {
        long s = block.getStartMillis();
        long e = block.getEndMillis();
        if (s > 0 && e > 0) {
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return tf.format(new Date(s)) + " - " + tf.format(new Date(e));
        }
        return block.getStart() + " - " + block.getEnd();
    }

    private String getPlaceNameById(String id) {
        for (ParkingPlace p : currentPlacesList) {
            if (p.getId().equals(id)) {
                return p.getDisplayName();
            }
        }
        return getString(R.string.select_parking_place);
    }

    @Nullable
    private ParkingPlace getPlaceById(String id) {
        for (ParkingPlace p : currentPlacesList) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Nullable
    private AvailabilityRuleEntity findRuleForSpotAndDay(String spotId, int isoDay) {
        for (AvailabilityRuleEntity r : currentRules) {
            if (r.getSpotId().equals(spotId) && r.getDayOfWeek() == isoDay) {
                return r;
            }
        }
        return null;
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
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            cal.set(Calendar.HOUR_OF_DAY, defHour);
            cal.set(Calendar.MINUTE, defMin);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
