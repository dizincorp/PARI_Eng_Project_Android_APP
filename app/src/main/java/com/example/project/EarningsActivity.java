package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.databinding.ActivityEarningsBinding;
import com.example.project.databinding.ItemTransactionBinding;

import java.util.List;
import java.util.Locale;

public class EarningsActivity extends AppCompatActivity {

    private ActivityEarningsBinding binding;
    private PlacesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityEarningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(PlacesViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getTransactionsLiveData().observe(this, this::updateTransactionsList);
    }

    private void updateTransactionsList(List<Transaction> transactions) {
        if (binding == null) {
            return;
        }

        binding.containerTransactions.removeAllViews();

        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        double totalSum = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Transaction tx : transactions) {
            if (tx == null) {
                continue;
            }

            totalSum += tx.getTotalPrice();

            ItemTransactionBinding itemBinding = ItemTransactionBinding.inflate(inflater, binding.containerTransactions, false);
            itemBinding.textPlaceName.setText(tx.getPlaceName() != null ? tx.getPlaceName() : "");
            
            String carInfo = tx.getCarInfo() != null ? tx.getCarInfo() : "";
            itemBinding.textCarInfo.setText(getString(R.string.format_vehicle, carInfo));
            
            String duration = tx.getDuration() != null ? tx.getDuration() : "";
            itemBinding.textDuration.setText(getString(R.string.format_duration, duration));
            
            String date = tx.getDate() != null ? tx.getDate() : "";
            itemBinding.textDate.setText(getString(R.string.format_date, date));
            
            itemBinding.textTotalPrice.setText(String.format(Locale.getDefault(), "%.2f PLN", tx.getTotalPrice()));

            binding.containerTransactions.addView(itemBinding.getRoot());
        }

        binding.textTotalEarningsSum.setText(String.format(Locale.getDefault(), "%.2f PLN", totalSum));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}