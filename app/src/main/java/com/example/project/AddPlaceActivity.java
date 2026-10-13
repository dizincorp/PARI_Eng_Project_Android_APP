package com.example.project;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.databinding.ActivityAddPlaceBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddPlaceActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "EXTRA_PLACE_ID";

    private ActivityAddPlaceBinding binding;
    private AddPlaceViewModel viewModel;
    private String currentPlaceId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddPlaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AddPlaceViewModel.class);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_PLACE_ID)) {
            currentPlaceId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        }

        setupClickListeners();
        observeViewModel();

        if (currentPlaceId != null) {
            viewModel.loadPlaceForEditing(currentPlaceId);
        }
    }

    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSubmitPlace.setOnClickListener(v -> {
            String street = binding.editStreet.getText() != null ? binding.editStreet.getText().toString() : "";
            String city = binding.editCity.getText() != null ? binding.editCity.getText().toString() : "";
            String postalCode = binding.editPostalCode.getText() != null ? binding.editPostalCode.getText().toString() : "";
            String placeNumber = binding.editPlaceNumber.getText() != null ? binding.editPlaceNumber.getText().toString() : "";
            String floor = binding.editFloor.getText() != null ? binding.editFloor.getText().toString() : "";
            String accessTime = binding.editAccessTime.getText() != null ? binding.editAccessTime.getText().toString() : "";
            String price = binding.editPrice.getText() != null ? binding.editPrice.getText().toString() : "";

            List<String> amenities = new ArrayList<>();
            if (binding.chipMonitoring.isChecked()) amenities.add("Monitoring");
            if (binding.chipLighting.isChecked()) amenities.add("Oświetlenie");
            if (binding.chipEv.isChecked()) amenities.add("EV Charging");
            if (binding.chipUnderground.isChecked()) amenities.add("Podziemny");

            viewModel.savePlace(currentPlaceId, street, city, postalCode, placeNumber, floor, accessTime, price, amenities);
        });

        binding.btnUploadPhotos.setOnClickListener(v ->
                Toast.makeText(this, R.string.prompt_add_photos, Toast.LENGTH_SHORT).show()
        );
    }

    private void observeViewModel() {
        viewModel.getEditingPlaceLiveData().observe(this, place -> {
            if (binding == null || place == null) {
                return;
            }

            binding.textToolbarTitle.setText(R.string.title_edit_place);
            binding.btnSubmitPlace.setText(R.string.title_edit_place);

            if (place.getStreet() != null) binding.editStreet.setText(place.getStreet());
            if (place.getCity() != null) binding.editCity.setText(place.getCity());
            if (place.getPostalCode() != null) binding.editPostalCode.setText(place.getPostalCode());
            if (place.getPlaceNumber() != null) binding.editPlaceNumber.setText(place.getPlaceNumber());
            if (place.getFloor() != null) binding.editFloor.setText(place.getFloor());
            binding.editAccessTime.setText(place.getAccessTime());
            binding.editPrice.setText(String.format(Locale.US, "%.2f", place.getPrice()));

            if (place.getAmenities() != null) {
                binding.chipMonitoring.setChecked(place.getAmenities().contains("Monitoring"));
                binding.chipLighting.setChecked(place.getAmenities().contains("Oświetlenie"));
                binding.chipEv.setChecked(place.getAmenities().contains("EV Charging") || place.getAmenities().contains("Ładowanie EV"));
                binding.chipUnderground.setChecked(place.getAmenities().contains("Podziemny"));
            }
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.msg_place_added, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessageResId().observe(this, resId -> {
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}