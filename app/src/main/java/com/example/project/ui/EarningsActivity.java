package com.example.project.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project.R;
import com.example.project.adapter.DailyBreakdownAdapter;
import com.example.project.database.entity.OwnerAnalyticsEntity;
import com.example.project.databinding.ActivityEarningsBinding;
import com.example.project.network.model.DailyBreakdownItem;
import com.example.project.viewmodel.EarningsViewModel;

import java.util.List;
import java.util.Locale;

/**
 * Activity presenting real-time financial analytics, occupancy rates, and daily revenue breakdown
 * for parking spot owners backed by Room Database and GET /api/owner/analytics.
 */
public class EarningsActivity extends AppCompatActivity {

    private ActivityEarningsBinding binding;
    private EarningsViewModel viewModel;
    private DailyBreakdownAdapter breakdownAdapter;

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

        viewModel = new ViewModelProvider(this).get(EarningsViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        if (binding == null) return;

        breakdownAdapter = new DailyBreakdownAdapter();
        binding.recyclerDailyBreakdown.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDailyBreakdown.setAdapter(breakdownAdapter);
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRefreshAnalytics.setOnClickListener(v -> {
            if (viewModel != null) {
                viewModel.refreshAnalytics();
            }
        });
    }

    private void observeViewModel() {
        // Observe persistent Room cache as Single Source of Truth
        viewModel.getAnalyticsLiveData().observe(this, this::bindAnalyticsUI);

        // Observe loading state
        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (binding == null) return;
            binding.progressAnalytics.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        // Observe network errors
        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Binds cached or freshly synchronized OwnerAnalyticsEntity values to the views.
     *
     * @param entity OwnerAnalyticsEntity instance from Room Database
     */
    private void bindAnalyticsUI(OwnerAnalyticsEntity entity) {
        if (binding == null || entity == null) {
            return;
        }

        String currency = entity.getCurrency() != null && !entity.getCurrency().isEmpty()
                ? entity.getCurrency() : "PLN";

        // 1. Total Net Earnings
        binding.textTotalEarningsSum.setText(
                String.format(Locale.getDefault(), "%.2f %s", entity.getTotalEarnings(), currency)
        );

        // 2. Gross Earnings
        String formattedGross = String.format(Locale.getDefault(), "%.2f %s", entity.getGrossEarnings(), currency);
        binding.textGrossEarnings.setText(getString(R.string.label_gross_earnings, formattedGross));

        // 3. Earnings Growth / Change badge (+12.5% or -4.0%)
        double changePercent = entity.getEarningsChangePercent();
        String changeStr = String.format(Locale.getDefault(), "%s%.1f%%", changePercent > 0 ? "+" : "", changePercent);
        binding.badgeEarningsChange.setText(changeStr);
        if (changePercent >= 0) {
            binding.badgeEarningsChange.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green_badge_bg)
            );
            binding.badgeEarningsChange.setTextColor(
                    ContextCompat.getColor(this, R.color.green_badge_text)
            );
        } else {
            binding.badgeEarningsChange.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.logout_bg)
            );
            binding.badgeEarningsChange.setTextColor(
                    ContextCompat.getColor(this, R.color.logout_text)
            );
        }

        // 4. KPI Metrics: Occupancy rate, active spots ratio, average rate
        binding.textOccupancyRate.setText(
                String.format(Locale.getDefault(), "%.1f%%", entity.getOccupancyRatePercent())
        );

        binding.textActiveSpotsRatio.setText(
                getString(R.string.format_spots_ratio, entity.getActiveSpotsCount(), entity.getTotalSpotsCount())
        );

        binding.textAverageHourlyRate.setText(
                getString(R.string.format_hourly_rate, entity.getAverageHourlyRate(), currency)
        );

        // 5. Daily Breakdown List
        List<DailyBreakdownItem> breakdownItems = entity.getDailyBreakdownList();
        if (breakdownItems.isEmpty()) {
            binding.textNoBreakdownData.setVisibility(View.VISIBLE);
            binding.recyclerDailyBreakdown.setVisibility(View.GONE);
        } else {
            binding.textNoBreakdownData.setVisibility(View.GONE);
            binding.recyclerDailyBreakdown.setVisibility(View.VISIBLE);
            breakdownAdapter.setItems(breakdownItems, currency);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshAnalytics();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
