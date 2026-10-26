package com.example.project.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.databinding.ItemMyPlaceBinding;
import com.example.project.model.ParkingPlace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying and managing owner's parking spots list
 * with real-time status indicators (green, yellow, red, gray) and active/inactive switch slider.
 */
public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    /**
     * Interface for handling spot item clicks and slider state toggling.
     */
    public interface OnPlaceClickListener {
        /**
         * Triggered when the user taps on the spot card for viewing/editing details.
         *
         * @param place Selected ParkingPlace
         */
        void onPlaceClick(ParkingPlace place);

        /**
         * Triggered when the owner toggles the spot activation switch.
         *
         * @param place    Target ParkingPlace
         * @param isActive True if activated (slider ON), false if deactivated (slider OFF)
         */
        void onPlaceToggleActive(ParkingPlace place, boolean isActive);
    }

    private final List<ParkingPlace> places = new ArrayList<>();
    private OnPlaceClickListener listener;

    /**
     * Sets the click and toggle listener.
     *
     * @param listener OnPlaceClickListener instance
     */
    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of parking places in the adapter.
     *
     * @param newPlaces List of ParkingPlace items
     */
    public void setPlaces(List<ParkingPlace> newPlaces) {
        this.places.clear();
        if (newPlaces != null) {
            this.places.addAll(newPlaces);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMyPlaceBinding binding = ItemMyPlaceBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PlaceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        if (position >= 0 && position < places.size()) {
            holder.bind(places.get(position), listener);
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {

        private final ItemMyPlaceBinding binding;

        PlaceViewHolder(ItemMyPlaceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ParkingPlace place, OnPlaceClickListener listener) {
            if (place == null) return;

            Context context = itemView.getContext();

            // 1. Title and address subtitle
            binding.textPlaceName.setText(place.getTitle() != null ? place.getTitle() : "Miejsce parkingowe");
            String priceStr = String.format(Locale.getDefault(), "%.2f zł/h", place.getPrice());
            String subInfo = (place.getStreet() != null ? place.getStreet() : "Warszawa")
                    + " • " + priceStr
                    + " • Czas: " + (place.getAccessTime() != null ? place.getAccessTime() : "24/7");
            binding.textPlaceSub.setText(subInfo);

            // 2. Slider (MaterialSwitch) configuration
            binding.switchSpotActive.setOnCheckedChangeListener(null);
            boolean isActive = place.isActive();
            binding.switchSpotActive.setChecked(isActive);
            binding.switchSpotActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onPlaceToggleActive(place, isChecked);
                }
            });

            // 3. Card Visual State & Status Dot Indicators:
            // - Зеленый: место свободно и активно
            // - Желтый: место уже зарезервировано (водитель и машина отображаются)
            // - Красный: место занято (водитель сделал check-in)
            // - Серый: место не активно (slider OFF)
            if (!isActive) {
                // INACTIVE STATE: Dim card to grayish appearance
                binding.cardPlaceRoot.setAlpha(0.60f);
                binding.cardPlaceRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.inactive_card_bg));
                binding.viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.status_dot_gray)));
                binding.textDriverInfo.setVisibility(View.GONE);
            } else {
                // ACTIVE STATE: Normal opaque card
                binding.cardPlaceRoot.setAlpha(1.0f);
                binding.cardPlaceRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));

                if (place.isCheckedIn()) {
                    // RED: Driver checked-in (currently parked)
                    binding.viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.status_dot_red)));

                    String driver = place.getDriverName() != null && !place.getDriverName().isEmpty()
                            ? place.getDriverName() : "Kierowca";
                    String vehicle = place.getDriverVehicle() != null && !place.getDriverVehicle().isEmpty()
                            ? place.getDriverVehicle() : "Pojazd w trakcie parkowania";
                    binding.textDriverInfo.setText(String.format("🚗 Parkuje: %s • %s", driver, vehicle));
                    binding.textDriverInfo.setTextColor(ContextCompat.getColor(context, R.color.status_dot_red));
                    binding.textDriverInfo.setVisibility(View.VISIBLE);
                } else if (place.isOccupied() || place.isReserved() || (place.getDriverName() != null && !place.getDriverName().isEmpty())) {
                    // YELLOW: Reserved by a driver, awaiting check-in
                    binding.viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.status_dot_yellow)));

                    String driver = place.getDriverName() != null && !place.getDriverName().isEmpty()
                            ? place.getDriverName() : "Zarezerwowane";
                    String vehicle = place.getDriverVehicle() != null && !place.getDriverVehicle().isEmpty()
                            ? " • " + place.getDriverVehicle() : "";
                    binding.textDriverInfo.setText(String.format("👤 Zarezerwowane: %s%s", driver, vehicle));
                    binding.textDriverInfo.setTextColor(ContextCompat.getColor(context, R.color.status_dot_yellow));
                    binding.textDriverInfo.setVisibility(View.VISIBLE);
                } else {
                    // GREEN: Free and available
                    binding.viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.status_dot_green)));
                    binding.textDriverInfo.setVisibility(View.GONE);
                }
            }

            // 4. Card Click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });
        }
    }
}
