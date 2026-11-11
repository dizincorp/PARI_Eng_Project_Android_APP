package com.example.project.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.utils.AmenityUtils;
import com.example.project.utils.MapSpotFilterHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter rendering nearby parking spots either as a compact horizontal card strip on the map
 * or as a full-width scrollable list inside the "Parkingi w pobliżu" bottom sheet.
 */
public class NearbySpotsAdapter extends RecyclerView.Adapter<NearbySpotsAdapter.SpotViewHolder> {

    public interface OnSpotClickListener {
        void onSpotClick(@NonNull ParkingPlaceEntity spot);
    }

    private final boolean horizontalStrip;
    private final OnSpotClickListener listener;
    private final List<ParkingPlaceEntity> items = new ArrayList<>();
    @Nullable
    private LatLng referencePoint;
    private final Map<String, List<AvailabilityRuleEntity>> rulesBySpot = new HashMap<>();

    public NearbySpotsAdapter(boolean horizontalStrip, @NonNull OnSpotClickListener listener) {
        this.horizontalStrip = horizontalStrip;
        this.listener = listener;
    }

    public void submitData(@Nullable List<ParkingPlaceEntity> spots,
                           @Nullable LatLng referencePoint,
                           @Nullable Map<String, List<AvailabilityRuleEntity>> rulesMap) {
        items.clear();
        if (spots != null) {
            items.addAll(spots);
        }
        this.referencePoint = referencePoint;
        rulesBySpot.clear();
        if (rulesMap != null) {
            rulesBySpot.putAll(rulesMap);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_spot, parent, false);

        if (!horizontalStrip && view instanceof MaterialCardView) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.setMarginEnd(0);
            float density = parent.getResources().getDisplayMetrics().density;
            lp.bottomMargin = Math.round(10 * density);
            view.setLayoutParams(lp);
        }

        return new SpotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
        ParkingPlaceEntity spot = items.get(position);
        holder.bind(spot, referencePoint, rulesBySpot.get(spot.getId()), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SpotViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textAddress;
        private final TextView textPrice;
        private final TextView textDistance;
        private final TextView textHours;
        private final TextView textRating;
        private final TextView textMeta;

        SpotViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_spot_title);
            textAddress = itemView.findViewById(R.id.text_spot_address);
            textPrice = itemView.findViewById(R.id.text_spot_price);
            textDistance = itemView.findViewById(R.id.text_spot_distance);
            textHours = itemView.findViewById(R.id.text_spot_hours);
            textRating = itemView.findViewById(R.id.text_spot_rating);
            textMeta = itemView.findViewById(R.id.text_spot_meta);
        }

        void bind(@NonNull ParkingPlaceEntity spot,
                  @Nullable LatLng referencePoint,
                  @Nullable List<AvailabilityRuleEntity> rules,
                  @NonNull OnSpotClickListener listener) {
            String title = spot.getTitle() != null && !spot.getTitle().trim().isEmpty()
                    ? spot.getTitle().trim()
                    : (spot.getStreet() != null ? spot.getStreet().trim() : "Parking");
            textTitle.setText(title);

            StringBuilder addr = new StringBuilder();
            if (spot.getStreet() != null && !spot.getStreet().trim().isEmpty()) {
                addr.append(spot.getStreet().trim());
            }
            if (spot.getCity() != null && !spot.getCity().trim().isEmpty()) {
                if (addr.length() > 0) addr.append(", ");
                addr.append(spot.getCity().trim());
            }
            textAddress.setText(addr.length() > 0 ? addr.toString() : "—");

            double price = spot.getPrice();
            if (Math.abs(price - Math.round(price)) < 0.01) {
                textPrice.setText(String.format(Locale.getDefault(), "%d zł/h", Math.round(price)));
            } else {
                textPrice.setText(String.format(Locale.getDefault(), "%.2f zł/h", price));
            }

            if (referencePoint != null) {
                double meters = MapSpotFilterHelper.distanceMeters(
                        referencePoint.latitude, referencePoint.longitude,
                        spot.getLatitude(), spot.getLongitude());
                textDistance.setText(MapSpotFilterHelper.formatDistance(meters));
                textDistance.setVisibility(View.VISIBLE);
            } else {
                textDistance.setVisibility(View.GONE);
            }

            String hoursSummary = MapSpotFilterHelper.formatWorkingHoursSummary(
                    rules != null ? rules : Collections.emptyList(),
                    System.currentTimeMillis());
            textHours.setText(hoursSummary);

            textRating.setText(String.format(Locale.getDefault(), "%.1f", spot.getRating()));

            StringBuilder meta = new StringBuilder();
            if (spot.getLengthCm() > 0 && spot.getWidthCm() > 0) {
                meta.append(String.format(Locale.US, "%.1f × %.1f m",
                        spot.getLengthCm() / 100.0, spot.getWidthCm() / 100.0));
                if (spot.getHeightCm() != null && spot.getHeightCm() > 0) {
                    meta.append(String.format(Locale.US, " (wys. %.1f m)", spot.getHeightCm() / 100.0));
                }
            }
            List<String> amenities = AmenityUtils.parseAmenitiesCsv(spot.getAmenitiesCsv());
            if (!amenities.isEmpty()) {
                int limit = Math.min(2, amenities.size());
                for (int i = 0; i < limit; i++) {
                    if (meta.length() > 0) meta.append(" • ");
                    meta.append(amenities.get(i));
                }
            }
            if (meta.length() > 0) {
                textMeta.setText(meta.toString());
                textMeta.setVisibility(View.VISIBLE);
            } else {
                textMeta.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onSpotClick(spot));
        }
    }
}
