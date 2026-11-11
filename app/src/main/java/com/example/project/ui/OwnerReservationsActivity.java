package com.example.project.ui;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project.R;
import com.example.project.adapter.OwnerReservationsAdapter;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.databinding.ActivityOwnerReservationsBinding;
import com.example.project.databinding.DialogOwnerReservationsFiltersBinding;
import com.example.project.utils.ReservationFilterHelper;
import com.example.project.viewmodel.OwnerReservationsViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Screen presenting incoming and active reservations for the parking spot owner.
 * Allows confirming or rejecting reservation requests (F03).
 */
public class OwnerReservationsActivity extends BaseActivity implements OwnerReservationsAdapter.OnReservationActionListener {

    private ActivityOwnerReservationsBinding binding;
    private OwnerReservationsViewModel viewModel;
    private OwnerReservationsAdapter adapter;

    private final List<ReservationEntity> allReservations = new ArrayList<>();
    private final ReservationFilterHelper.OwnerCriteria ownerCriteria = new ReservationFilterHelper.OwnerCriteria();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityOwnerReservationsBinding.inflate(getLayoutInflater());
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

        viewModel = new ViewModelProvider(this).get(OwnerReservationsViewModel.class);

        setupRecyclerView();
        setupFilters();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new OwnerReservationsAdapter(this);
        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerReservations.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_pending)) {
                ownerCriteria.setStatusFilter("PENDING");
            } else if (checkedIds.contains(R.id.chip_confirmed)) {
                ownerCriteria.setStatusFilter("CONFIRMED");
            } else if (checkedIds.contains(R.id.chip_in_progress)) {
                ownerCriteria.setStatusFilter("IN_PROGRESS");
            } else if (checkedIds.contains(R.id.chip_completed)) {
                ownerCriteria.setStatusFilter("COMPLETED");
            } else if (checkedIds.contains(R.id.chip_cancelled_rejected)) {
                ownerCriteria.setStatusFilter("CANCELLED_REJECTED");
            } else if (checkedIds.contains(R.id.chip_expired_noshow)) {
                ownerCriteria.setStatusFilter("EXPIRED_NO_SHOW");
            } else {
                ownerCriteria.setStatusFilter("ALL");
            }
            applyFilter();
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> {
            com.example.project.utils.ViewAnimationUtils.playBounceAnimation(v, null);
            finish();
        });

        binding.btnOwnerFilters.setOnClickListener(v -> showOwnerFiltersBottomSheet());

        binding.btnResetOwnerFilters.setOnClickListener(v -> {
            ownerCriteria.resetAll();
            binding.chipAll.setChecked(true);
            applyFilter();
        });

        binding.btnRefresh.setOnClickListener(v -> {
            com.example.project.utils.ViewAnimationUtils.playRotateAnimation(v, () -> {
                if (viewModel != null) {
                    viewModel.refreshIncomingReservations();
                }
            });
            Toast.makeText(this, R.string.msg_refreshing_reservations, Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getIncomingReservationsLiveData().observe(this, list -> {
            allReservations.clear();
            if (list != null) {
                allReservations.addAll(list);
            }
            applyFilter();
        });

        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getToastMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessageLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        List<ReservationEntity> filtered = ReservationFilterHelper.filterAndSortOwner(
                allReservations, ownerCriteria, System.currentTimeMillis());

        adapter.setReservations(filtered);

        int extraCount = ownerCriteria.countExtraFilters();
        if (extraCount > 0) {
            binding.badgeOwnerFilterCount.setText(String.valueOf(extraCount));
            binding.badgeOwnerFilterCount.setVisibility(View.VISIBLE);
        } else {
            binding.badgeOwnerFilterCount.setVisibility(View.GONE);
        }

        if (allReservations.isEmpty()) {
            binding.textSubtitle.setText(R.string.subtitle_owner_reservations);
        } else {
            binding.textSubtitle.setText(getString(
                    R.string.owner_filter_summary_count, filtered.size(), allReservations.size()));
        }

        if (filtered.isEmpty()) {
            binding.recyclerReservations.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            boolean hasAnyActiveFilter = !allReservations.isEmpty()
                    && (extraCount > 0 || !"ALL".equals(ownerCriteria.getStatusFilter()));
            binding.textEmptyTitle.setText(hasAnyActiveFilter
                    ? R.string.owner_filter_empty : R.string.msg_no_owner_reservations);
            binding.btnResetOwnerFilters.setVisibility(hasAnyActiveFilter ? View.VISIBLE : View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerReservations.setVisibility(View.VISIBLE);
        }
    }

    private void showOwnerFiltersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogOwnerReservationsFiltersBinding fb = DialogOwnerReservationsFiltersBinding.inflate(LayoutInflater.from(this));
        dialog.setContentView(fb.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        final Long[] tempFromMs = new Long[]{ownerCriteria.getCustomFromMs()};
        final Long[] tempToMs = new Long[]{ownerCriteria.getCustomToMs()};

        // Collect distinct owner spots represented in incoming reservations
        Map<String, String> spotOptions = new LinkedHashMap<>();
        for (ReservationEntity e : allReservations) {
            if (e == null) continue;
            String spotId = e.getPlaceId() != null ? e.getPlaceId().trim() : "";
            String spotName = e.getPlaceName() != null ? e.getPlaceName().trim() : "";
            String key = !spotId.isEmpty() ? spotId : spotName;
            if (key.isEmpty()) continue;
            if (!spotOptions.containsKey(key)) {
                String label = !spotName.isEmpty() ? spotName
                        : (e.getStreet() != null && !e.getStreet().trim().isEmpty() ? e.getStreet().trim() : key);
                spotOptions.put(key, label);
            }
        }

        final int allSpotChipId = View.generateViewId();
        final Map<Integer, String> chipIdToSpotKey = new LinkedHashMap<>();

        ColorStateList bgColors = ContextCompat.getColorStateList(this, R.color.chip_filter_bg_selector);
        ColorStateList textColors = ContextCompat.getColorStateList(this, R.color.chip_filter_text_selector);
        ColorStateList strokeColors = ContextCompat.getColorStateList(this, R.color.chip_filter_stroke_selector);
        float strokePx = getResources().getDisplayMetrics().density;

        Chip allSpotChip = buildChoiceChip(allSpotChipId, getString(R.string.owner_filter_all_spots),
                bgColors, textColors, strokeColors, strokePx);
        fb.chipGroupOwnerSpots.addView(allSpotChip);
        chipIdToSpotKey.put(allSpotChipId, null);

        for (Map.Entry<String, String> entry : spotOptions.entrySet()) {
            int chipId = View.generateViewId();
            Chip c = buildChoiceChip(chipId, entry.getValue(), bgColors, textColors, strokeColors, strokePx);
            fb.chipGroupOwnerSpots.addView(c);
            chipIdToSpotKey.put(chipId, entry.getKey());
        }

        Runnable syncUi = () -> {
            fb.editOwnerQuery.setText(ownerCriteria.getQuery());

            switch (ownerCriteria.getSortBy()) {
                case START_ASC:
                    fb.chipOsortStartAsc.setChecked(true);
                    break;
                case PRICE_DESC:
                    fb.chipOsortPriceDesc.setChecked(true);
                    break;
                case PRICE_ASC:
                    fb.chipOsortPriceAsc.setChecked(true);
                    break;
                case START_DESC:
                default:
                    fb.chipOsortStartDesc.setChecked(true);
                    break;
            }

            String activeSpotKey = ownerCriteria.getSpotKeyFilter();
            int matchedChipId = allSpotChipId;
            if (activeSpotKey != null) {
                for (Map.Entry<Integer, String> entry : chipIdToSpotKey.entrySet()) {
                    if (activeSpotKey.equals(entry.getValue())) {
                        matchedChipId = entry.getKey();
                        break;
                    }
                }
            }
            fb.chipGroupOwnerSpots.check(matchedChipId);

            switch (ownerCriteria.getDatePreset()) {
                case TODAY:
                    fb.chipOdateToday.setChecked(true);
                    fb.layoutOwnerCustomDates.setVisibility(View.GONE);
                    break;
                case UPCOMING:
                    fb.chipOdateUpcoming.setChecked(true);
                    fb.layoutOwnerCustomDates.setVisibility(View.GONE);
                    break;
                case LAST_7_DAYS:
                    fb.chipOdate7d.setChecked(true);
                    fb.layoutOwnerCustomDates.setVisibility(View.GONE);
                    break;
                case LAST_30_DAYS:
                    fb.chipOdate30d.setChecked(true);
                    fb.layoutOwnerCustomDates.setVisibility(View.GONE);
                    break;
                case CUSTOM:
                    fb.chipOdateCustom.setChecked(true);
                    fb.layoutOwnerCustomDates.setVisibility(View.VISIBLE);
                    break;
                case ALL:
                default:
                    fb.chipOdateAll.setChecked(true);
                    fb.layoutOwnerCustomDates.setVisibility(View.GONE);
                    break;
            }

            tempFromMs[0] = ownerCriteria.getCustomFromMs();
            tempToMs[0] = ownerCriteria.getCustomToMs();
            fb.btnOwnerDateFrom.setText(tempFromMs[0] != null
                    ? getString(R.string.history_filter_date_from) + ": " + dateFmt.format(new Date(tempFromMs[0]))
                    : getString(R.string.history_filter_date_from));
            fb.btnOwnerDateTo.setText(tempToMs[0] != null
                    ? getString(R.string.history_filter_date_to) + ": " + dateFmt.format(new Date(tempToMs[0]))
                    : getString(R.string.history_filter_date_to));

            fb.editOwnerPriceMin.setText(ownerCriteria.getMinPrice() != null
                    ? formatCompactPrice(ownerCriteria.getMinPrice()) : "");
            fb.editOwnerPriceMax.setText(ownerCriteria.getMaxPrice() != null
                    ? formatCompactPrice(ownerCriteria.getMaxPrice()) : "");
        };
        syncUi.run();

        fb.chipGroupOwnerDate.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean custom = checkedIds.contains(R.id.chip_odate_custom);
            fb.layoutOwnerCustomDates.setVisibility(custom ? View.VISIBLE : View.GONE);
        });

        fb.btnOwnerDateFrom.setOnClickListener(v -> showDatePicker(tempFromMs[0], pickedMs -> {
            tempFromMs[0] = pickedMs;
            fb.btnOwnerDateFrom.setText(getString(R.string.history_filter_date_from) + ": " + dateFmt.format(new Date(pickedMs)));
        }));

        fb.btnOwnerDateTo.setOnClickListener(v -> showDatePicker(tempToMs[0], pickedMs -> {
            tempToMs[0] = pickedMs;
            fb.btnOwnerDateTo.setText(getString(R.string.history_filter_date_to) + ": " + dateFmt.format(new Date(pickedMs)));
        }));

        fb.btnCloseOwnerFilters.setOnClickListener(v -> dialog.dismiss());

        fb.btnResetOwnerDialogFilters.setOnClickListener(v -> {
            ownerCriteria.resetKeepStatus();
            syncUi.run();
        });

        fb.btnApplyOwnerDialogFilters.setOnClickListener(v -> {
            ownerCriteria.setQuery(fb.editOwnerQuery.getText().toString());

            int sortId = fb.chipGroupOwnerSort.getCheckedChipId();
            if (sortId == R.id.chip_osort_start_asc) ownerCriteria.setSortBy(ReservationFilterHelper.OwnerSort.START_ASC);
            else if (sortId == R.id.chip_osort_price_desc) ownerCriteria.setSortBy(ReservationFilterHelper.OwnerSort.PRICE_DESC);
            else if (sortId == R.id.chip_osort_price_asc) ownerCriteria.setSortBy(ReservationFilterHelper.OwnerSort.PRICE_ASC);
            else ownerCriteria.setSortBy(ReservationFilterHelper.OwnerSort.START_DESC);

            int checkedSpotId = fb.chipGroupOwnerSpots.getCheckedChipId();
            ownerCriteria.setSpotKeyFilter(chipIdToSpotKey.get(checkedSpotId));

            int dateId = fb.chipGroupOwnerDate.getCheckedChipId();
            if (dateId == R.id.chip_odate_today) ownerCriteria.setDatePreset(ReservationFilterHelper.DatePreset.TODAY);
            else if (dateId == R.id.chip_odate_upcoming) ownerCriteria.setDatePreset(ReservationFilterHelper.DatePreset.UPCOMING);
            else if (dateId == R.id.chip_odate_7d) ownerCriteria.setDatePreset(ReservationFilterHelper.DatePreset.LAST_7_DAYS);
            else if (dateId == R.id.chip_odate_30d) ownerCriteria.setDatePreset(ReservationFilterHelper.DatePreset.LAST_30_DAYS);
            else if (dateId == R.id.chip_odate_custom) ownerCriteria.setDatePreset(ReservationFilterHelper.DatePreset.CUSTOM);
            else ownerCriteria.setDatePreset(ReservationFilterHelper.DatePreset.ALL);

            ownerCriteria.setCustomFromMs(tempFromMs[0]);
            ownerCriteria.setCustomToMs(tempToMs[0]);

            ownerCriteria.setMinPrice(parseOptionalDouble(fb.editOwnerPriceMin.getText().toString()));
            ownerCriteria.setMaxPrice(parseOptionalDouble(fb.editOwnerPriceMax.getText().toString()));

            dialog.dismiss();
            applyFilter();
        });

        dialog.show();
    }

    @NonNull
    private Chip buildChoiceChip(int id,
                                 @NonNull String label,
                                 @Nullable ColorStateList bgColors,
                                 @Nullable ColorStateList textColors,
                                 @Nullable ColorStateList strokeColors,
                                 float strokePx) {
        Chip chip = new Chip(this);
        chip.setId(id);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);
        if (bgColors != null) chip.setChipBackgroundColor(bgColors);
        if (textColors != null) chip.setTextColor(textColors);
        if (strokeColors != null) {
            chip.setChipStrokeColor(strokeColors);
            chip.setChipStrokeWidth(strokePx);
        }
        return chip;
    }

    private interface OnDateSelected {
        void onPicked(long epochMs);
    }

    private void showDatePicker(@Nullable Long initialMs, @NonNull OnDateSelected callback) {
        Calendar cal = Calendar.getInstance();
        if (initialMs != null && initialMs > 0) {
            cal.setTimeInMillis(initialMs);
        }
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth, 12, 0, 0);
                    callback.onPicked(picked.getTimeInMillis());
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    @Nullable
    private Double parseOptionalDouble(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            double val = Double.parseDouble(raw.trim().replace(',', '.'));
            return val >= 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    private String formatCompactPrice(double val) {
        if (Math.abs(val - Math.round(val)) < 0.01) {
            return String.valueOf(Math.round(val));
        }
        return String.format(Locale.US, "%.2f", val);
    }

    @Override
    public void onAccept(@NonNull ReservationEntity reservation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_accept_title)
                .setMessage(R.string.dialog_accept_message)
                .setPositiveButton(R.string.btn_accept_reservation, (dialog, which) -> {
                    viewModel.acceptReservation(reservation.getId());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public void onReject(@NonNull ReservationEntity reservation) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reject_reservation, null);
        EditText inputReason = dialogView.findViewById(R.id.edit_reject_reason);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_reject_title)
                .setMessage(R.string.dialog_reject_message)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_reject_reservation, (dialog, which) -> {
                    String reason = inputReason != null && inputReason.getText() != null
                            ? inputReason.getText().toString().trim() : "";
                    viewModel.rejectReservation(reservation.getId(), reason.isEmpty() ? null : reason);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshIncomingReservations();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
