package com.example.project.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project.R;
import com.example.project.adapter.OwnerReservationsAdapter;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.databinding.ActivityOwnerReservationsBinding;
import com.example.project.viewmodel.OwnerReservationsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen presenting incoming and active reservations for the parking spot owner.
 * Allows confirming or rejecting reservation requests (F03).
 */
public class OwnerReservationsActivity extends BaseActivity implements OwnerReservationsAdapter.OnReservationActionListener {

    private ActivityOwnerReservationsBinding binding;
    private OwnerReservationsViewModel viewModel;
    private OwnerReservationsAdapter adapter;

    private final List<ReservationEntity> allReservations = new ArrayList<>();
    private String currentFilter = "ALL";

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
                currentFilter = "PENDING";
            } else if (checkedIds.contains(R.id.chip_confirmed)) {
                currentFilter = "CONFIRMED";
            } else if (checkedIds.contains(R.id.chip_in_progress)) {
                currentFilter = "IN_PROGRESS";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> {
            com.example.project.utils.ViewAnimationUtils.playBounceAnimation(v, null);
            finish();
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
        List<ReservationEntity> filtered = new ArrayList<>();

        for (ReservationEntity res : allReservations) {
            if (res == null) continue;
            String status = res.getStatus() != null ? res.getStatus().toUpperCase(Locale.ROOT) : "";

            if ("ALL".equals(currentFilter)) {
                filtered.add(res);
            } else if ("PENDING".equals(currentFilter)) {
                if ("PENDING_APPROVAL".equals(status) || "PENDING_PAYMENT".equals(status)) {
                    filtered.add(res);
                }
            } else if ("CONFIRMED".equals(currentFilter)) {
                if ("CONFIRMED".equals(status) || "AKTYWNA".equals(status)) {
                    filtered.add(res);
                }
            } else if ("IN_PROGRESS".equals(currentFilter)) {
                if ("IN_PROGRESS".equals(status)) {
                    filtered.add(res);
                }
            }
        }

        adapter.setReservations(filtered);

        if (filtered.isEmpty()) {
            binding.recyclerReservations.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerReservations.setVisibility(View.VISIBLE);
        }
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_phone, null);
        EditText inputReason = dialogView.findViewById(R.id.edit_phone_number);
        if (inputReason != null) {
            inputReason.setHint(R.string.hint_reject_reason);
            inputReason.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }

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
