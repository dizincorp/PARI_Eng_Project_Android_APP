package com.example.project.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.databinding.ItemDailyBreakdownBinding;
import com.example.project.network.model.DailyBreakdownItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom RecyclerView Adapter for rendering the owner's daily revenue breakdown items.
 * Employs View Binding and provides defensive null checks.
 */
public class DailyBreakdownAdapter extends RecyclerView.Adapter<DailyBreakdownAdapter.DailyBreakdownViewHolder> {

    private final List<DailyBreakdownItem> items = new ArrayList<>();
    private String currency = "PLN";

    /**
     * Default constructor for DailyBreakdownAdapter.
     */
    public DailyBreakdownAdapter() {
    }

    /**
     * Updates the dataset of daily breakdown items and the display currency code.
     *
     * @param newItems List of daily breakdown entries
     * @param currency Currency code (e.g., "PLN")
     */
    public void setItems(@Nullable List<DailyBreakdownItem> newItems, @Nullable String currency) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        if (currency != null && !currency.trim().isEmpty()) {
            this.currency = currency.trim();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DailyBreakdownViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDailyBreakdownBinding binding = ItemDailyBreakdownBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new DailyBreakdownViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyBreakdownViewHolder holder, int position) {
        DailyBreakdownItem item = items.get(position);
        holder.bind(item, currency);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder managing daily breakdown row binding.
     */
    static class DailyBreakdownViewHolder extends RecyclerView.ViewHolder {

        private final ItemDailyBreakdownBinding binding;

        public DailyBreakdownViewHolder(@NonNull ItemDailyBreakdownBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@Nullable DailyBreakdownItem item, @NonNull String currency) {
            if (item == null) return;

            String day = item.getDay();
            if (day != null && !day.trim().isEmpty()) {
                String badgeText = day.trim();
                if (badgeText.length() > 3) {
                    badgeText = badgeText.substring(0, 3);
                }
                binding.textDayBadge.setText(badgeText.toUpperCase(Locale.getDefault()));
                binding.textDayName.setText(day.trim());
            } else {
                binding.textDayBadge.setText("DZ.");
                binding.textDayName.setText("Dzień");
            }

            String date = item.getDate();
            binding.textBreakdownDate.setText(date != null && !date.trim().isEmpty() ? date.trim() : "—");

            binding.textBreakdownAmount.setText(
                    String.format(Locale.getDefault(), "%.2f %s", item.getAmount(), currency)
            );
        }
    }
}
