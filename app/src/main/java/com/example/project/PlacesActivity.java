package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.databinding.ActivityPlacesBinding;
import com.example.project.databinding.ItemMyPlaceBinding;

import java.util.List;
import java.util.Locale;

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
    }

    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

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

        viewModel.getMyPlacesLiveData().observe(this, this::updateMyPlacesList);
    }

    private void updateMyPlacesList(List<ParkingPlace> places) {
        if (binding == null) {
            return;
        }

        binding.containerMyPlaces.removeAllViews();

        if (places == null || places.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (ParkingPlace p : places) {
            if (p == null) {
                continue;
            }

            ItemMyPlaceBinding itemBinding = ItemMyPlaceBinding.inflate(inflater, binding.containerMyPlaces, false);

            itemBinding.textPlaceName.setText(p.getTitle());

            String priceStr = String.format(Locale.getDefault(), "%.2f zł/h", p.getPrice());
            String subInfo = p.getStreet() + " • " + priceStr + " • Czas: " + p.getAccessTime();
            itemBinding.textPlaceSub.setText(subInfo);

            if (p.isOccupied()) {
                String untilText = (p.getOccupiedUntil() != null && !p.getOccupiedUntil().isEmpty())
                        ? getString(R.string.format_occupied_until, p.getOccupiedUntil())
                        : getString(R.string.status_occupied);
                itemBinding.textPlaceStatus.setText(untilText);
                itemBinding.textPlaceStatus.setBackgroundTintList(getColorStateList(R.color.blue_badge_bg));
                itemBinding.textPlaceStatus.setTextColor(getColor(R.color.blue_badge_text));
            } else {
                itemBinding.textPlaceStatus.setText(R.string.status_free);
                itemBinding.textPlaceStatus.setBackgroundTintList(getColorStateList(R.color.green_badge_bg));
                itemBinding.textPlaceStatus.setTextColor(getColor(R.color.green_badge_text));
            }

            // Click to EDIT information of this place
            itemBinding.getRoot().setOnClickListener(v -> {
                Intent editIntent = new Intent(PlacesActivity.this, AddPlaceActivity.class);
                editIntent.putExtra(AddPlaceActivity.EXTRA_PLACE_ID, p.getId());
                startActivity(editIntent);
            });

            binding.containerMyPlaces.addView(itemBinding.getRoot());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}