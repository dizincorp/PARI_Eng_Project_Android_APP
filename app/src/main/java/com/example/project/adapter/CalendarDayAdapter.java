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
import com.example.project.databinding.ItemCalendarDayBinding;
import com.example.project.model.CalendarDay;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter rendering the 7-column calendar month grid.
 * Dynamically colors cells according to their availability status:
 * Green (Free), Yellow (Reserved), Red (Blocked), Gray (Past).
 */
public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(@NonNull CalendarDay day);
    }

    private final List<CalendarDay> days = new ArrayList<>();
    private OnDayClickListener onDayClickListener;

    public void setDays(@NonNull List<CalendarDay> newDays) {
        this.days.clear();
        this.days.addAll(newDays);
        notifyDataSetChanged();
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }

    public void setSelectedDateString(@NonNull String dateStr) {
        for (int i = 0; i < days.size(); i++) {
            CalendarDay day = days.get(i);
            boolean shouldBeSelected = day.isCurrentMonth() && dateStr.equals(day.getDateString());
            if (day.isSelected() != shouldBeSelected) {
                day.setSelected(shouldBeSelected);
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(days.get(position), onDayClickListener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        DayViewHolder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CalendarDay day, OnDayClickListener listener) {
            Context context = itemView.getContext();
            float density = context.getResources().getDisplayMetrics().density;

            if (!day.isCurrentMonth()) {
                binding.textDayNumber.setText(String.valueOf(day.getDayOfMonth()));
                binding.textDayNumber.setTextColor(ContextCompat.getColor(context, R.color.brand_slate_300));
                binding.cardDay.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
                binding.cardDay.setStrokeColor(ContextCompat.getColor(context, R.color.brand_slate_100));
                binding.cardDay.setStrokeWidth((int) (1 * density));
                binding.dotStatus.setVisibility(View.GONE);
                binding.cardDay.setAlpha(0.25f);
                binding.cardDay.setOnClickListener(null);
                binding.cardDay.setClickable(false);
                return;
            }

            binding.cardDay.setAlpha(1.0f);
            binding.cardDay.setClickable(true);
            binding.textDayNumber.setText(String.valueOf(day.getDayOfMonth()));

            int bgColor;
            int strokeColor;
            int textColor;
            int dotColor;
            boolean showDot = true;

            switch (day.getStatus()) {
                case BLOCKED_RED:
                    bgColor = ContextCompat.getColor(context, R.color.brand_rose_50);
                    strokeColor = ContextCompat.getColor(context, R.color.brand_rose_500);
                    textColor = ContextCompat.getColor(context, R.color.brand_rose_700);
                    dotColor = ContextCompat.getColor(context, R.color.brand_rose_600);
                    break;
                case RESERVED_YELLOW:
                    bgColor = ContextCompat.getColor(context, R.color.brand_amber_50);
                    strokeColor = ContextCompat.getColor(context, R.color.brand_amber_600);
                    textColor = ContextCompat.getColor(context, R.color.brand_amber_800);
                    dotColor = ContextCompat.getColor(context, R.color.brand_amber_600);
                    break;
                case FREE_GREEN:
                    bgColor = ContextCompat.getColor(context, R.color.brand_emerald_50);
                    strokeColor = ContextCompat.getColor(context, R.color.brand_emerald_500);
                    textColor = ContextCompat.getColor(context, R.color.brand_emerald_800);
                    dotColor = ContextCompat.getColor(context, R.color.brand_emerald_600);
                    break;
                case PAST:
                default:
                    bgColor = ContextCompat.getColor(context, R.color.brand_slate_50);
                    strokeColor = ContextCompat.getColor(context, R.color.brand_slate_200);
                    textColor = ContextCompat.getColor(context, R.color.brand_slate_400);
                    dotColor = textColor;
                    showDot = false;
                    break;
            }

            binding.cardDay.setCardBackgroundColor(bgColor);
            binding.textDayNumber.setTextColor(textColor);

            if (showDot) {
                binding.dotStatus.setVisibility(View.VISIBLE);
                binding.dotStatus.setBackgroundTintList(ColorStateList.valueOf(dotColor));
            } else {
                binding.dotStatus.setVisibility(View.GONE);
            }

            // Highlighting selection and today's date
            if (day.isSelected()) {
                binding.cardDay.setStrokeColor(ContextCompat.getColor(context, R.color.brand_slate_950));
                binding.cardDay.setStrokeWidth((int) (2.5f * density));
                binding.cardDay.setCardElevation(4f * density);
            } else if (day.isToday()) {
                binding.cardDay.setStrokeColor(ContextCompat.getColor(context, R.color.brand_slate_700));
                binding.cardDay.setStrokeWidth((int) (2.0f * density));
                binding.cardDay.setCardElevation(1f * density);
            } else {
                binding.cardDay.setStrokeColor(strokeColor);
                binding.cardDay.setStrokeWidth((int) (1.2f * density));
                binding.cardDay.setCardElevation(0f);
            }

            binding.cardDay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
            });
        }
    }
}
