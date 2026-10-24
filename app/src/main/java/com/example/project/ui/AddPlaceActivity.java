package com.example.project.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityAddPlaceBinding;
import com.example.project.repository.AppRepository;
import com.example.project.viewmodel.AddPlaceViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Activity for creating or updating a parking spot with photo selection, TimePicker,
 * Google Maps Geocoding integration, and full alignment with POST /api/spots.
 */
public class AddPlaceActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "EXTRA_PLACE_ID";

    private ActivityAddPlaceBinding binding;
    private AddPlaceViewModel viewModel;
    private String currentPlaceId = null;
    private String selectedPhotoUri = "";

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedPhotoUri = imageUri.toString();
                        Toast.makeText(this, R.string.msg_place_added, Toast.LENGTH_SHORT).show();
                    }
                }
            });

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
            binding.btnDeletePlace.setVisibility(View.VISIBLE);
            viewModel.loadPlaceForEditing(currentPlaceId);
        }
    }

    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        // TimePicker for access time
        binding.editAccessTime.setFocusable(false);
        binding.editAccessTime.setOnClickListener(v -> showTimePickerForAccessTime());

        // Geocoding preview button
        binding.btnSetOnMap.setOnClickListener(v -> {
            String street = binding.editStreet.getText() != null ? binding.editStreet.getText().toString().trim() : "";
            String city = binding.editCity.getText() != null ? binding.editCity.getText().toString().trim() : "";
            if (street.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, R.string.msg_validation_error, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, R.string.msg_geocoding_in_progress, Toast.LENGTH_SHORT).show();
            Executors.newSingleThreadExecutor().execute(() -> {
                String query = street + ", " + city + ", Poland";
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(query, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        runOnUiThread(() -> Toast.makeText(this, "Współrzędne GPS: " + String.format(Locale.US, "%.4f, %.4f", addr.getLatitude(), addr.getLongitude()), Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, R.string.error_location_not_found, Toast.LENGTH_SHORT).show());
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.error_location_not_found, Toast.LENGTH_SHORT).show());
                }
            });
        });

        // Submit button
        binding.btnSubmitPlace.setOnClickListener(v -> {
            String name = binding.editSpotName.getText() != null ? binding.editSpotName.getText().toString().trim() : "";
            String description = binding.editSpotDescription.getText() != null ? binding.editSpotDescription.getText().toString().trim() : "";
            String street = binding.editStreet.getText() != null ? binding.editStreet.getText().toString().trim() : "";
            String city = binding.editCity.getText() != null ? binding.editCity.getText().toString().trim() : "";
            String postalCode = binding.editPostalCode.getText() != null ? binding.editPostalCode.getText().toString().trim() : "";
            String placeNumber = binding.editPlaceNumber.getText() != null ? binding.editPlaceNumber.getText().toString().trim() : "";
            String floor = binding.editFloor.getText() != null ? binding.editFloor.getText().toString().trim() : "";

            int lengthCm = 500;
            int widthCm = 250;
            int heightCm = 200;
            try {
                if (binding.editLengthCm.getText() != null && !binding.editLengthCm.getText().toString().trim().isEmpty()) {
                    lengthCm = Integer.parseInt(binding.editLengthCm.getText().toString().trim());
                }
                if (binding.editWidthCm.getText() != null && !binding.editWidthCm.getText().toString().trim().isEmpty()) {
                    widthCm = Integer.parseInt(binding.editWidthCm.getText().toString().trim());
                }
                if (binding.editHeightCm.getText() != null && !binding.editHeightCm.getText().toString().trim().isEmpty()) {
                    heightCm = Integer.parseInt(binding.editHeightCm.getText().toString().trim());
                }
            } catch (NumberFormatException ignored) {
            }

            String price = binding.editPrice.getText() != null ? binding.editPrice.getText().toString().trim() : "";
            String accessInstructions = binding.editAccessInstructions.getText() != null ? binding.editAccessInstructions.getText().toString().trim() : "";
            String accessTime = binding.editAccessTime.getText() != null ? binding.editAccessTime.getText().toString().trim() : "";

            boolean autoAccept = binding.switchAutoAccept.isChecked();
            boolean requestVerification = binding.switchRequestVerification.isChecked();
            boolean ownershipDeclared = binding.checkOwnershipDeclared.isChecked();

            List<String> amenities = new ArrayList<>();
            if (binding.chipMonitoring.isChecked()) amenities.add("Monitoring");
            if (binding.chipLighting.isChecked()) amenities.add("Oświetlenie");
            if (binding.chipEv.isChecked()) amenities.add("EV Charging");
            if (binding.chipUnderground.isChecked()) amenities.add("Podziemny");
            if (binding.chipGate.isChecked()) amenities.add("Brama automatyczna");
            if (binding.chipCovered.isChecked()) amenities.add("Zadaszone");

            String ownerEmail = AppRepository.getInstance().getUserEmailLiveData().getValue();

            viewModel.savePlace(
                    currentPlaceId,
                    name,
                    description,
                    street,
                    city,
                    postalCode,
                    placeNumber,
                    floor,
                    lengthCm,
                    widthCm,
                    heightCm,
                    price,
                    accessInstructions,
                    accessTime,
                    autoAccept,
                    requestVerification,
                    ownershipDeclared,
                    amenities,
                    ownerEmail,
                    selectedPhotoUri
            );
        });

        // Delete button
        binding.btnDeletePlace.setOnClickListener(v -> {
            if (currentPlaceId != null) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_delete_title)
                        .setMessage(R.string.dialog_delete_confirm)
                        .setPositiveButton(R.string.btn_delete_place, (dialog, which) -> {
                            viewModel.deletePlace(currentPlaceId);
                            Toast.makeText(this, R.string.msg_place_deleted, Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            }
        });

        binding.btnUploadPhotos.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoPickerLauncher.launch(pickIntent);
        });
    }

    private void showTimePickerForAccessTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog fromPicker = new TimePickerDialog(this, (view, hourOfDayFrom, minuteFrom) -> {
            String fromFormatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDayFrom, minuteFrom);

            TimePickerDialog toPicker = new TimePickerDialog(this, (view2, hourOfDayTo, minuteTo) -> {
                String toFormatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDayTo, minuteTo);
                String timeRange = getString(R.string.format_sub_details, fromFormatted, toFormatted);
                binding.editAccessTime.setText(timeRange);
            }, (hourOfDayFrom + 8) % 24, minuteFrom, true);
            toPicker.setTitle("Godzina DO");
            toPicker.show();

        }, hour, minute, true);
        fromPicker.setTitle("Godzina OD");
        fromPicker.show();
    }

    private void observeViewModel() {
        viewModel.getEditingPlaceLiveData().observe(this, place -> {
            if (binding == null || place == null) {
                return;
            }

            binding.textToolbarTitle.setText(R.string.title_edit_place);
            binding.btnSubmitPlace.setText(R.string.title_edit_place);

            if (place.getTitle() != null) binding.editSpotName.setText(place.getTitle());
            if (place.getStreet() != null) binding.editStreet.setText(place.getStreet());
            if (place.getCity() != null) binding.editCity.setText(place.getCity());
            if (place.getPostalCode() != null) binding.editPostalCode.setText(place.getPostalCode());
            if (place.getPlaceNumber() != null) binding.editPlaceNumber.setText(place.getPlaceNumber());
            if (place.getFloor() != null) binding.editFloor.setText(place.getFloor());
            binding.editAccessTime.setText(place.getAccessTime());
            binding.editPrice.setText(String.format(Locale.US, "%.2f", place.getPrice()));
            selectedPhotoUri = place.getImageUri();

            if (place.getAmenities() != null) {
                binding.chipMonitoring.setChecked(place.getAmenities().contains("Monitoring"));
                binding.chipLighting.setChecked(place.getAmenities().contains("Oświetlenie"));
                binding.chipEv.setChecked(place.getAmenities().contains("EV Charging") || place.getAmenities().contains("Ładowanie EV"));
                binding.chipUnderground.setChecked(place.getAmenities().contains("Podziemny"));
                binding.chipGate.setChecked(place.getAmenities().contains("Brama automatyczna") || place.getAmenities().contains("GATE"));
                binding.chipCovered.setChecked(place.getAmenities().contains("Zadaszone") || place.getAmenities().contains("COVERED"));
            }
        });

        viewModel.getCreateSpotStatusLiveData().observe(this, resource -> {
            if (resource == null || binding == null) return;
            switch (resource.status) {
                case LOADING:
                    binding.btnSubmitPlace.setEnabled(false);
                    Toast.makeText(this, R.string.msg_geocoding_in_progress, Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    binding.btnSubmitPlace.setEnabled(true);
                    Toast.makeText(this, R.string.msg_place_added, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ERROR:
                    binding.btnSubmitPlace.setEnabled(true);
                    String error = resource.message != null ? resource.message : getString(R.string.error_spot_creation_failed, "Błąd sieci");
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    break;
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
