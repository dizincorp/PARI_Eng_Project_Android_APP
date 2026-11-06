package com.example.project.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityAddPlaceBinding;
import com.example.project.model.ParkingPlace;
import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.repository.AppRepository;
import com.example.project.utils.ViewAnimationUtils;
import com.example.project.viewmodel.AddPlaceViewModel;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Activity for creating or updating a parking spot with photo selection, geocoding,
 * and decoupled availability management via {@link SpotAvailabilityActivity}.
 */
public class AddPlaceActivity extends BaseActivity {

    public static final String EXTRA_PLACE_ID = "EXTRA_PLACE_ID";

    private ActivityAddPlaceBinding binding;
    private AddPlaceViewModel viewModel;
    private String currentPlaceId = null;
    private String selectedPhotoUri = "";

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    openGallery();
                } else {
                    Toast.makeText(this, R.string.permission_storage_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedPhotoUri = imageUri.toString();
                        if (binding != null) {
                            binding.imgSpotPreview.setImageURI(imageUri);
                            binding.cardPhotoPreview.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> availabilityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    boolean is247 = data.getBooleanExtra(SpotAvailabilityActivity.EXTRA_IS_24_7, true);
                    Serializable rawRules = data.getSerializableExtra(SpotAvailabilityActivity.EXTRA_RULES);
                    List<AvailabilityRuleRequest> rules = new ArrayList<>();
                    if (rawRules instanceof List<?>) {
                        for (Object obj : (List<?>) rawRules) {
                            if (obj instanceof AvailabilityRuleRequest) {
                                rules.add((AvailabilityRuleRequest) obj);
                            }
                        }
                    }
                    String summary = data.getStringExtra(SpotAvailabilityActivity.EXTRA_ACCESS_TIME_SUMMARY);
                    if (summary == null || summary.trim().isEmpty()) {
                        summary = is247 ? "Całodobowo 24/7" : "Zgodnie z harmonogramem";
                    }

                    viewModel.setAvailabilitySchedule(is247, rules, summary);
                    if (binding != null) {
                        binding.textAvailabilitySummary.setText(summary);
                    }
                }
            });

    private Double selectedLatitude = null;
    private Double selectedLongitude = null;

    private final ActivityResultLauncher<Intent> pickLocationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    double lat = data.getDoubleExtra(PickLocationActivity.EXTRA_LATITUDE, 0.0);
                    double lng = data.getDoubleExtra(PickLocationActivity.EXTRA_LONGITUDE, 0.0);
                    String street = data.getStringExtra(PickLocationActivity.EXTRA_STREET);
                    String city = data.getStringExtra(PickLocationActivity.EXTRA_CITY);
                    String postalCode = data.getStringExtra(PickLocationActivity.EXTRA_POSTAL_CODE);

                    if (street != null && !street.isEmpty()) {
                        binding.editStreet.setText(street);
                    }
                    if (city != null && !city.isEmpty()) {
                        binding.editCity.setText(city);
                    }
                    if (postalCode != null && !postalCode.isEmpty()) {
                        binding.editPostalCode.setText(postalCode);
                    }

                    if (lat != 0.0 && lng != 0.0) {
                        selectedLatitude = lat;
                        selectedLongitude = lng;
                        viewModel.setCustomCoordinates(lat, lng);
                        if (binding.layoutSelectedCoords != null && binding.textSelectedCoords != null) {
                            binding.layoutSelectedCoords.setVisibility(View.VISIBLE);
                            binding.textSelectedCoords.setText(getString(R.string.coords_set_from_map,
                                    String.format(Locale.US, "%.5f, %.5f", lat, lng)));
                        }
                    }
                    Toast.makeText(this, R.string.toast_location_selected, Toast.LENGTH_SHORT).show();
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
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

    /**
     * Sets up click listeners for all interactive form actions.
     */
    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

        binding.btnBack.setOnClickListener(v -> {
            ViewAnimationUtils.playBounceAnimation(v, null);
            finish();
        });
        binding.btnMore.setOnClickListener(v -> finish());

        // Black button with white text "Dodaj zdjęcie" requesting storage permission before opening gallery
        binding.btnAddPhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());

        // Remove photo button to clear selection and hide preview
        binding.btnRemovePhoto.setOnClickListener(v -> {
            selectedPhotoUri = "";
            binding.imgSpotPreview.setImageDrawable(null);
            binding.cardPhotoPreview.setVisibility(View.GONE);
        });

        // Availability card opens dedicated SpotAvailabilityActivity
        binding.cardSpotAvailability.setOnClickListener(v -> {
            ViewAnimationUtils.playBounceAnimation(v, () -> {
                Intent intent = new Intent(AddPlaceActivity.this, SpotAvailabilityActivity.class);
                if (currentPlaceId != null) {
                    intent.putExtra(SpotAvailabilityActivity.EXTRA_SPOT_ID, currentPlaceId);
                }
                String name = binding.editSpotName.getText() != null ? binding.editSpotName.getText().toString().trim() : "";
                if (!name.isEmpty()) {
                    intent.putExtra(SpotAvailabilityActivity.EXTRA_SPOT_TITLE, name);
                }
                Boolean is247 = viewModel.getIs247LiveData().getValue();
                intent.putExtra(SpotAvailabilityActivity.EXTRA_IS_24_7, is247 != null ? is247 : true);

                List<AvailabilityRuleRequest> currentRules = viewModel.getAvailabilityRulesLiveData().getValue();
                if (currentRules != null && !currentRules.isEmpty()) {
                    intent.putExtra(SpotAvailabilityActivity.EXTRA_RULES, new ArrayList<>(currentRules));
                }

                String currentSummary = viewModel.getAccessTimeLiveData().getValue();
                if (currentSummary != null) {
                    intent.putExtra(SpotAvailabilityActivity.EXTRA_ACCESS_TIME_SUMMARY, currentSummary);
                }

                availabilityLauncher.launch(intent);
            });
        });

        // Launch interactive Map location picker
        binding.btnSetOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, PickLocationActivity.class);
            String street = binding.editStreet.getText() != null ? binding.editStreet.getText().toString().trim() : "";
            String city = binding.editCity.getText() != null ? binding.editCity.getText().toString().trim() : "";
            String postalCode = binding.editPostalCode.getText() != null ? binding.editPostalCode.getText().toString().trim() : "";

            if (!street.isEmpty()) {
                intent.putExtra(PickLocationActivity.EXTRA_INITIAL_STREET, street);
            }
            if (!city.isEmpty()) {
                intent.putExtra(PickLocationActivity.EXTRA_INITIAL_CITY, city);
            }
            if (!postalCode.isEmpty()) {
                intent.putExtra(PickLocationActivity.EXTRA_INITIAL_POSTAL_CODE, postalCode);
            }
            if (selectedLatitude != null && selectedLongitude != null
                    && selectedLatitude != 0.0 && selectedLongitude != 0.0) {
                intent.putExtra(PickLocationActivity.EXTRA_INITIAL_LAT, selectedLatitude);
                intent.putExtra(PickLocationActivity.EXTRA_INITIAL_LNG, selectedLongitude);
            }
            pickLocationLauncher.launch(intent);
        });

        // Submit button
        binding.btnSubmitPlace.setOnClickListener(v -> {
            String name = binding.editSpotName.getText() != null ? binding.editSpotName.getText().toString().trim() : "";
            String description = binding.editSpotDescription.getText() != null ? binding.editSpotDescription.getText().toString().trim() : "";
            String street = binding.editStreet.getText() != null ? binding.editStreet.getText().toString().trim() : "";
            String city = binding.editCity.getText() != null ? binding.editCity.getText().toString().trim() : "";
            String postalCode = binding.editPostalCode.getText() != null ? binding.editPostalCode.getText().toString().trim() : "";

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
            String accessTime = viewModel.getAccessTimeLiveData().getValue();
            if (accessTime == null || accessTime.trim().isEmpty()) {
                accessTime = "08:00 - 20:00";
            }

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
                    "",
                    "",
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
    }

    /**
     * Checks media/storage permissions and requests them or opens gallery directly.
     */
    private void checkPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_permission_needed)
                    .setMessage(R.string.permission_storage_rationale)
                    .setPositiveButton(R.string.btn_allow, (dialog, which) -> permissionLauncher.launch(permission))
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    /**
     * Opens device gallery picker to allow user to choose a spot photograph.
     */
    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerLauncher.launch(pickIntent);
    }

    /**
     * Observes ViewModel LiveData streams for editing data and creation network status.
     */
    private void observeViewModel() {
        viewModel.getEditingPlaceLiveData().observe(this, place -> {
            if (binding == null || place == null) {
                return;
            }

            binding.textToolbarTitle.setText(R.string.title_edit_place);
            binding.btnSubmitPlace.setText(R.string.title_edit_place);

            if (place.getTitle() != null) binding.editSpotName.setText(place.getTitle());
            if (place.getDescription() != null) binding.editSpotDescription.setText(place.getDescription());
            if (place.getAccessInstructions() != null) binding.editAccessInstructions.setText(place.getAccessInstructions());
            if (place.getStreet() != null) binding.editStreet.setText(place.getStreet());
            if (place.getCity() != null) binding.editCity.setText(place.getCity());
            if (place.getPostalCode() != null) binding.editPostalCode.setText(place.getPostalCode());
            if (place.getLatitude() != 0.0 && place.getLongitude() != 0.0) {
                selectedLatitude = place.getLatitude();
                selectedLongitude = place.getLongitude();
                viewModel.setCustomCoordinates(selectedLatitude, selectedLongitude);
                if (binding.layoutSelectedCoords != null && binding.textSelectedCoords != null) {
                    binding.layoutSelectedCoords.setVisibility(View.VISIBLE);
                    binding.textSelectedCoords.setText(getString(R.string.coords_set_from_map,
                            String.format(Locale.US, "%.5f, %.5f", selectedLatitude, selectedLongitude)));
                }
            }
            binding.editPrice.setText(String.format(Locale.US, "%.2f", place.getPrice()));

            selectedPhotoUri = place.getImageUri();
            // Do NOT show default photo in creation/edit menu; only show if user has uploaded a custom image
            if (place.getImageUri() != null && !place.getImageUri().trim().isEmpty()) {
                binding.cardPhotoPreview.setVisibility(View.VISIBLE);
                com.example.project.utils.ImageLoader.getInstance(this).loadImage(
                        binding.imgSpotPreview,
                        place.getImageUri().trim(),
                        0
                );
            } else {
                binding.cardPhotoPreview.setVisibility(View.GONE);
            }

            if (place.getAmenities() != null) {
                binding.chipMonitoring.setChecked(place.getAmenities().contains("Monitoring"));
                binding.chipLighting.setChecked(place.getAmenities().contains("Oświetlenie"));
                binding.chipEv.setChecked(place.getAmenities().contains("EV Charging") || place.getAmenities().contains("Ładowanie EV"));
                binding.chipUnderground.setChecked(place.getAmenities().contains("Podziemny"));
                binding.chipGate.setChecked(place.getAmenities().contains("Brama automatyczna") || place.getAmenities().contains("GATE"));
                binding.chipCovered.setChecked(place.getAmenities().contains("Zadaszone") || place.getAmenities().contains("COVERED"));
            }
        });

        // Observe access time to update summary on card
        viewModel.getAccessTimeLiveData().observe(this, accessTime -> {
            if (binding != null && accessTime != null && !accessTime.trim().isEmpty()) {
                binding.textAvailabilitySummary.setText(accessTime.trim());
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

        viewModel.getToastMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
