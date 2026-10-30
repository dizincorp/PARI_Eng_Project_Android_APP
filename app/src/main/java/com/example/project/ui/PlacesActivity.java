package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityPlacesBinding;
import com.example.project.databinding.ItemMyPlaceBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Transaction;
import com.example.project.repository.OwnerAnalyticsRepository;
import com.example.project.viewmodel.PlacesViewModel;

import java.util.List;
import java.util.Locale;

/**
 * Places activity displaying owner's dashboard, earnings summary, and list of owned parking spots
 * with real-time status indicators (green/yellow/red/gray), active/inactive slider switch,
 * and driver/vehicle booking details.
 */
public class PlacesActivity extends BaseActivity {

    private ActivityPlacesBinding binding;
    private PlacesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPlacesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_places);

        viewModel = new ViewModelProvider(this).get(PlacesViewModel.class);

        setupClickListeners();
        observeViewModel();

        // Refresh owner spots from REST backend (GET /api/spots/mine)
        viewModel.refreshPlaces();
        OwnerAnalyticsRepository.getInstance(this).fetchOwnerAnalytics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshPlaces();
        }
        OwnerAnalyticsRepository.getInstance(this).fetchOwnerAnalytics();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.cardEarnings.setOnClickListener(v -> {
            Intent intent = new Intent(PlacesActivity.this, EarningsActivity.class);
            startActivity(intent);
        });

        binding.btnAddPlace.setOnClickListener(v -> {
            Intent intent = new Intent(PlacesActivity.this, AddPlaceActivity.class);
            startActivity(intent);
        });

        binding.btnAvailability.setOnClickListener(v -> {
            Intent intent = new Intent(PlacesActivity.this, AvailabilityCalendarActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getGreetingLiveData().observe(this, greeting -> {
            if (binding != null) {
                binding.textGreeting.setText(greeting);
            }
        });

        viewModel.getSubtitleLiveData().observe(this, subtitle -> {
            if (binding != null) {
                binding.textSubtitle.setText(subtitle);
            }
        });

        viewModel.getTransactionsLiveData().observe(this, this::updateEarningsUI);

        // Observe owner analytics to show real-time monthly earnings
        OwnerAnalyticsRepository.getInstance(this).getCachedAnalytics()
                .observe(this, analytics -> {
                    if (binding != null && analytics != null && analytics.getTotalEarnings() > 0) {
                        String cur = analytics.getCurrency() != null ? analytics.getCurrency() : "PLN";
                        binding.textEarningsValue.setText(String.format(Locale.getDefault(), "%.2f %s", analytics.getTotalEarnings(), cur));
                    }
                });

        // Observe owner's parking spots with dynamic slider and 4-state indicator
        viewModel.getMyPlacesLiveData().observe(this, places -> {
            if (binding == null) return;

            binding.containerMyPlaces.removeAllViews();

            if (places == null || places.isEmpty()) {
                android.widget.TextView emptyView = new android.widget.TextView(this);
                emptyView.setText(R.string.msg_no_places);
                emptyView.setTextSize(14);
                emptyView.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
                emptyView.setPadding(16, 24, 16, 24);
                binding.containerMyPlaces.addView(emptyView);
                return;
            }

            for (ParkingPlace p : places) {
                if (p == null) continue;

                ItemMyPlaceBinding itemBinding =
                        ItemMyPlaceBinding.inflate(getLayoutInflater(), binding.containerMyPlaces, false);

                // 1. Title and address subtitle
                itemBinding.textPlaceName.setText(p.getTitle() != null ? p.getTitle() : "Miejsce parkingowe");
                String priceStr = String.format(Locale.getDefault(), "%.2f zł/h", p.getPrice());
                String subInfo = (p.getStreet() != null ? p.getStreet() : "Warszawa")
                        + " • " + priceStr
                        + " • Czas: " + (p.getAccessTime() != null ? p.getAccessTime() : "24/7");
                itemBinding.textPlaceSub.setText(subInfo);

                // 2. Slider (MaterialSwitch) configuration
                itemBinding.switchSpotActive.setOnCheckedChangeListener(null);
                boolean isActive = p.isActive();
                itemBinding.switchSpotActive.setChecked(isActive);
                itemBinding.switchSpotActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        viewModel.activateSpot(p.getId());
                    } else {
                        viewModel.deactivateSpot(p.getId());
                    }
                });

                // 3. Card Visual State & Status Dot Indicators:
                // - Зеленый: место свободно и активно
                // - Желтый: место уже кто-то зарезервировал (водитель и машина отображаются)
                // - Красный: место занято (водитель сделал check-in)
                // - Серый: место не активно (slider OFF)
                if (!isActive) {
                    itemBinding.cardPlaceRoot.setAlpha(0.60f);
                    itemBinding.cardPlaceRoot.setCardBackgroundColor(getColor(R.color.inactive_card_bg));
                    itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.status_dot_gray));
                    itemBinding.textDriverInfo.setVisibility(View.GONE);
                } else {
                    itemBinding.cardPlaceRoot.setAlpha(1.0f);
                    itemBinding.cardPlaceRoot.setCardBackgroundColor(getColor(R.color.card_background));

                    if (p.isCheckedIn()) {
                        // RED: Driver checked in (parked now)
                        itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.status_dot_red));
                        String driver = p.getDriverName() != null && !p.getDriverName().isEmpty()
                                ? p.getDriverName() : "Kierowca";
                        String vehicle = p.getDriverVehicle() != null && !p.getDriverVehicle().isEmpty()
                                ? p.getDriverVehicle() : "Pojazd na miejscu";
                        itemBinding.textDriverInfo.setText(String.format("🚗 Parkuje: %s • %s", driver, vehicle));
                        itemBinding.textDriverInfo.setTextColor(getColor(R.color.status_dot_red));
                        itemBinding.textDriverInfo.setVisibility(View.VISIBLE);
                    } else if (p.isOccupied() || p.isReserved() || (p.getDriverName() != null && !p.getDriverName().isEmpty())) {
                        // YELLOW: Reserved by a driver
                        itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.status_dot_yellow));
                        String driver = p.getDriverName() != null && !p.getDriverName().isEmpty()
                                ? p.getDriverName() : "Zarezerwowane";
                        String vehicle = p.getDriverVehicle() != null && !p.getDriverVehicle().isEmpty()
                                ? " • " + p.getDriverVehicle() : "";
                        itemBinding.textDriverInfo.setText(String.format("👤 Zarezerwowane: %s%s", driver, vehicle));
                        itemBinding.textDriverInfo.setTextColor(getColor(R.color.status_dot_yellow));
                        itemBinding.textDriverInfo.setVisibility(View.VISIBLE);
                    } else {
                        // GREEN: Free & Active
                        itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.status_dot_green));
                        itemBinding.textDriverInfo.setVisibility(View.GONE);
                    }
                }

                // 4. Click opens editing screen
                itemBinding.getRoot().setOnClickListener(v -> {
                    Intent editIntent = new Intent(PlacesActivity.this, AddPlaceActivity.class);
                    editIntent.putExtra(AddPlaceActivity.EXTRA_PLACE_ID, p.getId());
                    startActivity(editIntent);
                });

                binding.containerMyPlaces.addView(itemBinding.getRoot());
            }
        });

        // Observe Toast messages from spot lifecycle operations
        viewModel.getToastMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe network errors
        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEarningsUI(List<Transaction> transactions) {
        if (binding == null) return;

        double total = 0.0;
        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx != null) {
                    total += tx.getTotalPrice();
                }
            }
        }

        binding.textEarningsValue.setText(String.format(Locale.getDefault(), "%.2f PLN", total));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
