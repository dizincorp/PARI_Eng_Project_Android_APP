package com.example.project.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.R;
import com.example.project.databinding.ActivityPickLocationBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interactive map activity allowing the user to select the exact physical coordinates
 * and address of their parking spot. Reverse-geocodes dynamically as the map moves
 * and returns the chosen coordinates + address back to the caller.
 */
public class PickLocationActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "PickLocationActivity";

    // Intent Extras
    public static final String EXTRA_INITIAL_LAT = "extra_initial_lat";
    public static final String EXTRA_INITIAL_LNG = "extra_initial_lng";
    public static final String EXTRA_INITIAL_STREET = "extra_initial_street";
    public static final String EXTRA_INITIAL_CITY = "extra_initial_city";
    public static final String EXTRA_INITIAL_POSTAL_CODE = "extra_initial_postal_code";

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_STREET = "extra_street";
    public static final String EXTRA_CITY = "extra_city";
    public static final String EXTRA_POSTAL_CODE = "extra_postal_code";

    private ActivityPickLocationBinding binding;
    private GoogleMap googleMap;

    private double selectedLat = 52.2297;
    private double selectedLng = 21.0122;
    private String selectedStreet = "";
    private String selectedCity = "";
    private String selectedPostalCode = "";

    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int geocodeRequestId = 0;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean fineGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineGranted || coarseGranted) {
                    enableMyLocationAndCenter();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPickLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.cardHeader.setPadding(0, systemBars.top, 0, 0);
            binding.cardBottomAddress.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        readInitialExtras();
        setupViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void readInitialExtras() {
        if (getIntent() != null) {
            double initLat = getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, 0.0);
            double initLng = getIntent().getDoubleExtra(EXTRA_INITIAL_LNG, 0.0);
            if (initLat != 0.0 && initLng != 0.0) {
                selectedLat = initLat;
                selectedLng = initLng;
            }
            String street = getIntent().getStringExtra(EXTRA_INITIAL_STREET);
            if (street != null && !street.trim().isEmpty()) {
                selectedStreet = street.trim();
            }
            String city = getIntent().getStringExtra(EXTRA_INITIAL_CITY);
            if (city != null && !city.trim().isEmpty()) {
                selectedCity = city.trim();
            }
            String postal = getIntent().getStringExtra(EXTRA_INITIAL_POSTAL_CODE);
            if (postal != null && !postal.trim().isEmpty()) {
                selectedPostalCode = postal.trim();
            }
        }
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Search clear button
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.editSearch.setText("");
            hideKeyboard();
        });

        // Search submit via keyboard
        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = binding.editSearch.getText() != null ? binding.editSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    searchLocationQuery(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });

        // GPS my-location button
        binding.btnMyLocation.setOnClickListener(v -> checkLocationPermissionAndCenter());

        // Confirm location button
        binding.btnConfirmLocation.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_LATITUDE, selectedLat);
            resultIntent.putExtra(EXTRA_LONGITUDE, selectedLng);
            resultIntent.putExtra(EXTRA_STREET, selectedStreet);
            resultIntent.putExtra(EXTRA_CITY, selectedCity);
            resultIntent.putExtra(EXTRA_POSTAL_CODE, selectedPostalCode);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        updateAddressUi();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(false);

        // Initial centering
        LatLng startPoint = new LatLng(selectedLat, selectedLng);
        boolean hasExplicitCoords = getIntent() != null && getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, 0.0) != 0.0;

        if (hasExplicitCoords) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 16f));
            triggerReverseGeocode(startPoint);
        } else if (!selectedStreet.isEmpty() || !selectedCity.isEmpty()) {
            String initialQuery = (selectedStreet + " " + selectedCity).trim();
            searchLocationQuery(initialQuery);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 13f));
            triggerReverseGeocode(startPoint);
            // Optionally center to GPS if permission is already granted
            if (hasLocationPermission()) {
                enableMyLocationAndCenter();
            }
        }

        // Tap on map centers camera on clicked position
        googleMap.setOnMapClickListener(latLng -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        });

        // Camera move listeners
        googleMap.setOnCameraMoveStartedListener(reason -> {
            binding.textSelectedStreet.setText(R.string.loading_address);
        });

        googleMap.setOnCameraIdleListener(() -> {
            if (googleMap != null) {
                LatLng center = googleMap.getCameraPosition().target;
                selectedLat = center.latitude;
                selectedLng = center.longitude;
                triggerReverseGeocode(center);
            }
        });
    }

    private void triggerReverseGeocode(@NonNull LatLng latLng) {
        final int currentRequestId = ++geocodeRequestId;
        geocodeExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                mainHandler.post(() -> {
                    if (currentRequestId != geocodeRequestId || isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String thoroughfare = addr.getThoroughfare();
                        String subThoroughfare = addr.getSubThoroughfare();
                        String feature = addr.getFeatureName();

                        String streetResult = "";
                        if (thoroughfare != null && !thoroughfare.isEmpty()) {
                            if (subThoroughfare != null && !subThoroughfare.isEmpty()) {
                                streetResult = thoroughfare + " " + subThoroughfare;
                            } else if (feature != null && !feature.isEmpty() && !feature.equalsIgnoreCase(thoroughfare)) {
                                streetResult = thoroughfare + " " + feature;
                            } else {
                                streetResult = thoroughfare;
                            }
                        } else if (addr.getAddressLine(0) != null) {
                            String full = addr.getAddressLine(0);
                            String[] parts = full.split(",");
                            streetResult = parts.length > 0 ? parts[0].trim() : full;
                        } else {
                            streetResult = getString(R.string.unknown_address);
                        }

                        String cityResult = addr.getLocality();
                        if (cityResult == null || cityResult.isEmpty()) {
                            cityResult = addr.getSubAdminArea() != null ? addr.getSubAdminArea() :
                                    (addr.getAdminArea() != null ? addr.getAdminArea() : "");
                        }
                        String postalResult = addr.getPostalCode() != null ? addr.getPostalCode() : "";

                        selectedStreet = streetResult;
                        selectedCity = cityResult;
                        selectedPostalCode = postalResult;
                    } else {
                        selectedStreet = getString(R.string.unknown_address);
                    }
                    updateAddressUi();
                });
            } catch (Exception e) {
                Log.w(TAG, "Reverse geocoding error: " + e.getMessage());
                mainHandler.post(() -> {
                    if (currentRequestId != geocodeRequestId || isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (selectedStreet.isEmpty()) {
                        selectedStreet = getString(R.string.unknown_address);
                    }
                    updateAddressUi();
                });
            }
        });
    }

    private void searchLocationQuery(@NonNull String query) {
        binding.textSelectedStreet.setText(R.string.loading_address);
        geocodeExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                String searchQuery = query;
                if (!query.toLowerCase().contains("polska") && !query.toLowerCase().contains("poland")) {
                    searchQuery += ", Poland";
                }
                List<Address> addresses = geocoder.getFromLocationName(searchQuery, 1);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        LatLng target = new LatLng(addr.getLatitude(), addr.getLongitude());
                        if (googleMap != null) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f));
                        }
                    } else {
                        Toast.makeText(this, R.string.toast_address_not_found, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Geocoding query error: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.toast_address_not_found, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void checkLocationPermissionAndCenter() {
        if (hasLocationPermission()) {
            enableMyLocationAndCenter();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocationAndCenter() {
        if (googleMap == null) return;
        try {
            if (hasLocationPermission()) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);

                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Location best = gpsLoc != null ? gpsLoc : netLoc;
                    if (best != null) {
                        LatLng myLatLng = new LatLng(best.getLatitude(), best.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 16f));
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException enabling location", e);
        }
    }

    private void updateAddressUi() {
        if (binding == null) return;

        binding.textSelectedStreet.setText(selectedStreet.isEmpty() ? getString(R.string.unknown_address) : selectedStreet);

        StringBuilder cityPostal = new StringBuilder();
        if (!selectedPostalCode.isEmpty()) {
            cityPostal.append(selectedPostalCode).append(" ");
        }
        if (!selectedCity.isEmpty()) {
            cityPostal.append(selectedCity);
        }
        binding.textSelectedCity.setText(cityPostal.toString());
        binding.textSelectedCity.setVisibility(cityPostal.length() > 0 ? View.VISIBLE : View.GONE);

        binding.textSelectedCoords.setText(String.format(Locale.US, "GPS: %.5f, %.5f", selectedLat, selectedLng));
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdown();
        binding = null;
    }
}
