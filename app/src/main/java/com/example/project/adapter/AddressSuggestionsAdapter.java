package com.example.project.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.databinding.ItemAddressSuggestionBinding;
import com.example.project.utils.MapSpotFilterHelper;
import com.example.project.viewmodel.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter displaying multi-city address autocomplete suggestions
 * with geolocation-first section grouping and a prominent city badge per row.
 */
public class AddressSuggestionsAdapter extends RecyclerView.Adapter<AddressSuggestionsAdapter.SuggestionViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(@NonNull SearchViewModel.AddressSuggestion suggestion);
    }

    private final List<SearchViewModel.AddressSuggestion> items = new ArrayList<>();
    private final OnSuggestionClickListener clickListener;

    public AddressSuggestionsAdapter(@NonNull OnSuggestionClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(@NonNull List<SearchViewModel.AddressSuggestion> suggestions) {
        items.clear();
        items.addAll(suggestions);
        notifyDataSetChanged();
    }

    public void clear() {
        if (items.isEmpty()) {
            return;
        }
        items.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddressSuggestionBinding binding = ItemAddressSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SuggestionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        SearchViewModel.AddressSuggestion item = items.get(position);
        boolean showNearbyHeader = item.isNearUserLocation() && position == 0;
        boolean showAllHeader = !item.isNearUserLocation()
                && (position == 0 || items.get(position - 1).isNearUserLocation());
        holder.bind(item, showNearbyHeader, showAllHeader, clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final ItemAddressSuggestionBinding binding;

        SuggestionViewHolder(@NonNull ItemAddressSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull SearchViewModel.AddressSuggestion item,
                  boolean showNearbyHeader,
                  boolean showAllHeader,
                  @NonNull OnSuggestionClickListener listener) {
            Context context = itemView.getContext();

            if (showNearbyHeader) {
                binding.textSuggestionSection.setVisibility(View.VISIBLE);
                binding.textSuggestionSection.setText(R.string.search_suggestions_section_nearby);
            } else if (showAllHeader) {
                binding.textSuggestionSection.setVisibility(View.VISIBLE);
                binding.textSuggestionSection.setText(R.string.search_suggestions_section_all);
            } else {
                binding.textSuggestionSection.setVisibility(View.GONE);
            }

            binding.textSuggestionStreet.setText(item.getPrimaryText());

            if (!item.getSecondaryText().isEmpty()) {
                binding.textSuggestionCityMeta.setVisibility(View.VISIBLE);
                binding.textSuggestionCityMeta.setText(item.getSecondaryText());
            } else {
                binding.textSuggestionCityMeta.setVisibility(View.GONE);
            }

            if (!item.getCity().isEmpty()) {
                binding.textSuggestionCityBadge.setVisibility(View.VISIBLE);
                binding.textSuggestionCityBadge.setText(item.getCity());
            } else {
                binding.textSuggestionCityBadge.setVisibility(View.GONE);
            }

            boolean near = item.isNearUserLocation();
            binding.textSuggestionCityBadge.setBackgroundResource(
                    near ? R.drawable.bg_badge_emerald : R.drawable.bg_badge_slate);
            binding.textSuggestionCityBadge.setTextColor(ContextCompat.getColor(
                    context, near ? R.color.brand_emerald_800 : R.color.brand_slate_700));
            ImageViewCompat.setImageTintList(binding.iconSuggestionPin, ColorStateList.valueOf(
                    ContextCompat.getColor(context, near ? R.color.brand_emerald_600 : R.color.brand_slate_500)));

            double distanceMeters = item.getDistanceMeters();
            if (!Double.isNaN(distanceMeters) && distanceMeters >= 0.0) {
                binding.textSuggestionDistance.setVisibility(View.VISIBLE);
                String formattedDist = MapSpotFilterHelper.formatDistance(distanceMeters);
                if (near) {
                    binding.textSuggestionDistance.setText(
                            context.getString(R.string.search_suggestions_near_you, formattedDist));
                    binding.textSuggestionDistance.setTextColor(
                            ContextCompat.getColor(context, R.color.brand_emerald_700));
                } else {
                    binding.textSuggestionDistance.setText(formattedDist);
                    binding.textSuggestionDistance.setTextColor(
                            ContextCompat.getColor(context, R.color.brand_slate_500));
                }
            } else {
                binding.textSuggestionDistance.setVisibility(View.GONE);
            }

            binding.rowSuggestionContent.setOnClickListener(v -> listener.onSuggestionClick(item));
        }
    }
}
