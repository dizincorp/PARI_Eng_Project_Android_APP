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
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.databinding.ActivityPlacesBinding;
import com.example.project.databinding.ItemMyPlaceBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.model.Transaction;
import com.example.project.repository.OwnerAnalyticsRepository;
import com.example.project.utils.AmenityUtils;
import com.example.project.viewmodel.PlacesViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Places activity displaying owner's dashboard, earnings summary, and list of owned parking spots
 * with real-time status indicators (green/yellow/red/gray), active/inactive slider switch,
 * calendar blocked intervals notice ("18:00 - 20:00 zablokowane"), amenities, and driver booking details.
 */
public class PlacesActivity extends BaseActivity {

    private ActivityPlacesBinding binding;
    private PlacesViewModel viewModel;
    private final Map<String, List<BlockedPeriodEntity>> spotBlockedMap = new HashMap<>();

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

        // Observe calendar blocked periods to show on owner place items
        viewModel.getBlockedPeriodsLiveData().observe(this, blocks -> {
            spotBlockedMap.clear();
            if (blocks != null) {
                for (BlockedPeriodEntity b : blocks) {
                    if (b == null || b.getSpotId() == null || b.isExpired()) continue;
                    List<BlockedPeriodEntity> list = spotBlockedMap.get(b.getSpotId());
                    if (list == null) {
                        list = new ArrayList<>();
                        spotBlockedMap.put(b.getSpotId(), list);
                    }
                    list.add(b);
                }
            }
            List<ParkingPlace> currentPlaces = viewModel.getMyPlacesLiveData().getValue();
            if (currentPlaces != null) {
                renderPlacesList(currentPlaces);
            }
        });

        // Observe owner's parking spots with dynamic slider and 4-state indicator
        viewModel.getMyPlacesLiveData().observe(this, this::renderPlacesList);

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

    /**
     * Renders the list of owner parking spots into the container view with status indicators,
     * blocked hours badges, amenities chips, description, and action callbacks.
     *
     * @param places List of ParkingPlace items owned by user
     */
    private void renderPlacesList(List<ParkingPlace> places) {
        if (binding == null) return;

        binding.containerMyPlaces.removeAllViews();

        if (places == null || places.isEmpty()) {
            android.widget.TextView emptyView = new android.widget.TextView(this);
            emptyView.setText(R.string.msg_no_places);
            emptyView.setTextSize(14);
            emptyView.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_slate_500));
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

            // Amenities under title
            itemBinding.chipGroupItemAmenities.removeAllViews();
            boolean hasAmenities = false;
            List<String> amenities = p.getAmenities();
            if (amenities != null && !amenities.isEmpty()) {
                for (String amenity : amenities) {
                    String label = AmenityUtils.formatAmenity(amenity);
                    if (!label.isEmpty()) {
                        Chip chip = new Chip(this);
                        chip.setText(label);
                        chip.setTextSize(11f);
                        chip.setClickable(false);
                        chip.setCheckable(false);
                        chip.setChipBackgroundColorResource(R.color.brand_slate_100);
                        chip.setChipStrokeColorResource(R.color.brand_slate_200);
                        chip.setChipStrokeWidth(1f);
                        chip.setTextColor(getColor(R.color.brand_slate_700));
                        itemBinding.chipGroupItemAmenities.addView(chip);
                        hasAmenities = true;
                    }
                }
            }
            itemBinding.chipGroupItemAmenities.setVisibility(hasAmenities ? View.VISIBLE : View.GONE);

            // Description under amenities
            String description = p.getDescription();
            if ((description == null || description.trim().isEmpty()) && p.getAccessInstructions() != null && !p.getAccessInstructions().trim().isEmpty()) {
                description = p.getAccessInstructions();
            }
            if (description != null && !description.trim().isEmpty()) {
                itemBinding.textPlaceDescription.setText(description.trim());
                itemBinding.textPlaceDescription.setVisibility(View.VISIBLE);
            } else {
                itemBinding.textPlaceDescription.setVisibility(View.GONE);
            }

            // Blocked periods notice for owner
            List<BlockedPeriodEntity> blocks = spotBlockedMap.get(p.getId());
            StringBuilder sb = new StringBuilder();
            if (blocks != null && !blocks.isEmpty()) {
                for (BlockedPeriodEntity b : blocks) {
                    if (b == null || b.isExpired()) continue;
                    if (sb.length() > 0) sb.append("\n");
                    String reason = (b.getReason() != null && !b.getReason().trim().isEmpty())
                            ? " (" + b.getReason().trim() + ")" : "";
                    sb.append("⛔ ").append(b.getFormattedBlockedNotice()).append(reason);
                }
            }
            if (sb.length() > 0) {
                itemBinding.textItemBlockedInfo.setText(sb.toString());
                itemBinding.textItemBlockedInfo.setVisibility(View.VISIBLE);
            } else {
                itemBinding.textItemBlockedInfo.setVisibility(View.GONE);
            }

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
            if (!isActive) {
                itemBinding.cardPlaceRoot.setAlpha(0.60f);
                itemBinding.cardPlaceRoot.setCardBackgroundColor(getColor(R.color.inactive_card_bg));
                itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.brand_slate_400));
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
                    itemBinding.textDriverInfo.setTextColor(getColor(R.color.brand_rose_600));
                    itemBinding.textDriverInfo.setVisibility(View.VISIBLE);
                } else if (p.isOccupied() || p.isReserved() || (p.getDriverName() != null && !p.getDriverName().isEmpty())) {
                    // YELLOW: Reserved by a driver
                    itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.status_dot_yellow));
                    String driver = p.getDriverName() != null && !p.getDriverName().isEmpty()
                            ? p.getDriverName() : "Zarezerwowane";
                    String vehicle = p.getDriverVehicle() != null && !p.getDriverVehicle().isEmpty()
                            ? " • " + p.getDriverVehicle() : "";
                    itemBinding.textDriverInfo.setText(String.format("👤 Zarezerwowane: %s%s", driver, vehicle));
                    itemBinding.textDriverInfo.setTextColor(getColor(R.color.brand_amber_600));
                    itemBinding.textDriverInfo.setVisibility(View.VISIBLE);
                } else {
                    // GREEN: Free & Active
                    itemBinding.viewStatusDot.setBackgroundTintList(getColorStateList(R.color.brand_emerald_500));
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