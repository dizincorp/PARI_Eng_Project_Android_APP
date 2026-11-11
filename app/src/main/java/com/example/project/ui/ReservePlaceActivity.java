package com.example.project.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.project.R;
import com.example.project.adapter.CalendarDayAdapter;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.databinding.ActivityReservePlaceBinding;
import com.example.project.databinding.DialogAddVehicleBinding;
import com.example.project.databinding.DialogDateTimePickerBinding;
import com.example.project.model.CalendarDay;
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
 * Activity for booking a parking place with multimodal reservation support
 * (Hourly, Daily/Weekly, Monthly tariffs), interactive month availability calendar,
 * dynamic access hours, real-time pricing breakdown via backend endpoints,
 * conflict prevention, and reactive reservation dispatch.
 */
public class ReservePlaceActivity extends BaseActivity {

    public static final String EXTRA_PLACE_ID = "EXTRA_PLACE_ID";

    private ActivityReservePlaceBinding binding;
    private ReservePlaceViewModel viewModel;
    private ParkingPlace currentPlace;
    private final List<Vehicle> currentVehiclesList = new ArrayList<>();

    // Booking Tariff State
    private String selectedBookingType = "HOURLY"; // "HOURLY", "DAILY", "MONTHLY"
    private int selectedDurationDays = 7;          // For DAILY tariff: default 1 week (7 days)
    private Integer selectedDurationMonths = 1;    // For MONTHLY tariff: default 1 month

    // Calendar state
    private final Calendar displayedMonthCal = Calendar.getInstance();
    private CalendarDayAdapter calendarDayAdapter;
    private String selectedCalendarDateStr;
    private final List<BlockedPeriodEntity> currentBlockedPeriods = new ArrayList<>();
    private final List<ReservationEntity> currentReservations = new ArrayList<>();
    private final List<AvailabilityRuleEntity> currentRules = new ArrayList<>();

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
        // If current actual time is outside opening hours, initialize to next opening time.
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        if (currentHour < allowedStartHour) {
            startCalendar.set(Calendar.HOUR_OF_DAY, allowedStartHour);
            startCalendar.set(Calendar.MINUTE, allowedStartMinute);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);
        } else if (currentHour >= allowedEndHour) {
            startCalendar.add(Calendar.DAY_OF_MONTH, 1);
            startCalendar.set(Calendar.HOUR_OF_DAY, allowedStartHour);
            startCalendar.set(Calendar.MINUTE, allowedStartMinute);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);
        } else {
            startCalendar.setTimeInMillis(now.getTimeInMillis());
        }

        endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + 7200000L); // Default +2 hours

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        selectedCalendarDateStr = sdfDate.format(startCalendar.getTime());
        displayedMonthCal.setTime(startCalendar.getTime());

        setupUI();
        setupTariffChips();
        setupCalendarView();
        setupClickListeners();
        observeViewModel();

        // Fetch spot availability rules and blocked periods directly from backend
        if (currentPlace != null && currentPlace.getId() != null) {
            viewModel.fetchSpotAvailability(currentPlace.getId());
        }
    }

    private void setupUI() {
        if (binding == null || currentPlace == null) return;

        com.example.project.utils.ImageLoader.getInstance(this).loadImage(
                binding.imgPlacePhoto,
                currentPlace.getImageUri(),
                currentPlace.getPhotoResId() != 0 ? currentPlace.getPhotoResId() : R.drawable.default_parking_spot
        );

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
                    chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.brand_slate_100));
                    chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.brand_slate_200));
                    chip.setChipStrokeWidth(1f);
                    chip.setChipCornerRadius(16f);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_700));
                    chip.setTextSize(12f);
                    binding.chipGroupPlaceAmenities.addView(chip);
                    hasAmenities = true;
                }
            }
        }
        binding.chipGroupPlaceAmenities.setVisibility(hasAmenities ? View.VISIBLE : View.GONE);

        // Description under amenities (if present)
        // Spot dimensions
        String dimensions = currentPlace.getFormattedDimensions();
        if (dimensions != null && !dimensions.isEmpty()) {
            binding.textPlaceDimensions.setText(String.format("📐 Wymiary: %s", dimensions));
            binding.textPlaceDimensions.setVisibility(View.VISIBLE);
        } else {
            binding.textPlaceDimensions.setVisibility(View.GONE);
        }

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
     * Sets up the multimodal Tariff chip group (Hourly, Daily/Weekly, Monthly)
     * and toggles duration packages for longer rental periods.
     */
    private void setupTariffChips() {
        if (binding == null) return;

        binding.chipGroupBookingType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_tariff_hourly) {
                selectedBookingType = "HOURLY";
                binding.scrollPackages.setVisibility(View.GONE);
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + 7200000L); // Default +2 hours
                updatePriceSummary();
            } else if (checkedId == R.id.chip_tariff_daily) {
                selectedBookingType = "DAILY";
                binding.scrollPackages.setVisibility(View.VISIBLE);
                setupDailyPackages();
            } else if (checkedId == R.id.chip_tariff_monthly) {
                selectedBookingType = "MONTHLY";
                binding.scrollPackages.setVisibility(View.VISIBLE);
                setupMonthlyPackages();
            }
        });
    }

    /**
     * Populates duration packages for DAILY tariff (1 day, 3 days, 1 week, 2 weeks).
     */
    private void setupDailyPackages() {
        if (binding == null) return;
        binding.chipGroupPackages.removeAllViews();

        int[] days = new int[]{1, 3, 7, 14};
        int[] labelRes = new int[]{R.string.package_1_day, R.string.package_3_days, R.string.package_1_week, R.string.package_2_weeks};

        for (int i = 0; i < days.length; i++) {
            final int dayCount = days[i];
            Chip chip = new Chip(this);
            chip.setText(labelRes[i]);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipCornerRadius(14f);
            chip.setTextSize(12f);
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.brand_slate_100));
            chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.brand_slate_300));
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.brand_slate_900));

            // Default select 1 week (7 days)
            if (dayCount == 7) {
                chip.setChecked(true);
                selectedDurationDays = 7;
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + selectedDurationDays * 86400000L);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedDurationDays = dayCount;
                    endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + selectedDurationDays * 86400000L);
                    updatePriceSummary();
                }
            });
            binding.chipGroupPackages.addView(chip);
        }

        updatePriceSummary();
    }

    /**
     * Populates duration packages for MONTHLY tariff (1 month, 3 months, 6 months, 1 year).
     */
    private void setupMonthlyPackages() {
        if (binding == null) return;
        binding.chipGroupPackages.removeAllViews();

        int[] months = new int[]{1, 3, 6, 12};
        int[] labelRes = new int[]{R.string.package_1_month, R.string.package_3_months, R.string.package_6_months, R.string.package_12_months};

        for (int i = 0; i < months.length; i++) {
            final int monthCount = months[i];
            Chip chip = new Chip(this);
            chip.setText(labelRes[i]);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipCornerRadius(14f);
            chip.setTextSize(12f);
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.brand_slate_100));
            chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.brand_slate_300));
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.brand_slate_900));

            // Default select 1 month
            if (monthCount == 1) {
                chip.setChecked(true);
                selectedDurationMonths = 1;
                Calendar c = (Calendar) startCalendar.clone();
                c.add(Calendar.MONTH, selectedDurationMonths);
                endCalendar.setTimeInMillis(c.getTimeInMillis());
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedDurationMonths = monthCount;
                    Calendar c = (Calendar) startCalendar.clone();
                    c.add(Calendar.MONTH, selectedDurationMonths);
                    endCalendar.setTimeInMillis(c.getTimeInMillis());
                    updatePriceSummary();
                }
            });
            binding.chipGroupPackages.addView(chip);
        }

        updatePriceSummary();
    }

    /**
     * Configures the Driver Availability Calendar grid and month navigation.
     */
    private void setupCalendarView() {
        if (binding == null) return;

        calendarDayAdapter = new CalendarDayAdapter();
        binding.recyclerCalendarDays.setLayoutManager(new GridLayoutManager(this, 7));
        binding.recyclerCalendarDays.setAdapter(calendarDayAdapter);

        calendarDayAdapter.setOnDayClickListener(this::onCalendarDayClicked);

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
            String todayStr = sdf.format(today.getTime());

            startCalendar.setTime(today.getTime());
            if ("DAILY".equals(selectedBookingType)) {
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + selectedDurationDays * 86400000L);
            } else if ("MONTHLY".equals(selectedBookingType)) {
                Calendar c = (Calendar) startCalendar.clone();
                c.add(Calendar.MONTH, selectedDurationMonths != null ? selectedDurationMonths : 1);
                endCalendar.setTimeInMillis(c.getTimeInMillis());
            } else {
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + 7200000L);
            }

            selectedCalendarDateStr = todayStr;
            updateMonthHeader();
            renderCalendarGrid();
            updateSelectedDayBanner(selectedCalendarDateStr);
            updatePriceSummary();
        });

        updateMonthHeader();
        renderCalendarGrid();
        updateSelectedDayBanner(selectedCalendarDateStr);
    }

    private void updateMonthHeader() {
        if (binding == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", new Locale("pl", "PL"));
        String monthName = sdf.format(displayedMonthCal.getTime());
        if (monthName.length() > 0) {
            monthName = monthName.substring(0, 1).toUpperCase(new Locale("pl", "PL")) + monthName.substring(1);
        }
        binding.textCurrentMonth.setText(monthName);
    }

    /**
     * Renders the 7-column calendar month grid with color-coded day availability.
     */
    private void renderCalendarGrid() {
        if (binding == null || calendarDayAdapter == null) return;

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        long todayStartMs = todayCal.getTimeInMillis();

        String spotId = currentPlace != null ? currentPlace.getId() : "";

        List<CalendarDay> days = buildMonthDays(
                displayedMonthCal,
                todayStartMs,
                selectedCalendarDateStr,
                currentBlockedPeriods,
                currentReservations,
                spotId
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
                        if (spotFilterId != null && !spotFilterId.isEmpty() && !spotFilterId.equals(b.getSpotId())) {
                            continue;
                        }
                        if (b.getStartMillis() < dayEndMs && b.getEndMillis() > dayStartMs) {
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
                            if (spotFilterId != null && !spotFilterId.isEmpty() && !spotFilterId.equals(r.getPlaceId())) {
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
     * Updates the day details banner with status badge and access hours for the chosen date.
     */
    private void updateSelectedDayBanner(@Nullable String dateStr) {
        if (binding == null || dateStr == null || dateStr.isEmpty()) return;

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar targetCal = Calendar.getInstance();
        try {
            Date d = sdfDate.parse(dateStr);
            if (d != null) {
                targetCal.setTime(d);
            }
        } catch (Exception ignored) {
        }

        targetCal.set(Calendar.HOUR_OF_DAY, 0);
        targetCal.set(Calendar.MINUTE, 0);
        targetCal.set(Calendar.SECOND, 0);
        targetCal.set(Calendar.MILLISECOND, 0);
        long dayStartMs = targetCal.getTimeInMillis();
        long dayEndMs = dayStartMs + 86400000L - 1L;

        String spotId = currentPlace != null ? currentPlace.getId() : "";

        // Status badge
        boolean hasBlock = false;
        for (BlockedPeriodEntity b : currentBlockedPeriods) {
            if (b == null || b.isExpired()) continue;
            if (spotId != null && !spotId.isEmpty() && !spotId.equals(b.getSpotId())) continue;
            if (b.getStartMillis() < dayEndMs && b.getEndMillis() > dayStartMs) {
                hasBlock = true;
                break;
            }
        }

        boolean hasRes = false;
        for (ReservationEntity r : currentReservations) {
            if (r == null) continue;
            if (spotId != null && !spotId.isEmpty() && !spotId.equals(r.getPlaceId())) continue;
            String resStatus = r.getStatus();
            if ("CANCELLED".equalsIgnoreCase(resStatus) || "REJECTED".equalsIgnoreCase(resStatus)
                    || "ANULOWANA".equalsIgnoreCase(resStatus) || "ODRZUCONA".equalsIgnoreCase(resStatus)
                    || "ZABLOKOWANA".equalsIgnoreCase(resStatus) || "BLOCKED".equalsIgnoreCase(resStatus)
                    || (r.getId() != null && r.getId().startsWith("block_"))) {
                continue;
            }
            if (r.getStartTimeMs() > 0 && r.getEndTimeMs() > 0) {
                if (r.getStartTimeMs() < dayEndMs && r.getEndTimeMs() > dayStartMs) {
                    hasRes = true;
                    break;
                }
            } else if (dateStr.equals(r.getDateString())) {
                hasRes = true;
                break;
            }
        }

        binding.textSelectedDayLabel.setText(String.format("Dzień: %s", dateStr));

        if (hasBlock) {
            binding.badgeSelectedDayStatus.setText("⛔ Blokada właściciela");
            binding.badgeSelectedDayStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_rose));
            binding.badgeSelectedDayStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_rose_700));
        } else if (hasRes) {
            binding.badgeSelectedDayStatus.setText("🚗 Częściowo zajęty");
            binding.badgeSelectedDayStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_amber));
            binding.badgeSelectedDayStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_amber_800));
        } else {
            binding.badgeSelectedDayStatus.setText("✅ Dostępny");
            binding.badgeSelectedDayStatus.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_emerald));
            binding.badgeSelectedDayStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_emerald_800));
        }

        // Access Hours / Godziny pracy
        int calDay = targetCal.get(Calendar.DAY_OF_WEEK);
        int isoDay = (calDay == Calendar.SUNDAY) ? 7 : (calDay - 1);
        String dayName = getDayOfWeekName(isoDay);

        AvailabilityRuleEntity matchedRule = findRuleForDay(isoDay);
        if (matchedRule != null) {
            if (matchedRule.getStartTime().equals("00:00") && matchedRule.getEndTime().equals("23:59")) {
                binding.textSelectedDayAccess.setText("🕒 Działa całodobowo (24/7)");
            } else {
                binding.textSelectedDayAccess.setText(String.format(Locale.getDefault(), "🕒 Działa tylko od %s do %s (%s)", matchedRule.getStartTime(), matchedRule.getEndTime(), dayName));
            }
        } else if (currentPlace != null && currentPlace.getAccessTime() != null && !currentPlace.getAccessTime().isEmpty()) {
            String acc = currentPlace.getAccessTime();
            if (acc.contains("-")) {
                String[] parts = acc.split("-");
                binding.textSelectedDayAccess.setText(String.format(Locale.getDefault(), "🕒 Działa tylko od %s do %s", parts[0].trim(), parts[1].trim()));
            } else {
                binding.textSelectedDayAccess.setText(String.format(Locale.getDefault(), "🕒 Działa: %s", acc));
            }
        } else {
            binding.textSelectedDayAccess.setText("🕒 Działa całodobowo (24/7)");
        }
    }

    private AvailabilityRuleEntity findRuleForDay(int isoDayOfWeek) {
        String spotId = currentPlace != null ? currentPlace.getId() : "";
        for (AvailabilityRuleEntity r : currentRules) {
            if (r == null) continue;
            if (spotId != null && !spotId.isEmpty() && !spotId.equals(r.getSpotId())) continue;
            if (r.getDayOfWeek() == isoDayOfWeek) {
                return r;
            }
        }
        return null;
    }

    private void updateAccessHoursForSelectedDay() {
        int calDay = startCalendar.get(Calendar.DAY_OF_WEEK);
        int isoDay = (calDay == Calendar.SUNDAY) ? 7 : (calDay - 1);
        AvailabilityRuleEntity rule = findRuleForDay(isoDay);
        if (rule != null) {
            allowedStartHour = rule.getStartHour();
            allowedStartMinute = rule.getStartMinute();
            allowedEndHour = rule.getEndHour();
            allowedEndMinute = rule.getEndMinute();
        } else if (currentPlace != null && currentPlace.getAccessTime() != null && !currentPlace.getAccessTime().isEmpty()) {
            allowedStartHour = AccessTimeParser.getStartHour(currentPlace.getAccessTime());
            allowedStartMinute = AccessTimeParser.getStartMinute(currentPlace.getAccessTime());
            allowedEndHour = AccessTimeParser.getEndHour(currentPlace.getAccessTime());
            allowedEndMinute = AccessTimeParser.getEndMinute(currentPlace.getAccessTime());
        } else {
            allowedStartHour = 0;
            allowedStartMinute = 0;
            allowedEndHour = 23;
            allowedEndMinute = 59;
        }
    }

    private String getDayOfWeekName(int isoDay) {
        switch (isoDay) {
            case 1: return "Poniedziałek";
            case 2: return "Wtorek";
            case 3: return "Środa";
            case 4: return "Czwartek";
            case 5: return "Piątek";
            case 6: return "Sobota";
            case 7: return "Niedziela";
            default: return "Dzień";
        }
    }

    /**
     * Handles calendar cell click by the driver.
     */
    private void onCalendarDayClicked(@NonNull CalendarDay day) {
        if (day.getStatus() == CalendarDay.Status.PAST) {
            Toast.makeText(this, "Nie można wybrać przeszłego terminu", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedCalendarDateStr = day.getDateString();
        calendarDayAdapter.setSelectedDateString(selectedCalendarDateStr);

        Calendar clicked = Calendar.getInstance();
        clicked.setTime(day.getDate());

        startCalendar.set(Calendar.YEAR, clicked.get(Calendar.YEAR));
        startCalendar.set(Calendar.MONTH, clicked.get(Calendar.MONTH));
        startCalendar.set(Calendar.DAY_OF_MONTH, clicked.get(Calendar.DAY_OF_MONTH));

        if ("DAILY".equals(selectedBookingType)) {
            endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + selectedDurationDays * 86400000L);
        } else if ("MONTHLY".equals(selectedBookingType)) {
            Calendar c = (Calendar) startCalendar.clone();
            c.add(Calendar.MONTH, selectedDurationMonths != null ? selectedDurationMonths : 1);
            endCalendar.setTimeInMillis(c.getTimeInMillis());
        } else {
            endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + 7200000L);
        }

        updateAccessHoursForSelectedDay();
        updateSelectedDayBanner(selectedCalendarDateStr);
        updatePriceSummary();

        if (day.getStatus() == CalendarDay.Status.BLOCKED_RED) {
            Toast.makeText(this, "Uwaga: ten dzień zawiera godziny zablokowane przez właściciela", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Updates the UI date-time labels, validates against calendar blocked periods,
     * displays collision warnings, recalculates preliminary total price summary
     * based on the active Tariff (Hourly, Daily, Monthly), and queries the backend
     * POST /api/reservations/quote endpoint for exact rate calculation.
     */
    private void updatePriceSummary() {
        if (binding == null || currentPlace == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        binding.textStartDateValue.setText(dateFormat.format(startCalendar.getTime()));
        binding.textStartTimeValue.setText(timeFormat.format(startCalendar.getTime()));

        binding.textEndDateValue.setText(dateFormat.format(endCalendar.getTime()));
        binding.textEndTimeValue.setText(timeFormat.format(endCalendar.getTime()));

        long reqStartMs = startCalendar.getTimeInMillis();
        long reqEndMs = endCalendar.getTimeInMillis();

        // Check if selected timeframe overlaps with any blocked period
        BlockedPeriodEntity conflict = viewModel.findConflictingBlockedPeriod(
                currentPlace.getId(),
                reqStartMs,
                reqEndMs
        );

        if (conflict != null) {
            // Pojęcie 1: Zablokowane przez właściciela
            String warningText = getString(R.string.error_spot_blocked_interval, conflict.getFormattedRange());
            binding.textReservationBlockedWarning.setText("⛔ Zablokowane przez właściciela: " + warningText);
            binding.textReservationBlockedWarning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_rose));
            binding.textReservationBlockedWarning.setTextColor(ContextCompat.getColor(this, R.color.brand_rose_700));
            binding.textReservationBlockedWarning.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_block, 0, 0, 0);
            binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
            binding.btnReserveAndPay.setEnabled(false);
        } else if (reqStartMs < System.currentTimeMillis() - 60000L) {
            // Termin w przeszłości
            binding.textReservationBlockedWarning.setText("⏱ Termin w przeszłości: Wybrany czas rozpoczęcia rezerwacji już minął. Wybierz datę i godzinę z przyszłości.");
            binding.textReservationBlockedWarning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_amber));
            binding.textReservationBlockedWarning.setTextColor(ContextCompat.getColor(this, R.color.brand_amber_800));
            binding.textReservationBlockedWarning.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock, 0, 0, 0);
            binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
            binding.btnReserveAndPay.setEnabled(false);
        } else if ("HOURLY".equals(selectedBookingType)) {
            int selectedStartMin = startCalendar.get(Calendar.HOUR_OF_DAY) * 60 + startCalendar.get(Calendar.MINUTE);
            int selectedEndMin = endCalendar.get(Calendar.HOUR_OF_DAY) * 60 + endCalendar.get(Calendar.MINUTE);
            int allowedStartMin = allowedStartHour * 60 + allowedStartMinute;
            int allowedEndMin = allowedEndHour * 60 + allowedEndMinute;

            if (selectedStartMin < allowedStartMin || selectedEndMin > allowedEndMin) {
                // Pojęcie 2: Godziny pracy (miejsce nie jest zablokowane, działa tylko w określonych godzinach)
                String openHoursMsg = String.format(Locale.getDefault(),
                        "🕒 Miejsce działa tylko od %02d:%02d do %02d:%02d. W wybranych godzinach parking jest zamknięty.",
                        allowedStartHour, allowedStartMinute, allowedEndHour, allowedEndMinute);
                binding.textReservationBlockedWarning.setText(openHoursMsg);
                binding.textReservationBlockedWarning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_amber));
                binding.textReservationBlockedWarning.setTextColor(ContextCompat.getColor(this, R.color.brand_amber_800));
                binding.textReservationBlockedWarning.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock, 0, 0, 0);
                binding.textReservationBlockedWarning.setVisibility(View.VISIBLE);
                binding.btnReserveAndPay.setEnabled(false);
            } else {
                binding.textReservationBlockedWarning.setVisibility(View.GONE);
                binding.btnReserveAndPay.setEnabled(false);
            }
        } else {
            binding.textReservationBlockedWarning.setVisibility(View.GONE);
            binding.btnReserveAndPay.setEnabled(false);
        }

        // Show pending calculation placeholder while fetching authoritative price quote from backend
        binding.textSummaryRateValue.setText("...");
        binding.textSummaryServiceFee.setText("...");
        binding.textSummaryTotalSum.setText("Kalkulacja...");
        binding.layoutSummaryDiscountRow.setVisibility(View.GONE);

        if ("DAILY".equals(selectedBookingType)) {
            long diffMs = Math.max(86400000L, reqEndMs - reqStartMs);
            long days = Math.max(1, Math.round((double) diffMs / 86400000.0));
            double dailyRate = currentPlace.getDailyPrice() > 0 ? currentPlace.getDailyPrice() : currentPlace.getPrice() * 7;

            String dayWord = (days == 1) ? "dzień" : (days == 7 ? "tydzień" : "dni");
            binding.textSummaryTariffValue.setText(String.format(Locale.getDefault(), "📅 Dni (%d %s)", days, dayWord));
            binding.textSummaryDuration.setText(String.format(Locale.getDefault(), "%d dni", days));
            binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka dzienna (%.2f PLN/d)", dailyRate));

            viewModel.fetchQuote(currentPlace.getId(), reqStartMs, reqEndMs, "DAILY", null);
        } else if ("MONTHLY".equals(selectedBookingType)) {
            int months = selectedDurationMonths != null ? selectedDurationMonths : 1;
            double dailyRef = currentPlace.getDailyPrice() > 0 ? currentPlace.getDailyPrice() : currentPlace.getPrice() * 7;
            double monthlyRate = currentPlace.getMonthlyPrice() > 0 ? currentPlace.getMonthlyPrice() : dailyRef * 20;

            String monthWord = (months == 1) ? "miesiąc" : (months < 5 ? "miesiące" : "miesięcy");
            binding.textSummaryTariffValue.setText(String.format(Locale.getDefault(), "🗓 Miesięczny (%d %s)", months, monthWord));
            binding.textSummaryDuration.setText(String.format(Locale.getDefault(), "%d mies.", months));
            binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka miesięczna (%.2f PLN/m)", monthlyRate));

            viewModel.fetchQuote(currentPlace.getId(), reqStartMs, reqEndMs, "MONTHLY", months);
        } else {
            // HOURLY
            double rate = currentPlace.getPrice() > 0 ? currentPlace.getPrice() : 10.0;
            long diffMs = Math.max(1800000L, reqEndMs - reqStartMs);
            double hours = Math.max(0.5, (double) diffMs / (1000.0 * 60.0 * 60.0));

            binding.textSummaryTariffValue.setText(R.string.tariff_hourly);
            binding.textSummaryDuration.setText(String.format(Locale.getDefault(), "%.1fh", hours));
            binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka (%.2f PLN/h)", rate));

            viewModel.fetchQuote(currentPlace.getId(), reqStartMs, reqEndMs, "HOURLY", null);
        }
    }

    /**
     * Updates the spot announcement card with formatted blocked period notices.
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

        if (startCalendar.getTimeInMillis() < System.currentTimeMillis() - 60000L) {
            new AlertDialog.Builder(this)
                    .setTitle("⏱ Termin w przeszłości")
                    .setMessage("Wybrany czas rozpoczęcia rezerwacji już minął. Wybierz datę i godzinę z przyszłości.")
                    .setPositiveButton(R.string.btn_ok, null)
                    .show();
            return false;
        }

        // Strict validation against calendar blocked periods and real-time status (Zablokowane przez właściciela)
        if (currentPlace != null) {
            BlockedPeriodEntity conflict = viewModel.findConflictingBlockedPeriod(
                    currentPlace.getId(),
                    startCalendar.getTimeInMillis(),
                    endCalendar.getTimeInMillis()
            );

            if (conflict != null) {
                String rangeStr = conflict.getFormattedRange();
                String reason = (conflict.getReason() != null && !conflict.getReason().isEmpty())
                        ? " Powód: " + conflict.getReason() : "";
                new AlertDialog.Builder(this)
                        .setTitle("⛔ Termin zablokowany przez właściciela")
                        .setMessage("Właściciel wyłączył to miejsce parkingowe z rezerwacji w wybranym terminie:\n\n" + rangeStr + (reason.isEmpty() ? "" : "\n" + reason) + "\n\nWybierz inny termin.")
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
                return false;
            }
        }

        // For Hourly tariff, check allowed access time range (Poza godzinami otwarcia)
        if ("HOURLY".equals(selectedBookingType)) {
            int selectedStartMin = startCalendar.get(Calendar.HOUR_OF_DAY) * 60 + startCalendar.get(Calendar.MINUTE);
            int selectedEndMin = endCalendar.get(Calendar.HOUR_OF_DAY) * 60 + endCalendar.get(Calendar.MINUTE);

            int allowedStartMin = allowedStartHour * 60 + allowedStartMinute;
            int allowedEndMin = allowedEndHour * 60 + allowedEndMinute;

            if (selectedStartMin < allowedStartMin || selectedEndMin > allowedEndMin) {
                String allowedStr = String.format(Locale.getDefault(), "%02d:%02d – %02d:%02d",
                        allowedStartHour, allowedStartMinute, allowedEndHour, allowedEndMinute);
                new AlertDialog.Builder(this)
                        .setTitle("🕒 Poza godzinami otwarcia")
                        .setMessage(String.format(Locale.getDefault(),
                                "To miejsce parkingowe jest czynne w godzinach %s.\n\nW wybranych przez Ciebie godzinach parking jest zamknięty. Nie można dokonać rezerwacji poza godzinami otwarcia miejsca.", allowedStr))
                        .setPositiveButton(R.string.btn_ok, null)
                        .show();
                return false;
            }
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

            // Dispatch multimodal reservation via ViewModel to ReservationRepository (POST /api/reservations)
            viewModel.submitReservation(
                    currentPlace.getId(),
                    vehicleId,
                    startCalendar.getTimeInMillis(),
                    endCalendar.getTimeInMillis(),
                    "pm_card_visa",
                    selectedBookingType,
                    "MONTHLY".equals(selectedBookingType) ? selectedDurationMonths : null
            );
        });
    }

    private void observeViewModel() {
        // Reactively observe parking place updates to dynamically update image and details
        if (currentPlace != null && currentPlace.getId() != null) {
            String targetId = currentPlace.getId();
            com.example.project.repository.AppRepository.getInstance().getPlacesLiveData().observe(this, places -> {
                if (places != null) {
                    for (com.example.project.model.ParkingPlace p : places) {
                        if (p != null && targetId.equals(p.getId())) {
                            currentPlace = p;
                            if (binding != null) {
                                com.example.project.utils.ImageLoader.getInstance(this).loadImage(
                                        binding.imgPlacePhoto,
                                        p.getImageUri(),
                                        p.getPhotoResId() != 0 ? p.getPhotoResId() : R.drawable.default_parking_spot
                                );
                            }
                            break;
                        }
                    }
                }
            });
        }

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

        // Observe blocked periods for this spot to update UI and calendar dynamically
        if (currentPlace != null && currentPlace.getId() != null) {
            String spotId = currentPlace.getId();

            viewModel.getBlockedPeriodsLiveData(spotId).observe(this, blocks -> {
                currentBlockedPeriods.clear();
                if (blocks != null) {
                    currentBlockedPeriods.addAll(blocks);
                }
                updateBlockedAnnouncement(blocks);
                renderCalendarGrid();
                updateSelectedDayBanner(selectedCalendarDateStr);
                updatePriceSummary();
            });

            viewModel.getSpotReservationsLiveData(spotId).observe(this, resList -> {
                currentReservations.clear();
                if (resList != null) {
                    currentReservations.addAll(resList);
                }
                renderCalendarGrid();
                updateSelectedDayBanner(selectedCalendarDateStr);
            });

            viewModel.getRulesLiveData(spotId).observe(this, rules -> {
                currentRules.clear();
                if (rules != null) {
                    currentRules.addAll(rules);
                }
                updateAccessHoursForSelectedDay();
                updateSelectedDayBanner(selectedCalendarDateStr);
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

                if (quote.getBookingType() != null) {
                    if ("MONTHLY".equalsIgnoreCase(quote.getBookingType())) {
                        int m = quote.getDurationMonths() != null ? quote.getDurationMonths() : (selectedDurationMonths != null ? selectedDurationMonths : 1);
                        String mWord = (m == 1) ? "miesiąc" : (m < 5 ? "miesiące" : "miesięcy");
                        binding.textSummaryTariffValue.setText(String.format(Locale.getDefault(), "🗓 Miesięczny (%d %s)", m, mWord));
                        if (quote.getMonthlyRate() != null) {
                            binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka miesięczna (%.2f %s/m)", quote.getMonthlyRate(), currency));
                        }
                    } else if ("DAILY".equalsIgnoreCase(quote.getBookingType())) {
                        int d = quote.getDurationDays() != null ? quote.getDurationDays() : selectedDurationDays;
                        String dWord = (d == 1) ? "dzień" : (d == 7 ? "tydzień" : "dni");
                        binding.textSummaryTariffValue.setText(String.format(Locale.getDefault(), "📅 Dni (%d %s)", d, dWord));
                        if (quote.getDailyRate() != null) {
                            binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka dzienna (%.2f %s/d)", quote.getDailyRate(), currency));
                        }
                    } else {
                        binding.textSummaryTariffValue.setText(R.string.tariff_hourly);
                        if (quote.getHourlyRate() != null) {
                            binding.textSummaryRateLabel.setText(String.format(Locale.getDefault(), "Stawka (%.2f %s/h)", quote.getHourlyRate(), currency));
                        }
                    }
                }

                if (quote.getSpotCost() != null) {
                    binding.textSummaryRateValue.setText(String.format(Locale.getDefault(), "%.2f %s", quote.getSpotCost(), currency));
                }
                if (quote.getDiscountAmount() != null && quote.getDiscountAmount() > 0) {
                    binding.layoutSummaryDiscountRow.setVisibility(View.VISIBLE);
                    double p = quote.getDiscountPercent() != null ? quote.getDiscountPercent() : 0.0;
                    binding.textSummaryDiscountValue.setText(String.format(Locale.getDefault(), "-%.0f%% (-%.2f %s)", p, quote.getDiscountAmount(), currency));
                } else {
                    binding.layoutSummaryDiscountRow.setVisibility(View.GONE);
                }
                if (quote.getServiceFee() != null) {
                    binding.textSummaryServiceFee.setText(String.format(Locale.getDefault(), "%.2f %s", quote.getServiceFee(), currency));
                }
                if (quote.getTotal() != null) {
                    binding.textSummaryTotalSum.setText(String.format(Locale.getDefault(), "%.2f %s", quote.getTotal(), currency));
                }

                // Enable reserve button only if no active block warning is present
                boolean hasWarning = binding.textReservationBlockedWarning.getVisibility() == View.VISIBLE;
                binding.btnReserveAndPay.setEnabled(!hasWarning);
            } else if (resource.isLoading()) {
                binding.textSummaryRateValue.setText("...");
                binding.textSummaryServiceFee.setText("...");
                binding.textSummaryTotalSum.setText("Kalkulacja...");
                binding.btnReserveAndPay.setEnabled(false);
            } else if (resource.isError() && resource.getMessage() != null && !resource.getMessage().isEmpty()) {
                binding.textSummaryRateValue.setText("—");
                binding.textSummaryServiceFee.setText("—");
                binding.textSummaryTotalSum.setText("— PLN");
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
                } else if (conflictMessage.contains("🕒") || conflictMessage.toLowerCase(Locale.ROOT).contains("godzin") || conflictMessage.toLowerCase(Locale.ROOT).contains("otwarcia") || conflictMessage.toLowerCase(Locale.ROOT).contains("zamknięt")) {
                    titleResId = R.string.title_out_of_hours;
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
                dialogBinding.radioStartMode.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));

                dialogBinding.radioEndMode.setChecked(false);
                dialogBinding.radioEndMode.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_600));
                dialogBinding.radioEndMode.setBackgroundTintList(null);

                dialogBinding.textActiveTargetLabel.setText(R.string.label_start_time);
                loadIntoPickers.execute(tempStart);
            } else {
                dialogBinding.radioEndMode.setChecked(true);
                dialogBinding.radioEndMode.setTextColor(ContextCompat.getColor(this, R.color.white));
                dialogBinding.radioEndMode.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));

                dialogBinding.radioStartMode.setChecked(false);
                dialogBinding.radioStartMode.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_600));
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
                dialogBinding.radioModeWheels.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));

                dialogBinding.radioModeText.setChecked(false);
                dialogBinding.radioModeText.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_600));
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
                dialogBinding.radioModeText.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));

                dialogBinding.radioModeWheels.setChecked(false);
                dialogBinding.radioModeWheels.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_600));
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

            if (tempStart.getTimeInMillis() < System.currentTimeMillis() - 60000L) {
                Toast.makeText(this, "⏱ Wybrany czas rozpoczęcia jest w przeszłości!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentPlace != null) {
                BlockedPeriodEntity blockConflict = viewModel.findConflictingBlockedPeriod(
                        currentPlace.getId(),
                        tempStart.getTimeInMillis(),
                        tempEnd.getTimeInMillis()
                );
                if (blockConflict != null) {
                    String reason = (blockConflict.getReason() != null && !blockConflict.getReason().isEmpty())
                            ? " (" + blockConflict.getReason() + ")" : "";
                    Toast.makeText(this, "⛔ Wybrany termin jest zablokowany przez właściciela: " + blockConflict.getFormattedRange() + reason, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            if ("HOURLY".equals(selectedBookingType)) {
                int selectedStartMin = tempStart.get(Calendar.HOUR_OF_DAY) * 60 + tempStart.get(Calendar.MINUTE);
                int selectedEndMin = tempEnd.get(Calendar.HOUR_OF_DAY) * 60 + tempEnd.get(Calendar.MINUTE);
                int allowedStartMin = allowedStartHour * 60 + allowedStartMinute;
                int allowedEndMin = allowedEndHour * 60 + allowedEndMinute;

                if (selectedStartMin < allowedStartMin || selectedEndMin > allowedEndMin) {
                    String msg = String.format(Locale.getDefault(),
                            "🕒 Miejsce działa tylko od %02d:%02d do %02d:%02d (w pozostałych godzinach jest zamknięte)!",
                            allowedStartHour, allowedStartMinute, allowedEndHour, allowedEndMinute);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            startCalendar.setTimeInMillis(tempStart.getTimeInMillis());
            endCalendar.setTimeInMillis(tempEnd.getTimeInMillis());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            selectedCalendarDateStr = sdf.format(startCalendar.getTime());
            displayedMonthCal.setTime(startCalendar.getTime());
            updateMonthHeader();
            renderCalendarGrid();
            updateAccessHoursForSelectedDay();
            updateSelectedDayBanner(selectedCalendarDateStr);

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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save_vehicle, (d, which) -> {
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
                .create();

        dialogBinding.btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
