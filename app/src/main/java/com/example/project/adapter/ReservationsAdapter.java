package com.example.project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.databinding.ItemReservationBinding;
import com.example.project.model.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter displaying reservation items for active and historical reservations.
 * Binds parking spot details, schedules, vehicle plate, PIN codes, price, and dynamic countdown timers.
 */
public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder> {

    public interface OnReservationItemClickListener {
        void onReservationClick(Reservation reservation);

        void onNavigateClick(Reservation reservation);
    }

    private final List<Reservation> reservations = new ArrayList<>();
    private boolean isHistoryTab = false;
    private final OnReservationItemClickListener listener;

    public ReservationsAdapter(OnReservationItemClickListener listener) {
        this.listener = listener;
    }

    public void setReservations(List<Reservation> newReservations) {
        this.reservations.clear();
        if (newReservations != null) {
            this.reservations.addAll(newReservations);
        }
        notifyDataSetChanged();
    }

    public void setIsHistoryTab(boolean isHistory) {
        this.isHistoryTab = isHistory;
        notifyDataSetChanged();
    }

    /**
     * Refreshes dynamic countdown timers on visible active items without rebinding the whole view.
     */
    public void updateCountdowns() {
        if (!isHistoryTab && !reservations.isEmpty()) {
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReservationBinding binding = ItemReservationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ReservationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        if (position >= 0 && position < reservations.size()) {
            holder.bind(reservations.get(position), isHistoryTab, listener);
        }
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {

        private final ItemReservationBinding binding;

        ReservationViewHolder(ItemReservationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Reservation res, boolean isHistory, OnReservationItemClickListener listener) {
            if (res == null) return;
            Context context = itemView.getContext();

            // Spot Title & Street Address
            binding.textReservationTitle.setText(res.getPlaceName() != null && !res.getPlaceName().isEmpty()
                    ? res.getPlaceName() : "Miejsce parkingowe");

            String street = res.getStreet() != null && !res.getStreet().isEmpty() ? res.getStreet() : "Warszawa";
            String city = res.getCity() != null && !res.getCity().isEmpty() ? res.getCity() : "Warszawa";
            binding.textReservationAddress.setText(String.format(Locale.getDefault(), "%s, %s", city, street));

            // Spot Number designation pill
            String placeNum = res.getPlaceNumber() != null && !res.getPlaceNumber().isEmpty() ? res.getPlaceNumber() : "42";
            binding.textSpotNumberPill.setText(String.format(Locale.getDefault(), "Nr %s", placeNum));

            // Price
            binding.textReservationPrice.setText(String.format(Locale.getDefault(), "%.2f PLN", res.getTotalPrice()));

            // Schedule: Date & Time range
            String dateStr = res.getDateString() != null && !res.getDateString().isEmpty() ? res.getDateString() : "2026-10-25";
            String timeRange = res.getTimeRange() != null && !res.getTimeRange().isEmpty() ? res.getTimeRange() : "";
            if (!timeRange.isEmpty()) {
                binding.textReservationSchedule.setText(String.format(Locale.getDefault(), "%s • %s", dateStr, timeRange));
            } else {
                binding.textReservationSchedule.setText(dateStr);
            }

            // Vehicle Plate & Model
            String vehicleInfo = res.getVehicleInfo() != null && !res.getVehicleInfo().isEmpty()
                    ? res.getVehicleInfo() : (res.getCarPlate() != null ? res.getCarPlate() : "Pojazd");
            binding.textReservationVehicle.setText(vehicleInfo);

            // PIN Code
            if (res.getPinCode() != null && !res.getPinCode().isEmpty()) {
                binding.layoutPinContainer.setVisibility(View.VISIBLE);
                binding.textReservationPin.setText(String.format(Locale.getDefault(), "KOD PIN: %s", res.getPinCode()));
            } else {
                binding.layoutPinContainer.setVisibility(View.GONE);
            }

            // Status Badge Formatting
            String status = res.getStatus() != null ? res.getStatus().toUpperCase(Locale.ROOT) : "AKTYWNA";
            if (status.contains("AKT") || status.contains("CONFIRM") || status.contains("ACTIV")) {
                binding.badgeStatus.setText(R.string.badge_active_now);
                binding.badgeStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green_badge_bg));
                binding.badgeStatus.setTextColor(ContextCompat.getColor(context, R.color.green_badge_text));
            } else if (status.contains("PENDING")) {
                binding.badgeStatus.setText("OCZEKUJE NA PŁATNOŚĆ");
                binding.badgeStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.rating_bg));
                binding.badgeStatus.setTextColor(ContextCompat.getColor(context, R.color.orange_dot));
            } else if (status.contains("CANCEL") || status.contains("ANUL")) {
                binding.badgeStatus.setText("ANULOWANA");
                binding.badgeStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.logout_bg));
                binding.badgeStatus.setTextColor(ContextCompat.getColor(context, R.color.logout_text));
            } else {
                binding.badgeStatus.setText("ZAKOŃCZONA");
                binding.badgeStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray_input_bg));
                binding.badgeStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }

            // Countdown banner & Navigation button
            if (isHistory || status.contains("CANCEL") || status.contains("ZAKOŃ")) {
                binding.cardCountdownBanner.setVisibility(View.GONE);
                binding.btnItemNavigate.setVisibility(View.GONE);
            } else {
                binding.cardCountdownBanner.setVisibility(View.VISIBLE);
                binding.btnItemNavigate.setVisibility(View.VISIBLE);

                long now = System.currentTimeMillis();
                long startTime = res.getStartTimeMs();
                long endTime = res.getEndTimeMs();

                if (startTime > 0 && now < startTime) {
                    long diff = startTime - now;
                    long hours = diff / (1000 * 60 * 60);
                    long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
                    long seconds = (diff % (1000 * 60)) / 1000;
                    binding.textItemCountdown.setText(String.format(Locale.getDefault(), "Do rozpoczęcia: %dh %02dm %02ds", hours, minutes, seconds));
                } else if (endTime > 0) {
                    long diff = Math.max(0, endTime - now);
                    long hours = diff / (1000 * 60 * 60);
                    long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
                    long seconds = (diff % (1000 * 60)) / 1000;
                    binding.textItemCountdown.setText(String.format(Locale.getDefault(), "Pozostało: %dh %02dm %02ds", hours, minutes, seconds));
                } else {
                    binding.textItemCountdown.setText("Rezerwacja aktywna");
                }
            }

            // Click listeners
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onReservationClick(res);
            });

            binding.btnItemDetails.setOnClickListener(v -> {
                if (listener != null) listener.onReservationClick(res);
            });

            binding.btnItemNavigate.setOnClickListener(v -> {
                if (listener != null) listener.onNavigateClick(res);
            });
        }
    }
}
