package com.example.project.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
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
 * RecyclerView Adapter for displaying parking places list in PlacesActivity.
 */
public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    /**
     * Interface for item click listener.
     */
    public interface OnPlaceClickListener {
        void onPlaceClick(ParkingPlace place);
    }

    private final List<ParkingPlace> places = new ArrayList<>();
    private OnPlaceClickListener listener;

    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

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

            binding.textPlaceName.setText(place.getTitle());

            Context context = itemView.getContext();
            String priceStr = String.format(Locale.getDefault(), "%.2f zł/h", place.getPrice());
            String subInfo = place.getStreet() + " • " + priceStr + " • Czas: " + place.getAccessTime();
            binding.textPlaceSub.setText(subInfo);

            if (place.isOccupied()) {
                String untilText = (place.getOccupiedUntil() != null && !place.getOccupiedUntil().isEmpty())
                        ? context.getString(R.string.format_occupied_until, place.getOccupiedUntil())
                        : context.getString(R.string.status_occupied);
                binding.textPlaceStatus.setText(untilText);
                binding.textPlaceStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.blue_badge_bg)));
                binding.textPlaceStatus.setTextColor(ContextCompat.getColor(context, R.color.blue_badge_text));
            } else {
                binding.textPlaceStatus.setText(R.string.status_free);
                binding.textPlaceStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.green_badge_bg)));
                binding.textPlaceStatus.setTextColor(ContextCompat.getColor(context, R.color.green_badge_text));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });
        }
    }
}