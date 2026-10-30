package com.example.project.ui;

import android.content.Intent;
import android.net.Uri;
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
import com.example.project.databinding.DialogRatingBinding;
import com.example.project.model.Reservation;
import com.example.project.network.Resource;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReviewRepository;
import com.example.project.utils.NotificationHelper;
import com.example.project.viewmodel.ReservationsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private final List<Reservation> activeReservationsList = new ArrayList<>();
    private final List<Reservation> historyReservationsList = new ArrayList<>();

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
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
        setupClickListeners();
        observeViewModel();

        // Initial fetch of reservations from REST API
        viewModel.refreshReservations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
        // Automatically sync fresh reservations from REST backend
        if (viewModel != null) {
            viewModel.refreshReservations();
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
            binding.btnTabActive.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            binding.btnTabActive.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.btnTabHistory.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));
            binding.btnTabHistory.setTextColor(ContextCompat.getColor(this, R.color.white));

            renderList(historyReservationsList);
        } else {
            binding.btnTabActive.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));
            binding.btnTabActive.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.btnTabHistory.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            binding.btnTabHistory.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            renderList(activeReservationsList);
        }
    }

    private void renderList(List<Reservation> list) {
        if (binding == null || adapter == null) return;

        adapter.setReservations(list);

        if (list == null || list.isEmpty()) {
            binding.recyclerReservations.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);

            if (isHistoryTab) {
                binding.textEmptyTitle.setText(R.string.tab_history);
                binding.textEmptyMessage.setText(R.string.msg_no_history_reservation);
                binding.btnSearchOnMap.setVisibility(View.GONE);
            } else {
                binding.textEmptyTitle.setText(R.string.title_reservations);
                binding.textEmptyMessage.setText(R.string.msg_no_active_reservation);
                binding.btnSearchOnMap.setVisibility(View.VISIBLE);
            }
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerReservations.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnRefreshReservations.setOnClickListener(v -> {
            if (viewModel != null) {
                viewModel.refreshReservations();
            }
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
                renderList(historyReservationsList);
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
        if (reservation != null) {
            showActiveReservationDialog(reservation);
        }
    }

    @Override
    public void onNavigateClick(Reservation reservation) {
        if (reservation == null) return;

        String street = reservation.getStreet() != null && !reservation.getStreet().isEmpty()
                ? reservation.getStreet() : "Warszawa";
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(street));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            startActivity(genericMapIntent);
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

        long startMs = reservation.getStartTimeMs() > 0 ? reservation.getStartTimeMs() : System.currentTimeMillis();
        long endMs = reservation.getEndTimeMs() > 0 ? reservation.getEndTimeMs() : (startMs + 3600000L);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        dialogBinding.textDialogEntryTime.setText(timeFormat.format(new Date(startMs)));
        dialogBinding.textDialogEntryDate.setText(dateFormat.format(new Date(startMs)));
        dialogBinding.textDialogExitTime.setText(timeFormat.format(new Date(endMs)));
        dialogBinding.textDialogExitDate.setText(dateFormat.format(new Date(endMs)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        // Configure Check-In and Check-Out actions according to parking lifecycle state
        boolean isCheckedIn = reservation.isCheckedIn();
        if (isCheckedIn) {
            dialogBinding.btnCheckIn.setEnabled(false);
            dialogBinding.btnCheckIn.setText(R.string.btn_already_checked_in);
            dialogBinding.btnCheckIn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            dialogBinding.btnCheckIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            dialogBinding.btnCheckOut.setEnabled(true);
            dialogBinding.btnCheckOut.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_dot));
            dialogBinding.btnCancelReservation.setVisibility(View.GONE);
        } else {
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

        new AlertDialog.Builder(this)
                .setView(ratingBinding.getRoot())
                .setPositiveButton(R.string.btn_submit_review, (dialog, which) -> {
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
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        binding = null;
    }
}
