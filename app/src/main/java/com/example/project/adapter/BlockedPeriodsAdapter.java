package com.example.project.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.databinding.ItemBlockedPeriodBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView Adapter for rendering calendar blocked periods of a parking spot.
 * Employs View Binding and provides an unblock action callback.
 */
public class BlockedPeriodsAdapter extends RecyclerView.Adapter<BlockedPeriodsAdapter.BlockedPeriodViewHolder> {

    /**
     * Interface callback for unblocking (deleting) a calendar blocked period.
     */
    public interface OnUnblockClickListener {
        /**
         * Invoked when the user clicks the unblock button on a blocked period.
         *
         * @param block The blocked period entity to delete
         */
        void onUnblockClick(@NonNull BlockedPeriodEntity block);
    }

    private final List<BlockedPeriodEntity> blockedList = new ArrayList<>();
    private OnUnblockClickListener unblockClickListener;

    /**
     * Default constructor for BlockedPeriodsAdapter.
     */
    public BlockedPeriodsAdapter() {
    }

    /**
     * Sets the unblock click listener.
     *
     * @param listener Callback listener
     */
    public void setOnUnblockClickListener(@Nullable OnUnblockClickListener listener) {
        this.unblockClickListener = listener;
    }

    /**
     * Updates the underlying collection of blocked periods and refreshes UI.
     *
     * @param newList New list of {@link BlockedPeriodEntity}
     */
    public void setBlockedPeriods(@Nullable List<BlockedPeriodEntity> newList) {
        blockedList.clear();
        if (newList != null) {
            blockedList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BlockedPeriodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlockedPeriodBinding binding = ItemBlockedPeriodBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new BlockedPeriodViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockedPeriodViewHolder holder, int position) {
        BlockedPeriodEntity item = blockedList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return blockedList.size();
    }

    /**
     * ViewHolder for blocked period items.
     */
    class BlockedPeriodViewHolder extends RecyclerView.ViewHolder {

        private final ItemBlockedPeriodBinding binding;

        BlockedPeriodViewHolder(@NonNull ItemBlockedPeriodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull BlockedPeriodEntity item) {
            String startStr = formatTimestamp(item.getStart());
            String endStr = formatTimestamp(item.getEnd());
            String timeRange = startStr + " - " + endStr;
            binding.textBlockTimeRange.setText(timeRange);

            String reason = item.getReason();
            if (reason != null && !reason.trim().isEmpty()) {
                binding.textBlockReason.setText(reason);
                binding.textBlockReason.setVisibility(View.VISIBLE);
            } else {
                binding.textBlockReason.setVisibility(View.GONE);
            }

            binding.btnUnblock.setOnClickListener(v -> {
                if (unblockClickListener != null) {
                    unblockClickListener.onUnblockClick(item);
                }
            });
        }

        private String formatTimestamp(String iso) {
            if (iso == null || iso.trim().isEmpty()) {
                return "";
            }
            // Simple clean fallback if ISO formatted: 2026-11-01T15:10:00Z -> 2026-11-01 15:10
            String cleaned = iso.replace("Z", "").replace("T", " ");
            if (cleaned.length() >= 16) {
                return cleaned.substring(0, 16);
            }
            return cleaned;
        }
    }
}
