package com.example.project.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project.R;
import com.example.project.adapter.ReservationsAdapter;
import com.example.project.databinding.ActivityReservationsBinding;
import com.example.project.databinding.DialogActiveReservationDetailsBinding;
import com.example.project.databinding.DialogHistoryFiltersBinding;
import com.example.project.databinding.DialogRatingBinding;
import com.example.project.model.Reservation;
import com.example.project.network.Resource;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReviewRepository;
import com.example.project.utils.NotificationHelper;
import com.example.project.utils.ReservationFilterHelper;
import com.example.project.utils.ReservationLifecycleManager;
import com.example.project.viewmodel.ReservationsViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for viewing user reservations pulled from the REST backend (GET /api/reservations/incoming
 * and GET /api/reservations) and displayed in an interactive RecyclerView using MVVM architecture.
 * Manages full parking lifecycle actions including Check-in, Check-out, Cancellation, and Reviews.
 */
public class ReservationsActivity extends BaseActivity implements ReservationsAdapter.OnReservationItemClickListener {

    private ActivityReservationsBinding binding;
    private ReservationsViewModel viewModel;
    private ReservationsAdapter adapter;

    private boolean isHistoryTab = false;
    private boolean suppressQuickChipEvents = false;
    private final List<Reservation> activeReservationsList = new ArrayList<>();
    private final List<Reservation> historyReservationsList = new ArrayList<>();
    private final ReservationFilterHelper.HistoryCriteria historyCriteria = new ReservationFilterHelper.HistoryCriteria();

    private static final int REQ_LIFECYCLE_PERMISSIONS = 2101;
    private boolean hasRequestedLifecyclePermissions = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                viewModel.onTimeTick();
            }
            if (adapter != null) {
                adapter.updateCountdowns();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityReservationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_reservations);

        viewModel = new ViewModelProvider(this).get(ReservationsViewModel.class);

        setupRecyclerView();
        setupTabs();
        setupHistoryFilters();
        setupClickListeners();
        observeViewModel();

        // Initial fetch of reservations from REST API
        viewModel.refreshReservations();
        requestLifecyclePermissionsIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
        ReservationLifecycleManager.getInstance(this).evaluateAsync();
        // Automatically sync fresh reservations from REST backend
        if (viewModel != null) {
            viewModel.refreshReservations();
        }
    }

    private void requestLifecyclePermissionsIfNeeded() {
        if (hasRequestedLifecyclePermissions) return;
        hasRequestedLifecyclePermissions = true;

        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_LIFECYCLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LIFECYCLE_PERMISSIONS) {
            ReservationLifecycleManager.getInstance(this).startForegroundMonitoring();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    private void setupRecyclerView() {
        if (binding == null) return;

        adapter = new ReservationsAdapter(this);
        binding.recyclerReservations.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerReservations.setAdapter(adapter);
    }

    private void setupTabs() {
        if (binding == null) return;

        binding.btnTabActive.setOnClickListener(v -> selectTab(false));
        binding.btnTabHistory.setOnClickListener(v -> selectTab(true));

        selectTab(false);
    }

    private void selectTab(boolean isHistory) {
        isHistoryTab = isHistory;
        if (binding == null || adapter == null) return;

        adapter.setIsHistoryTab(isHistory);

        if (isHistory) {
            binding.btnTabActive.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_100));
            binding.btnTabActive.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_600));

            binding.btnTabHistory.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));
            binding.btnTabHistory.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.layoutHistoryFilters.setVisibility(View.VISIBLE);
            renderFilteredHistory();
        } else {
            binding.btnTabActive.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_950));
            binding.btnTabActive.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.btnTabHistory.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_slate_100));
            binding.btnTabHistory.setTextColor(ContextCompat.getColor(this, R.color.brand_slate_600));

            binding.layoutHistoryFilters.setVisibility(View.GONE);
            renderList(activeReservationsList);
        }
    }

    private void renderFilteredHistory() {
        List<Reservation> filtered = ReservationFilterHelper.filterAndSortHistory(
                historyReservationsList, historyCriteria, System.currentTimeMillis());
        updateHistoryFilterBadge();
        renderList(filtered);
    }

    private void renderList(List<Reservation> list) {
        if (binding == null || adapter == null) return;

        adapter.setReservations(list);

        if (list == null || list.isEmpty()) {
            binding.recyclerReservations.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);

            if (isHistoryTab) {
                binding.textEmptyTitle.setText(R.string.tab_history);
                boolean hasFilters = historyCriteria.countActiveFilters() > 0;
                binding.textEmptyMessage.setText(hasFilters
                        ? R.string.history_filter_empty : R.string.msg_no_history_reservation);
                binding.btnSearchOnMap.setVisibility(View.GONE);
                binding.btnResetHistoryFilters.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
            } else {
                binding.textEmptyTitle.setText(R.string.title_reservations);
                binding.textEmptyMessage.setText(R.string.msg_no_active_reservation);
                binding.btnSearchOnMap.setVisibility(View.VISIBLE);
                binding.btnResetHistoryFilters.setVisibility(View.GONE);
            }
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerReservations.setVisibility(View.VISIBLE);
        }
    }

    private void setupHistoryFilters() {
        if (binding == null) return;

        binding.chipGroupHistoryQuick.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressQuickChipEvents || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            historyCriteria.reset();
            if (id == R.id.chip_hquick_completed) {
                historyCriteria.setStatusFilter("COMPLETED");
            } else if (id == R.id.chip_hquick_7d) {
                historyCriteria.setEndDatePreset(ReservationFilterHelper.DatePreset.LAST_7_DAYS);
            } else if (id == R.id.chip_hquick_short) {
                historyCriteria.setDurationPreset(ReservationFilterHelper.DurationPreset.SHORT_LE_2H);
            } else if (id == R.id.chip_hquick_price_desc) {
                historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.PRICE_DESC);
            }
            if (isHistoryTab) {
                renderFilteredHistory();
            } else {
                updateHistoryFilterBadge();
            }
        });

        binding.btnHistoryFilters.setOnClickListener(v -> showHistoryFiltersBottomSheet());

        binding.btnResetHistoryFilters.setOnClickListener(v -> {
            historyCriteria.reset();
            syncQuickHistoryChips();
            if (isHistoryTab) {
                renderFilteredHistory();
            }
        });
    }

    private void syncQuickHistoryChips() {
        if (binding == null) return;
        suppressQuickChipEvents = true;
        int count = historyCriteria.countActiveFilters();
        if (count == 0) {
            binding.chipHquickAll.setChecked(true);
        } else if (count == 1 && "COMPLETED".equals(historyCriteria.getStatusFilter())) {
            binding.chipHquickCompleted.setChecked(true);
        } else if (count == 1 && historyCriteria.getEndDatePreset() == ReservationFilterHelper.DatePreset.LAST_7_DAYS) {
            binding.chipHquick7d.setChecked(true);
        } else if (count == 1 && historyCriteria.getDurationPreset() == ReservationFilterHelper.DurationPreset.SHORT_LE_2H) {
            binding.chipHquickShort.setChecked(true);
        } else if (count == 1 && historyCriteria.getSortBy() == ReservationFilterHelper.HistorySort.PRICE_DESC) {
            binding.chipHquickPriceDesc.setChecked(true);
        } else {
            binding.chipGroupHistoryQuick.clearCheck();
        }
        suppressQuickChipEvents = false;
    }

    private void updateHistoryFilterBadge() {
        if (binding == null) return;
        int count = historyCriteria.countActiveFilters();
        if (count > 0) {
            binding.badgeHistoryFilterCount.setText(String.valueOf(count));
            binding.badgeHistoryFilterCount.setVisibility(View.VISIBLE);
        } else {
            binding.badgeHistoryFilterCount.setVisibility(View.GONE);
        }
    }

    private void showHistoryFiltersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogHistoryFiltersBinding fb = DialogHistoryFiltersBinding.inflate(LayoutInflater.from(this));
        dialog.setContentView(fb.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        final Long[] tempFromMs = new Long[]{historyCriteria.getCustomEndFromMs()};
        final Long[] tempToMs = new Long[]{historyCriteria.getCustomEndToMs()};

        Runnable syncUi = () -> {
            fb.editHistoryQuery.setText(historyCriteria.getQuery());

            switch (historyCriteria.getSortBy()) {
                case END_ASC:
                    fb.chipHsortEndAsc.setChecked(true);
                    break;
                case PRICE_DESC:
                    fb.chipHsortPriceDesc.setChecked(true);
                    break;
                case PRICE_ASC:
                    fb.chipHsortPriceAsc.setChecked(true);
                    break;
                case DURATION_DESC:
                    fb.chipHsortDurDesc.setChecked(true);
                    break;
                case DURATION_ASC:
                    fb.chipHsortDurAsc.setChecked(true);
                    break;
                case END_DESC:
                default:
                    fb.chipHsortEndDesc.setChecked(true);
                    break;
            }

            switch (historyCriteria.getEndDatePreset()) {
                case TODAY:
                    fb.chipHdateToday.setChecked(true);
                    fb.layoutHistoryCustomDates.setVisibility(View.GONE);
                    break;
                case LAST_7_DAYS:
                    fb.chipHdate7d.setChecked(true);
                    fb.layoutHistoryCustomDates.setVisibility(View.GONE);
                    break;
                case LAST_30_DAYS:
                    fb.chipHdate30d.setChecked(true);
                    fb.layoutHistoryCustomDates.setVisibility(View.GONE);
                    break;
                case CUSTOM:
                    fb.chipHdateCustom.setChecked(true);
                    fb.layoutHistoryCustomDates.setVisibility(View.VISIBLE);
                    break;
                case ALL:
                default:
                    fb.chipHdateAll.setChecked(true);
                    fb.layoutHistoryCustomDates.setVisibility(View.GONE);
                    break;
            }

            tempFromMs[0] = historyCriteria.getCustomEndFromMs();
            tempToMs[0] = historyCriteria.getCustomEndToMs();
            fb.btnHistoryDateFrom.setText(tempFromMs[0] != null
                    ? getString(R.string.history_filter_date_from) + ": " + dateFmt.format(new Date(tempFromMs[0]))
                    : getString(R.string.history_filter_date_from));
            fb.btnHistoryDateTo.setText(tempToMs[0] != null
                    ? getString(R.string.history_filter_date_to) + ": " + dateFmt.format(new Date(tempToMs[0]))
                    : getString(R.string.history_filter_date_to));

            switch (historyCriteria.getDurationPreset()) {
                case SHORT_LE_2H:
                    fb.chipHdurShort.setChecked(true);
                    break;
                case MEDIUM_2_6H:
                    fb.chipHdurMedium.setChecked(true);
                    break;
                case LONG_GT_6H:
                    fb.chipHdurLong.setChecked(true);
                    break;
                case ALL:
                default:
                    fb.chipHdurAll.setChecked(true);
                    break;
            }

            fb.editHistoryPriceMin.setText(historyCriteria.getMinPrice() != null
                    ? formatCompactPrice(historyCriteria.getMinPrice()) : "");
            fb.editHistoryPriceMax.setText(historyCriteria.getMaxPrice() != null
                    ? formatCompactPrice(historyCriteria.getMaxPrice()) : "");
            fb.checkboxOverstayOnly.setChecked(historyCriteria.isOnlyOverstay());

            switch (historyCriteria.getStatusFilter()) {
                case "COMPLETED":
                    fb.chipHstatusCompleted.setChecked(true);
                    break;
                case "CANCELLED":
                    fb.chipHstatusCancelled.setChecked(true);
                    break;
                case "REJECTED":
                    fb.chipHstatusRejected.setChecked(true);
                    break;
                case "EXPIRED":
                    fb.chipHstatusExpired.setChecked(true);
                    break;
                case "NO_SHOW":
                    fb.chipHstatusNoshow.setChecked(true);
                    break;
                case "ALL":
                default:
                    fb.chipHstatusAll.setChecked(true);
                    break;
            }
        };
        syncUi.run();

        fb.chipGroupHistoryDate.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean custom = checkedIds.contains(R.id.chip_hdate_custom);
            fb.layoutHistoryCustomDates.setVisibility(custom ? View.VISIBLE : View.GONE);
        });

        fb.btnHistoryDateFrom.setOnClickListener(v -> showDatePicker(tempFromMs[0], pickedMs -> {
            tempFromMs[0] = pickedMs;
            fb.btnHistoryDateFrom.setText(getString(R.string.history_filter_date_from) + ": " + dateFmt.format(new Date(pickedMs)));
        }));

        fb.btnHistoryDateTo.setOnClickListener(v -> showDatePicker(tempToMs[0], pickedMs -> {
            tempToMs[0] = pickedMs;
            fb.btnHistoryDateTo.setText(getString(R.string.history_filter_date_to) + ": " + dateFmt.format(new Date(pickedMs)));
        }));

        fb.btnCloseHistoryFilters.setOnClickListener(v -> dialog.dismiss());

        fb.btnResetHistoryDialogFilters.setOnClickListener(v -> {
            historyCriteria.reset();
            syncUi.run();
        });

        fb.btnApplyHistoryDialogFilters.setOnClickListener(v -> {
            historyCriteria.setQuery(fb.editHistoryQuery.getText().toString());

            int sortId = fb.chipGroupHistorySort.getCheckedChipId();
            if (sortId == R.id.chip_hsort_end_asc) historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.END_ASC);
            else if (sortId == R.id.chip_hsort_price_desc) historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.PRICE_DESC);
            else if (sortId == R.id.chip_hsort_price_asc) historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.PRICE_ASC);
            else if (sortId == R.id.chip_hsort_dur_desc) historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.DURATION_DESC);
            else if (sortId == R.id.chip_hsort_dur_asc) historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.DURATION_ASC);
            else historyCriteria.setSortBy(ReservationFilterHelper.HistorySort.END_DESC);

            int dateId = fb.chipGroupHistoryDate.getCheckedChipId();
            if (dateId == R.id.chip_hdate_today) historyCriteria.setEndDatePreset(ReservationFilterHelper.DatePreset.TODAY);
            else if (dateId == R.id.chip_hdate_7d) historyCriteria.setEndDatePreset(ReservationFilterHelper.DatePreset.LAST_7_DAYS);
            else if (dateId == R.id.chip_hdate_30d) historyCriteria.setEndDatePreset(ReservationFilterHelper.DatePreset.LAST_30_DAYS);
            else if (dateId == R.id.chip_hdate_custom) historyCriteria.setEndDatePreset(ReservationFilterHelper.DatePreset.CUSTOM);
            else historyCriteria.setEndDatePreset(ReservationFilterHelper.DatePreset.ALL);

            historyCriteria.setCustomEndFromMs(tempFromMs[0]);
            historyCriteria.setCustomEndToMs(tempToMs[0]);

            int durId = fb.chipGroupHistoryDuration.getCheckedChipId();
            if (durId == R.id.chip_hdur_short) historyCriteria.setDurationPreset(ReservationFilterHelper.DurationPreset.SHORT_LE_2H);
            else if (durId == R.id.chip_hdur_medium) historyCriteria.setDurationPreset(ReservationFilterHelper.DurationPreset.MEDIUM_2_6H);
            else if (durId == R.id.chip_hdur_long) historyCriteria.setDurationPreset(ReservationFilterHelper.DurationPreset.LONG_GT_6H);
            else historyCriteria.setDurationPreset(ReservationFilterHelper.DurationPreset.ALL);

            historyCriteria.setMinPrice(parseOptionalDouble(fb.editHistoryPriceMin.getText().toString()));
            historyCriteria.setMaxPrice(parseOptionalDouble(fb.editHistoryPriceMax.getText().toString()));
            historyCriteria.setOnlyOverstay(fb.checkboxOverstayOnly.isChecked());

            int statusId = fb.chipGroupHistoryStatus.getCheckedChipId();
            if (statusId == R.id.chip_hstatus_completed) historyCriteria.setStatusFilter("COMPLETED");
            else if (statusId == R.id.chip_hstatus_cancelled) historyCriteria.setStatusFilter("CANCELLED");
            else if (statusId == R.id.chip_hstatus_rejected) historyCriteria.setStatusFilter("REJECTED");
            else if (statusId == R.id.chip_hstatus_expired) historyCriteria.setStatusFilter("EXPIRED");
            else if (statusId == R.id.chip_hstatus_noshow) historyCriteria.setStatusFilter("NO_SHOW");
            else historyCriteria.setStatusFilter("ALL");

            dialog.dismiss();
            syncQuickHistoryChips();
            if (isHistoryTab) {
                renderFilteredHistory();
            } else {
                updateHistoryFilterBadge();
            }
        });

        dialog.show();
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

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnRefreshReservations.setOnClickListener(v -> {
            com.example.project.utils.ViewAnimationUtils.playRotateAnimation(v, () -> {
                if (viewModel != null) {
                    viewModel.refreshReservations();
                }
            });
            Toast.makeText(this, R.string.msg_refreshing_reservations, Toast.LENGTH_SHORT).show();
        });

        binding.btnSearchOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(ReservationsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void observeViewModel() {
        // Observe incoming / active reservations from Room Single Source of Truth
        viewModel.getIncomingReservationsLiveData().observe(this, activeList -> {
            activeReservationsList.clear();
            if (activeList != null) {
                activeReservationsList.addAll(activeList);
            }
            if (!isHistoryTab) {
                renderList(activeReservationsList);
            }
        });

        // Observe past / history reservations from Room Single Source of Truth
        viewModel.getHistoryReservationsLiveData().observe(this, historyList -> {
            historyReservationsList.clear();
            if (historyList != null) {
                historyReservationsList.addAll(historyList);
            }
            if (isHistoryTab) {
                renderFilteredHistory();
            }
        });

        // Observe loading state to toggle progress bar
        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (binding == null) return;
            binding.progressBarReservations.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe toast feedback messages (check-in, check-out, cancel)
        viewModel.getToastMessageLiveData().observe(this, toastMsg -> {
            if (toastMsg != null && !toastMsg.trim().isEmpty()) {
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReservationClick(Reservation reservation) {
        if (isHistoryTab || reservation == null) return;
        showActiveReservationDialog(reservation);
    }

    @Override
    public void onNavigateClick(Reservation reservation) {
        if (reservation == null) return;

        com.example.project.model.ParkingPlace spot =
                com.example.project.repository.AppRepository.getInstance().getPlaceById(reservation.getPlaceId());

        Uri navUri;
        if (spot != null && spot.getLatitude() != 0.0 && spot.getLongitude() != 0.0) {
            navUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%f,%f", spot.getLatitude(), spot.getLongitude()));
        } else {
            String street = reservation.getStreet() != null && !reservation.getStreet().isEmpty()
                    ? reservation.getStreet() : "Warszawa";
            String city = reservation.getCity() != null && !reservation.getCity().isEmpty()
                    ? reservation.getCity() : "";
            String query = !city.isEmpty() ? city + ", " + street : street;
            navUri = Uri.parse("google.navigation:q=" + Uri.encode(query));
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, navUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri fallbackUri = (spot != null && spot.getLatitude() != 0.0 && spot.getLongitude() != 0.0)
                    ? Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                            spot.getLatitude(), spot.getLongitude(),
                            spot.getLatitude(), spot.getLongitude(),
                            Uri.encode(reservation.getPlaceName() != null ? reservation.getPlaceName() : "Parking")))
                    : Uri.parse("geo:0,0?q=" + Uri.encode(reservation.getStreet() != null ? reservation.getStreet() : "Warszawa"));
            startActivity(new Intent(Intent.ACTION_VIEW, fallbackUri));
        }
    }

    @Override
    public void onCheckInClick(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) return;
        if (viewModel != null) {
            viewModel.checkIn(reservation.getId(), reservation.getPinCode());
        }
    }

    @Override
    public void onCheckOutClick(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) return;
        if (!reservation.isCheckedIn()) {
            Toast.makeText(this, R.string.error_checkout_requires_checkin, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_checkout_confirm_title)
                .setMessage(R.string.dialog_checkout_confirm_msg)
                .setPositiveButton(R.string.btn_confirm_checkout, (dialog, which) -> {
                    if (viewModel != null) {
                        viewModel.checkOut(reservation.getId());
                    }
                    showRatingDialog(reservation.getId(), reservation.getSpotId());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public void onRateSpotClick(Reservation reservation) {
        if (reservation != null && reservation.getId() != null) {
            showRatingDialog(reservation.getId(), reservation.getSpotId());
        }
    }

    private void startTimer() {
        stopTimer();
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    /**
     * Shows detailed modal dialog for the selected reservation with entry PIN code,
     * exact schedule, confirmation download, check-in / check-out actions, and cancellation.
     *
     * @param reservation Selected Reservation instance
     */
    private void showActiveReservationDialog(Reservation reservation) {
        DialogActiveReservationDetailsBinding dialogBinding = DialogActiveReservationDetailsBinding.inflate(LayoutInflater.from(this));

        dialogBinding.textDialogTitle.setText(reservation.getPlaceName() != null ? reservation.getPlaceName() : "Miejsce parkingowe");
        dialogBinding.textDialogStreet.setText(reservation.getStreet() != null ? reservation.getStreet() : "Warszawa");
        dialogBinding.textDialogPin.setText(reservation.getPinCode() != null ? reservation.getPinCode() : "4829");

        String placeImg = reservation.getImageUri();
        if (placeImg == null || placeImg.isEmpty()) {
            com.example.project.model.ParkingPlace p = com.example.project.repository.AppRepository.getInstance().getPlaceById(reservation.getPlaceId());
            if (p != null) placeImg = p.getImageUri();
        }
        com.example.project.utils.ImageLoader.getInstance(this).loadImage(
                dialogBinding.imgDialogSpot,
                placeImg,
                reservation.getPhotoResId() != 0 ? reservation.getPhotoResId() : R.drawable.default_parking_spot
        );

        long startMs = reservation.getStartTimeMs() > 0 ? reservation.getStartTimeMs() : System.currentTimeMillis();
        long endMs = reservation.getEndTimeMs() > 0 ? reservation.getEndTimeMs() : (startMs + 3600000L);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        dialogBinding.textDialogEntryTime.setText(timeFormat.format(new Date(startMs)));
        dialogBinding.textDialogEntryDate.setText(dateFormat.format(new Date(startMs)));
        dialogBinding.textDialogExitTime.setText(timeFormat.format(new Date(endMs)));
        dialogBinding.textDialogExitDate.setText(dateFormat.format(new Date(endMs)));

        // Spot dimensions & access instructions
        com.example.project.model.ParkingPlace spot =
                com.example.project.repository.AppRepository.getInstance().getPlaceById(reservation.getPlaceId());

        if (spot != null) {
            String dimensions = spot.getFormattedDimensions();
            if (dimensions != null && !dimensions.isEmpty()) {
                dialogBinding.textDialogDimensions.setText(dimensions);
                dialogBinding.cardDialogDimensions.setVisibility(View.VISIBLE);
            } else {
                dialogBinding.cardDialogDimensions.setVisibility(View.GONE);
            }

            String instructions = spot.getAccessInstructions();
            if (instructions != null && !instructions.trim().isEmpty()) {
                dialogBinding.textDialogAccessInstructions.setText(instructions.trim());
                dialogBinding.cardDialogAccessInstructions.setVisibility(View.VISIBLE);
            } else {
                dialogBinding.cardDialogAccessInstructions.setVisibility(View.GONE);
            }
        } else {
            dialogBinding.cardDialogDimensions.setVisibility(View.GONE);
            dialogBinding.cardDialogAccessInstructions.setVisibility(View.GONE);
        }

        dialogBinding.btnDialogNavigate.setOnClickListener(v -> onNavigateClick(reservation));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        // Configure Check-In and Check-Out actions according to parking lifecycle state
        long now = System.currentTimeMillis();
        String effectiveStatus = ReservationLifecycleManager
                .computeEffectiveStatus(reservation.getStatus(), reservation.getEndTimeMs(), now)
                .toUpperCase(Locale.ROOT);
        boolean isHistoryState = ReservationLifecycleManager.isHistoryStatus(
                reservation.getStatus(), reservation.getEndTimeMs(), now);

        if (isHistoryState) {
            dialogBinding.btnCheckIn.setVisibility(View.GONE);
            dialogBinding.btnCheckOut.setVisibility(View.GONE);
            dialogBinding.btnCancelReservation.setVisibility(View.GONE);
            dialogBinding.btnDialogNavigate.setVisibility(View.GONE);
        } else if (reservation.isCheckedIn()) {
            dialogBinding.btnCheckIn.setVisibility(View.VISIBLE);
            dialogBinding.btnCheckOut.setVisibility(View.VISIBLE);
            dialogBinding.btnCheckIn.setEnabled(false);
            dialogBinding.btnCheckIn.setText(R.string.btn_already_checked_in);
            dialogBinding.btnCheckIn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            dialogBinding.btnCheckIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            boolean inOverstay = reservation.getEndTimeMs() > 0 && now >= reservation.getEndTimeMs();
            dialogBinding.btnCheckOut.setEnabled(true);
            dialogBinding.btnCheckOut.setBackgroundTintList(ContextCompat.getColorStateList(
                    this, inOverstay ? R.color.brand_rose_700 : R.color.orange_dot));
            dialogBinding.btnCancelReservation.setVisibility(View.GONE);
        } else if (effectiveStatus.contains("PENDING")) {
            dialogBinding.btnCheckIn.setVisibility(View.GONE);
            dialogBinding.btnCheckOut.setVisibility(View.GONE);
            dialogBinding.btnCancelReservation.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.btnCheckIn.setVisibility(View.VISIBLE);
            dialogBinding.btnCheckOut.setVisibility(View.VISIBLE);
            dialogBinding.btnCheckIn.setEnabled(true);
            dialogBinding.btnCheckIn.setText(R.string.btn_check_in);
            dialogBinding.btnCheckIn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));
            dialogBinding.btnCheckIn.setTextColor(ContextCompat.getColor(this, R.color.white));

            dialogBinding.btnCheckOut.setEnabled(false);
            dialogBinding.btnCheckOut.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            dialogBinding.btnCheckOut.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            dialogBinding.btnCancelReservation.setVisibility(View.VISIBLE);
        }

        dialogBinding.btnCheckIn.setOnClickListener(v -> {
            if (reservation != null && reservation.getId() != null && viewModel != null) {
                viewModel.checkIn(reservation.getId(), reservation.getPinCode());
            }
            dialog.dismiss();
        });

        dialogBinding.btnCheckOut.setOnClickListener(v -> {
            if (reservation == null || !reservation.isCheckedIn()) {
                Toast.makeText(this, R.string.error_checkout_requires_checkin, Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_checkout_confirm_title)
                    .setMessage(R.string.dialog_checkout_confirm_msg)
                    .setPositiveButton(R.string.btn_confirm_checkout, (confirmDialog, which) -> {
                        if (viewModel != null && reservation.getId() != null) {
                            viewModel.checkOut(reservation.getId());
                        }
                        showRatingDialog(reservation.getId(), reservation.getSpotId());
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        });

        dialogBinding.btnDownloadConfirmation.setOnClickListener(v -> {
            Toast.makeText(this, R.string.toast_download_confirmation, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogBinding.btnCancelReservation.setOnClickListener(v -> {
            if (reservation != null && reservation.getId() != null) {
                AppRepository.getInstance().cancelReservationById(reservation.getId());
                if (viewModel != null) {
                    viewModel.cancelReservation(reservation.getId());
                }
            } else {
                AppRepository.getInstance().cancelActiveReservation();
            }
            NotificationHelper.showNotification(this, "ParkShare", getString(R.string.toast_reservation_cancelled));
            Toast.makeText(this, R.string.toast_reservation_cancelled, Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            // Refresh from REST backend
            if (viewModel != null) {
                viewModel.refreshReservations();
            }
        });

        dialogBinding.btnCloseDialogTop.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnCloseDetails.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Opens rating and feedback dialog for a completed reservation.
     * Submits rating to POST /api/reservations/{id}/reviews and updates Room cache via ReviewRepository.
     *
     * @param reservationId Unique reservation identifier
     * @param spotId        Associated spot identifier
     */
    private void showRatingDialog(@NonNull String reservationId, @Nullable String spotId) {
        DialogRatingBinding ratingBinding = DialogRatingBinding.inflate(LayoutInflater.from(this));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(ratingBinding.getRoot())
                .setPositiveButton(R.string.btn_submit_review, (d, which) -> {
                    int rating = Math.round(ratingBinding.ratingBar.getRating());
                    if (rating < 1) {
                        Toast.makeText(this, R.string.error_rating_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String comment = ratingBinding.editReviewComment.getText() != null
                            ? ratingBinding.editReviewComment.getText().toString().trim() : null;

                    ReviewRepository.getInstance(this).createReview(reservationId, spotId, rating, comment)
                            .observe(this, resource -> {
                                if (resource != null) {
                                    if (resource.status == Resource.Status.SUCCESS) {
                                        Toast.makeText(this, R.string.toast_review_submitted, Toast.LENGTH_SHORT).show();
                                    } else if (resource.status == Resource.Status.ERROR) {
                                        String err = resource.message != null ? resource.message : getString(R.string.error_spot_operation_failed, "");
                                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        ratingBinding.btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        binding = null;
    }
}
