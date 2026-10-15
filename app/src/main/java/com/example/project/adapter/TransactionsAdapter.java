package com.example.project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.databinding.ItemTransactionBinding;
import com.example.project.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying earnings transaction history in EarningsActivity.
 */
public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {

    private final List<Transaction> transactions = new ArrayList<>();

    public void setTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        if (newTransactions != null) {
            this.transactions.addAll(newTransactions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        if (position >= 0 && position < transactions.size()) {
            holder.bind(transactions.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {

        private final ItemTransactionBinding binding;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Transaction tx) {
            if (tx == null) return;

            Context context = itemView.getContext();
            binding.textPlaceName.setText(tx.getPlaceName() != null ? tx.getPlaceName() : "");

            String carInfo = tx.getCarInfo() != null ? tx.getCarInfo() : "";
            binding.textCarInfo.setText(context.getString(R.string.format_vehicle, carInfo));

            String duration = tx.getDuration() != null ? tx.getDuration() : "";
            binding.textDuration.setText(context.getString(R.string.format_duration, duration));

            String date = tx.getDate() != null ? tx.getDate() : "";
            binding.textDate.setText(context.getString(R.string.format_date, date));

            binding.textTotalPrice.setText(String.format(Locale.getDefault(), "%.2f PLN", tx.getTotalPrice()));
        }
    }
}