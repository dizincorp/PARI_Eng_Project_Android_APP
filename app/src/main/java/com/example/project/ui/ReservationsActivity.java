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
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.R;
import com.example.project.databinding.ActivityReservationsBinding;
import com.example.project.databinding.DialogActiveReservationDetailsBinding;
import com.example.project.databinding.DialogRatingBinding;
import com.example.project.databinding.ItemHistoryReservationBinding;
import com.example.project.model.Reservation;
import com.example.project.repository.AppRepository;
import com.example.project.utils.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for viewing active reservations and reservation history with rating capability.
 */
public class ReservationsActivity extends BaseActivity {

    private ActivityReservationsBinding binding;
    private boolean isHistoryTab = false;
    private Reservation currentActiveReservation = null;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdownText();
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

        setupTabs();
        setupClickListeners();
        observeViewModel();
    }

    private void setupTabs() {
        if (binding == null) return;

        binding.btnTabActive.setOnClickListener(v -> selectTab(false));
        binding.btnTabHistory.setOnClickListener(v -> selectTab(true));

        selectTab(false);
    }

    private void selectTab(boolean isHistory) {
        isHistoryTab = isHistory;
        if (binding == null) return;

        if (isHistory) {
            binding.btnTabActive.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            binding.btnTabActive.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.btnTabHistory.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));
            binding.btnTabHistory.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.cardActiveReservation.setVisibility(View.GONE);
            binding.layoutEmptyActive.setVisibility(View.GONE);
            binding.containerHistoryReservations.setVisibility(View.VISIBLE);
        } else {
            binding.btnTabActive.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.chip_bg_selected));
            binding.btnTabActive.setTextColor(ContextCompat.getColor(this, R.color.white));

            binding.btnTabHistory.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_input_bg));
            binding.btnTabHistory.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.containerHistoryReservations.setVisibility(View.GONE);
            updateActiveReservationUI();
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnSearchOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(ReservationsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        binding.cardActiveReservation.setOnClickListener(v -> {
            if (currentActiveReservation != null) {
                showActiveReservationDialog(currentActiveReservation);
            }
        });

        binding.btnNavigateMap.setOnClickListener(v -> {
            if (currentActiveReservation != null) {
                String street = currentActiveReservation.getStreet() != null ? currentActiveReservation.getStreet() : "Warszawa";
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
        });
    }

    private void observeViewModel() {
        AppRepository.getInstance().getActiveReservationLiveData().observe(this, activeReservation -> {
            this.currentActiveReservation = activeReservation;
            if (!isHistoryTab) {
                updateActiveReservationUI();
            }
        });

        AppRepository.getInstance().getHistoryReservationsLiveData().observe(this, this::updateHistoryUI);
    }

    private void updateActiveReservationUI() {
        if (binding == null) return;

        if (currentActiveReservation == null) {
            binding.cardActiveReservation.setVisibility(View.GONE);
            binding.layoutEmptyActive.setVisibility(View.VISIBLE);
            stopTimer();
        } else {
            binding.layoutEmptyActive.setVisibility(View.GONE);
            binding.cardActiveReservation.setVisibility(View.VISIBLE);

            binding.textResTitle.setText(currentActiveReservation.getPlaceName() != null ? currentActiveReservation.getPlaceName() : "Centrum Park");
            String city = currentActiveReservation.getCity() != null ? currentActiveReservation.getCity() : "Warszawa";
            String street = currentActiveReservation.getStreet() != null ? currentActiveReservation.getStreet() : "";
            binding.textResAddress.setText(getString(R.string.format_city_street, city, street));
            binding.textResPlaceNumber.setText(currentActiveReservation.getPlaceNumber() != null ? currentActiveReservation.getPlaceNumber() : "42");
            binding.textResDate.setText(currentActiveReservation.getDateString() != null ? currentActiveReservation.getDateString() : "Dziś");
            binding.imgResPhoto.setImageResource(currentActiveReservation.getPhotoResId());

            startTimer();
        }
    }

    private void updateHistoryUI(List<Reservation> historyList) {
        if (binding == null) return;

        binding.containerHistoryReservations.removeAllViews();

        if (historyList == null || historyList.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Reservation h : historyList) {
            if (h == null) continue;

            ItemHistoryReservationBinding itemBinding = ItemHistoryReservationBinding.inflate(inflater, binding.containerHistoryReservations, false);

            itemBinding.textHistoryTitle.setText(h.getPlaceName() != null ? h.getPlaceName() : "");
            itemBinding.textHistoryStreet.setText(h.getStreet() != null ? h.getStreet() : "");
            itemBinding.textHistoryVehicle.setText(h.getVehicleInfo() != null ? h.getVehicleInfo() : "");
            itemBinding.textHistoryDate.setText(h.getDateString() != null ? h.getDateString() : "");
            itemBinding.textHistoryPrice.setText(String.format(Locale.getDefault(), "%.2f PLN", h.getTotalPrice()));

            binding.containerHistoryReservations.addView(itemBinding.getRoot());
        }
    }

    private void startTimer() {
        stopTimer();
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateCountdownText() {
        if (binding == null || currentActiveReservation == null) return;

        long now = System.currentTimeMillis();
        long startTime = currentActiveReservation.getStartTimeMs();
        long endTime = currentActiveReservation.getEndTimeMs();

        if (now < startTime) {
            // Not started yet
            long diff = startTime - now;
            long hours = diff / (1000 * 60 * 60);
            long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (diff % (1000 * 60)) / 1000;
            String formattedTime = String.format(Locale.getDefault(), "Do rozpoczęcia: %dh %02dm %02ds", hours, minutes, seconds);
            binding.textResCountdown.setText(formattedTime);
        } else {
            // Active now
            long diff = endTime - now;
            if (diff < 0) {
                diff = 0;
            }
            long hours = diff / (1000 * 60 * 60);
            long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (diff % (1000 * 60)) / 1000;
            String formattedTime = String.format(Locale.getDefault(), "Pozostało: %dh %02dm %02ds", hours, minutes, seconds);
            binding.textResCountdown.setText(formattedTime);
        }
    }

    private void showActiveReservationDialog(Reservation reservation) {
        DialogActiveReservationDetailsBinding dialogBinding = DialogActiveReservationDetailsBinding.inflate(LayoutInflater.from(this));

        dialogBinding.textDialogTitle.setText(reservation.getPlaceName());
        dialogBinding.textDialogStreet.setText(reservation.getStreet());
        dialogBinding.textDialogPin.setText(reservation.getPinCode());

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

        dialogBinding.btnDownloadConfirmation.setOnClickListener(v -> {
            Toast.makeText(this, R.string.toast_download_confirmation, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogBinding.btnCancelReservation.setOnClickListener(v -> {
            if (reservation != null && reservation.getId() != null) {
                AppRepository.getInstance().cancelReservationById(reservation.getId());
            } else {
                AppRepository.getInstance().cancelActiveReservation();
            }
            NotificationHelper.showNotification(this, "ParkShare", getString(R.string.toast_reservation_cancelled));
            Toast.makeText(this, R.string.toast_reservation_cancelled, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            showRatingDialog();
        });

        dialog.show();
    }

    private void showRatingDialog() {
        DialogRatingBinding ratingBinding = DialogRatingBinding.inflate(LayoutInflater.from(this));

        new AlertDialog.Builder(this)
                .setView(ratingBinding.getRoot())
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    float rating = ratingBinding.ratingBar.getRating();
                    AppRepository.getInstance().rateUser(rating);
                    Toast.makeText(this, R.string.toast_rating_submitted, Toast.LENGTH_SHORT).show();
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
