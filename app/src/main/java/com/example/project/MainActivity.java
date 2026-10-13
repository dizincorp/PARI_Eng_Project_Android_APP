package com.example.project;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.project.databinding.ActivityMainBinding;
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

public class MainActivity extends BaseActivity implements OnMapReadyCallback {

    private ActivityMainBinding binding;
    private SearchViewModel viewModel;
    private GoogleMap googleMap;
    private final Map<String, ParkingPlace> markerPlaceMap = new HashMap<>();
    private ParkingPlace selectedPlaceForBooking = null;

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
            if (selectedPlaceForBooking != null) {
                Intent intent = new Intent(MainActivity.this, ReservePlaceActivity.class);
                intent.putExtra(ReservePlaceActivity.EXTRA_PLACE_ID, selectedPlaceForBooking.getId());
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
        viewModel.getSearchResultLiveData().observe(this, result -> {
            if (result == null) {
                return;
            }

            if (result.isSuccess() && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(result.getLatLng(), 14f));
            } else if (result.getErrorMessage() != null) {
                Toast.makeText(MainActivity.this, result.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Observe places from Repository so newly added places appear automatically on the map!
        AppRepository.getInstance().getPlacesLiveData().observe(this, places -> {
            if (googleMap != null && places != null) {
                displayPlacesOnMap(places);
            }
        });
    }

    private void displayPlacesOnMap(List<ParkingPlace> places) {
        if (googleMap == null || places == null) return;

        googleMap.clear();
        markerPlaceMap.clear();

        for (ParkingPlace p : places) {
            if (p == null || p.isOccupied()) {
                // If occupied or null, do not show on map
                continue;
            }

            LatLng pos = new LatLng(p.getLatitude(), p.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(p.getTitle())
                    .snippet(String.format(Locale.getDefault(), "%.2f zł/h", p.getPrice()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            if (marker != null) {
                markerPlaceMap.put(marker.getId(), p);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLocation = new LatLng(52.2297, 21.0122); // Warszawa
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        List<ParkingPlace> currentPlaces = AppRepository.getInstance().getPlacesLiveData().getValue();
        if (currentPlaces != null) {
            displayPlacesOnMap(currentPlaces);
        }

        googleMap.setOnMarkerClickListener(marker -> {
            ParkingPlace place = markerPlaceMap.get(marker.getId());
            if (place != null) {
                showPlacePreviewCard(place);
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

    private void showPlacePreviewCard(ParkingPlace place) {
        if (binding == null || place == null) return;

        selectedPlaceForBooking = place;

        binding.textPreviewTitle.setText(place.getTitle());
        binding.textPreviewPriceValue.setText(String.format(Locale.getDefault(), "%.0f", place.getPrice()));

        binding.chipGroupPreviewAmenities.removeAllViews();
        if (place.getAmenities() != null) {
            for (String amenity : place.getAmenities()) {
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