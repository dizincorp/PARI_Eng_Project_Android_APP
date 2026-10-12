package com.example.project;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.databinding.ActivityPlacesBinding;

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
    }

    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

        binding.btnAddPlace.setOnClickListener(v -> {
            Intent intent = new Intent(PlacesActivity.this, AddPlaceActivity.class);
            startActivity(intent);
        });

        binding.btnAvailability.setOnClickListener(v -> {
            Intent intent = new Intent(PlacesActivity.this, AvailabilityCalendarActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}