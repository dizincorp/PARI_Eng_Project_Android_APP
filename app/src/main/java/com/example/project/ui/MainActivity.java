package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityMainBinding;
import com.example.project.network.model.ParkingSpotResponse;
import com.example.project.viewmodel.SearchViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main map activity hosting {@link SupportMapFragment} to discover and reserve
 * parking spots loaded dynamically from the backend via {@link SearchViewModel} and LiveData.
 */
public class MainActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private SearchViewModel viewModel;
    private GoogleMap googleMap;

    private final Map<String, ParkingSpotResponse> markerSpotMap = new HashMap<>();
    private ParkingSpotResponse selectedSpotForBooking = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int margin16Px = Math.round(16 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (binding != null && binding.cardSearch != null) {
                ConstraintLayout.LayoutParams cardParams =
                        (ConstraintLayout.LayoutParams) binding.cardSearch.getLayoutParams();
                if (cardParams != null) {
                    cardParams.topMargin = systemBars.top + margin16Px;
                    binding.cardSearch.setLayoutParams(cardParams);
                }
            }

            if (binding != null && binding.bottomNavigation != null) {
                binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_search);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearchView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnReserveNow.setOnClickListener(v -> {
            if (selectedSpotForBooking != null) {
                if (!selectedSpotForBooking.isFree()) {
                    Toast.makeText(this, "To miejsce jest obecnie zajęte!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, ReservePlaceActivity.class);
                intent.putExtra(ReservePlaceActivity.EXTRA_PLACE_ID, selectedSpotForBooking.getId());
                startActivity(intent);
            }
        });
    }

    private void setupSearchView() {
        if (binding == null || binding.searchView == null) {
            return;
        }

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    binding.searchView.clearFocus();
                    viewModel.searchLocation(getApplicationContext(), query.trim());
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_empty_query, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void observeViewModel() {
        // Observe geocoding search results to animate map camera
        viewModel.getSearchResultLiveData().observe(this, result -> {
            if (result == null) return;

            if (result.isSuccess() && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(result.getLatLng(), 14f));
            } else if (result.getErrorMessage() != null) {
                Toast.makeText(MainActivity.this, result.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Observe reactive parking spots from backend REST API
        viewModel.getSpotsLiveData().observe(this, resource -> {
            if (resource == null) return;

            if (resource.isSuccess() && resource.getData() != null) {
                displaySpotsOnMap(resource.getData());
            } else if (resource.isError()) {
                if (resource.getMessage() != null) {
                    Log.e(TAG, "Error loading parking spots from API: " + resource.getMessage());
                }
            }
        });
    }

    /**
     * Renders parking spot markers dynamically onto Google Map.
     *
     * @param spots List of parking spots returned from backend service
     */
    private void displaySpotsOnMap(List<ParkingSpotResponse> spots) {
        if (googleMap == null || spots == null) return;

        googleMap.clear();
        markerSpotMap.clear();

        for (ParkingSpotResponse spot : spots) {
            if (spot == null) continue;

            float markerColor;
            if (!spot.isFree()) {
                markerColor = BitmapDescriptorFactory.HUE_RED;   // Red: occupied spot
            } else {
                markerColor = BitmapDescriptorFactory.HUE_GREEN; // Green: available spot to reserve
            }

            LatLng pos = new LatLng(spot.getLat(), spot.getLng());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(spot.getName())
                    .snippet(String.format(Locale.getDefault(), "%.2f zł/h", spot.getHourlyPrice()))
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (marker != null) {
                markerSpotMap.put(marker.getId(), spot);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLocation = new LatLng(52.2297, 21.0122);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        // Initial spot loading for default location
        viewModel.loadNearbySpots(defaultLocation.latitude, defaultLocation.longitude, 5000.0);

        // Dynamically load spots around camera center when map panning stops
        googleMap.setOnCameraIdleListener(() -> {
            if (googleMap != null) {
                LatLng center = googleMap.getCameraPosition().target;
                viewModel.loadNearbySpots(center.latitude, center.longitude, 5000.0);
            }
        });

        googleMap.setOnMarkerClickListener(marker -> {
            ParkingSpotResponse spot = markerSpotMap.get(marker.getId());
            if (spot != null) {
                showSpotPreviewCard(spot);
                return true;
            }
            return false;
        });

        googleMap.setOnMapClickListener(latLng -> {
            if (binding != null && binding.cardPlacePreview != null) {
                binding.cardPlacePreview.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Displays the preview card overlay for a clicked parking spot marker.
     *
     * @param spot Selected parking spot
     */
    private void showSpotPreviewCard(ParkingSpotResponse spot) {
        if (binding == null || spot == null) return;

        selectedSpotForBooking = spot;

        binding.textPreviewTitle.setText(spot.getName());
        binding.textPreviewRating.setText(String.format(Locale.getDefault(), "★ %.1f (%d)",
                spot.getAvgRating(), spot.getReviewCount()));
        binding.textPreviewPriceValue.setText(String.format(Locale.getDefault(), "%.0f", spot.getHourlyPrice()));

        binding.chipGroupPreviewAmenities.removeAllViews();
        if (spot.getAmenities() != null) {
            for (String amenity : spot.getAmenities()) {
                Chip chip = new Chip(this);
                chip.setText(amenity);
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupPreviewAmenities.addView(chip);
            }
        }

        binding.cardPlacePreview.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        googleMap = null;
        binding = null;
    }
}
