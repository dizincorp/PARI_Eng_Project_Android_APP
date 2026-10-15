package com.example.project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.databinding.ItemHistoryReservationBinding;
import com.example.project.model.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying reservation history items.
 */
public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder> {

    private final List<Reservation> reservations = new ArrayList<>();

    public void setReservations(List<Reservation> newReservations) {
        this.reservations.clear();
        if (newReservations != null) {
            this.reservations.addAll(newReservations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryReservationBinding binding = ItemHistoryReservationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ReservationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        if (position >= 0 && position < reservations.size()) {
            holder.bind(reservations.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {

        private final ItemHistoryReservationBinding binding;

        ReservationViewHolder(ItemHistoryReservationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Reservation res) {
            if (res == null) return;

            binding.textHistoryTitle.setText(res.getPlaceName() != null ? res.getPlaceName() : "");
            binding.textHistoryStreet.setText(res.getStreet() != null ? res.getStreet() : "");
            binding.textHistoryVehicle.setText(res.getVehicleInfo() != null ? res.getVehicleInfo() : "");
            binding.textHistoryDate.setText(res.getDateString() != null ? res.getDateString() : "");
            binding.textHistoryPrice.setText(String.format(Locale.getDefault(), "%.2f PLN", res.getTotalPrice()));
        }
    }
}