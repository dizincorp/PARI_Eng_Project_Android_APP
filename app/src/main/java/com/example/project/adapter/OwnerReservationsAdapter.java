package com.example.project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.databinding.ItemOwnerReservationBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter presenting incoming / active / pending reservations for a parking spot owner.
 * Allows confirming (accepting) or rejecting reservation requests.
 */
public class OwnerReservationsAdapter extends RecyclerView.Adapter<OwnerReservationsAdapter.ViewHolder> {

    public interface OnReservationActionListener {
        void onAccept(@NonNull ReservationEntity reservation);
        void onReject(@NonNull ReservationEntity reservation);
    }

    private final List<ReservationEntity> reservations = new ArrayList<>();
    private final OnReservationActionListener listener;

    public OwnerReservationsAdapter(OnReservationActionListener listener) {
        this.listener = listener;
    }

    public void setReservations(List<ReservationEntity> newReservations) {
        reservations.clear();
        if (newReservations != null) {
            reservations.addAll(newReservations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOwnerReservationBinding binding = ItemOwnerReservationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReservationEntity item = reservations.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemOwnerReservationBinding binding;

        ViewHolder(ItemOwnerReservationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReservationEntity item, OnReservationActionListener listener) {
            Context context = binding.getRoot().getContext();

            // Spot Name and Address
            binding.textSpotName.setText(item.getPlaceName() != null && !item.getPlaceName().isEmpty()
                    ? item.getPlaceName() : context.getString(R.string.title_places));

            String street = item.getStreet() != null ? item.getStreet() : "";
            String city = item.getCity() != null ? item.getCity() : "";
            String fullAddress = (street + (street.isEmpty() || city.isEmpty() ? "" : " • ") + city).trim();
            binding.textSpotAddress.setText(fullAddress.isEmpty() ? "Lublin" : fullAddress);

            // Date & Time
            String date = item.getDateString() != null ? item.getDateString() : "";
            String time = item.getTimeRange() != null ? item.getTimeRange() : "";
            binding.textDateTimeRange.setText((date + "  " + time).trim());

            // Driver & Vehicle
            String driver = item.getUserName() != null && !item.getUserName().isEmpty()
                    ? item.getUserName() : "Kierowca";
            String plate = item.getCarPlate() != null && !item.getCarPlate().isEmpty()
                    ? item.getCarPlate() : (item.getVehicleInfo() != null ? item.getVehicleInfo() : "");
            binding.textDriverInfo.setText(plate.isEmpty() ? driver : driver + " • " + plate);

            // Price
            binding.textPrice.setText(String.format(Locale.getDefault(), "Koszt: %.2f zł", item.getTotalPrice()));

            // Status & Actions
            String status = item.getStatus() != null ? item.getStatus().toUpperCase(Locale.ROOT) : "PENDING_APPROVAL";

            if ("PENDING_APPROVAL".equalsIgnoreCase(status) || "PENDING_PAYMENT".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.tab_pending));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_amber);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_amber_700));

                binding.textPinCode.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.VISIBLE);
                binding.btnAccept.setVisibility(View.VISIBLE);
                binding.btnAccept.setText(context.getString(R.string.btn_accept_reservation));
                binding.btnReject.setVisibility(View.VISIBLE);
                binding.btnReject.setText(context.getString(R.string.btn_reject_reservation));

                binding.btnAccept.setOnClickListener(v -> {
                    if (listener != null) listener.onAccept(item);
                });
                binding.btnReject.setOnClickListener(v -> {
                    if (listener != null) listener.onReject(item);
                });

            } else if ("CONFIRMED".equalsIgnoreCase(status) || "AKTYWNA".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.status_confirmed));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_emerald);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_emerald_700));

                if (item.getPinCode() != null && !item.getPinCode().isEmpty()) {
                    binding.textPinCode.setVisibility(View.VISIBLE);
                    binding.textPinCode.setText(String.format("PIN: %s", item.getPinCode()));
                } else {
                    binding.textPinCode.setVisibility(View.GONE);
                }

                binding.layoutActions.setVisibility(View.VISIBLE);
                binding.btnAccept.setVisibility(View.GONE);
                binding.btnReject.setVisibility(View.VISIBLE);
                binding.btnReject.setText(context.getString(R.string.btn_reject_reservation));
                binding.btnReject.setOnClickListener(v -> {
                    if (listener != null) listener.onReject(item);
                });

            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.tab_in_progress));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_emerald);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_emerald_700));

                if (item.getPinCode() != null && !item.getPinCode().isEmpty()) {
                    binding.textPinCode.setVisibility(View.VISIBLE);
                    binding.textPinCode.setText(String.format("PIN: %s", item.getPinCode()));
                } else {
                    binding.textPinCode.setVisibility(View.GONE);
                }
                binding.layoutActions.setVisibility(View.GONE);

            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.status_completed));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_slate);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_slate_700));
                binding.textPinCode.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.GONE);

            } else if ("REJECTED".equalsIgnoreCase(status) || "ODRZUCONA".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.status_rejected));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_rose);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_rose_700));
                binding.textPinCode.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.GONE);

            } else if ("EXPIRED".equalsIgnoreCase(status) || "WYGASŁA".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.status_expired));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_slate);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_slate_600));
                binding.textPinCode.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.GONE);

            } else if ("NO_SHOW".equalsIgnoreCase(status)) {
                binding.textReservationStatus.setText(context.getString(R.string.status_no_show));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_rose);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_rose_700));
                binding.textPinCode.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.GONE);

            } else { // CANCELLED
                binding.textReservationStatus.setText(context.getString(R.string.status_cancelled));
                binding.textReservationStatus.setBackgroundResource(R.drawable.bg_badge_rose);
                binding.textReservationStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_rose_700));
                binding.textPinCode.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.GONE);
            }
        }
    }
}
