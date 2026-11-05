package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project.R;
import com.example.project.adapter.ReviewAdapter;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.databinding.ActivityMainBinding;
import com.example.project.databinding.DialogSpotReviewsBinding;
import com.example.project.network.Resource;
import com.example.project.repository.AvailabilityRepository;
import com.example.project.repository.ReviewRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main Activity hosting the Google Maps search fragment with reactive parking spot discovery
 * backed by Room Database as the Single Source of Truth, displaying dynamic pricing, amenities,
 * and active/upcoming calendar blocked period annotations on map markers and preview cards.
 */
public class MainActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private SearchViewModel viewModel;
    private GoogleMap googleMap;

    private final Map<String, ParkingPlaceEntity> markerSpotMap = new HashMap<>();
    private final Map<String, List<BlockedPeriodEntity>> spotBlockedMap = new HashMap<>();
    private ParkingPlaceEntity selectedSpotForBooking = null;

    private final Handler timeCheckHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (googleMap != null && viewModel != null) {
                List<ParkingPlaceEntity> currentSpots = viewModel.getSpotsLiveData().getValue();
                if (currentSpots != null) {
                    displaySpotsOnMap(currentSpots);
                }
            }
            timeCheckHandler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (googleMap != null) {
            LatLng center = googleMap.getCameraPosition().target;
            if (center != null && (center.latitude != 0.0 || center.longitude != 0.0)) {
                viewModel.loadNearbySpots(center.latitude, center.longitude, 5000.0);
            }
        }
        List<ParkingPlaceEntity> currentSpots = viewModel.getSpotsLiveData().getValue();
        if (currentSpots != null && googleMap != null) {
            displaySpotsOnMap(currentSpots);
        }
        if (selectedSpotForBooking != null) {
            showSpotPreviewCard(selectedSpotForBooking);
        }
        timeCheckHandler.removeCallbacks(timeCheckRunnable);
        timeCheckHandler.postDelayed(timeCheckRunnable, 15000);
        com.example.project.utils.SessionManager sessionManager = com.example.project.utils.SessionManager.getInstance(this);
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            com.example.project.repository.VehicleRepository.getInstance(this).fetchVehicles(sessionManager.getUserEmail());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeCheckHandler.removeCallbacks(timeCheckRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_search);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(binding.map.getId());
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
                if (!isSpotActiveAtCurrentMoment(selectedSpotForBooking)) {
                    Toast.makeText(this, R.string.error_spot_inactive, Toast.LENGTH_SHORT).show();
                    if (binding.cardPlacePreview != null) {
                        binding.cardPlacePreview.setVisibility(View.GONE);
                    }
                    selectedSpotForBooking = null;
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

        // Observe reactive parking spots from local Room Database (Single Source of Truth)
        viewModel.getSpotsLiveData().observe(this, spots -> {
            if (spots != null) {
                displaySpotsOnMap(spots);
            }
        });

        // Observe calendar blocked periods to annotate markers and preview card
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
            List<ParkingPlaceEntity> currentSpots = viewModel.getSpotsLiveData().getValue();
            if (currentSpots != null) {
                displaySpotsOnMap(currentSpots);
            }
            if (selectedSpotForBooking != null) {
                showSpotPreviewCard(selectedSpotForBooking);
            }
        });

        // Observe network sync status
        viewModel.getSyncStatusLiveData().observe(this, resource -> {
            if (resource != null && resource.isError() && resource.getMessage() != null) {
                Log.e(TAG, "Sync error: " + resource.getMessage());
            }
        });
    }

    /**
     * Renders parking spot markers dynamically onto Google Map with blocked periods annotations.
     * Displays strictly spots that are active and available at the current moment.
     * Non-active, currently blocked, and occupied/reserved spots are filtered out.
     *
     * @param spots List of parking spots from Room Database
     */
    private void displaySpotsOnMap(List<ParkingPlaceEntity> spots) {
        if (googleMap == null || spots == null) return;

        googleMap.clear();
        markerSpotMap.clear();

        // Dismiss preview card if the selected spot is no longer active at the current moment
        if (selectedSpotForBooking != null && !isSpotActiveAtCurrentMoment(selectedSpotForBooking)) {
            if (binding != null && binding.cardPlacePreview != null) {
                binding.cardPlacePreview.setVisibility(View.GONE);
            }
            selectedSpotForBooking = null;
        }

        for (ParkingPlaceEntity spot : spots) {
            if (!isSpotActiveAtCurrentMoment(spot)) {
                continue;
            }

            // Identify any upcoming calendar blocks for informative snippet text
            List<BlockedPeriodEntity> spotBlocks = spotBlockedMap.get(spot.getId());
            BlockedPeriodEntity upcomingBlock = null;
            if (spotBlocks != null) {
                for (BlockedPeriodEntity b : spotBlocks) {
                    if (b != null && !b.isExpired() && !b.isCurrentlyActive()) {
                        upcomingBlock = b;
                        break;
                    }
                }
            }

            float markerColor = BitmapDescriptorFactory.HUE_GREEN;  // Green: free / available to reserve
            String snippetText;
            if (upcomingBlock != null) {
                snippetText = String.format(Locale.getDefault(), "%.2f zł/h • ⛔ %s", spot.getPrice(), upcomingBlock.getFormattedBlockedNotice());
            } else {
                snippetText = String.format(Locale.getDefault(), "%.2f zł/h", spot.getPrice());
            }

            LatLng pos = new LatLng(spot.getLatitude(), spot.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(spot.getTitle())
                    .snippet(snippetText)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (marker != null) {
                markerSpotMap.put(marker.getId(), spot);
            }
        }
    }

    /**
     * Evaluates whether a parking spot is active and free at the current moment of time.
     * On the map, only spots that are ACTIVE (not deactivated by owner) and FREE
     * (not occupied, not checked-in, not currently blocked by calendar block) are displayed.
     *
     * @param spot Parking spot entity to check
     * @return True if spot is active and free right now, false otherwise
     */
    private boolean isSpotActiveAtCurrentMoment(@Nullable ParkingPlaceEntity spot) {
        if (spot == null) return false;

        // 1. Lifecycle status filter: owner deactivated (INACTIVE), archived, or blocked spots are excluded
        String status = spot.getStatus();
        if ("INACTIVE".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status)
                || "BLOCKED".equalsIgnoreCase(status) || spot.isBlocked()) {
            return false;
        }

        // 2. Occupancy filter: spot must be FREE right now (not currently checked-in or occupied)
        if (spot.isCheckedIn() || spot.isOccupied()) {
            return false;
        }

        // 3. Calendar block filter: spot must not have a calendar block active at the current moment of time
        List<BlockedPeriodEntity> spotBlocks = spotBlockedMap.get(spot.getId());
        if (spotBlocks != null) {
            for (BlockedPeriodEntity b : spotBlocks) {
                if (b != null && !b.isExpired() && b.isCurrentlyActive()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLocation = new LatLng(52.2297, 21.0122);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        // Initial spot loading from backend into Room for default location
        viewModel.loadNearbySpots(defaultLocation.latitude, defaultLocation.longitude, 5000.0);

        // Dynamically load spots around camera center when map panning stops
        googleMap.setOnCameraIdleListener(() -> {
            if (googleMap != null) {
                LatLng center = googleMap.getCameraPosition().target;
                viewModel.loadNearbySpots(center.latitude, center.longitude, 5000.0);
            }
        });

        googleMap.setOnMarkerClickListener(marker -> {
            ParkingPlaceEntity spot = markerSpotMap.get(marker.getId());
            if (spot != null) {
                showSpotPreviewCard(spot);
                if (spot.getId() != null) {
                    AvailabilityRepository.getInstance(this).fetchBlockedPeriods(spot.getId()).observe(this, res -> {
                        if (res != null && res.isSuccess()) {
                            if (selectedSpotForBooking != null && spot.getId().equals(selectedSpotForBooking.getId())) {
                                showSpotPreviewCard(spot);
                            }
                        }
                    });
                }
                return true;
            }
            return false;
        });

        googleMap.setOnMapClickListener(latLng -> {
            if (binding != null && binding.cardPlacePreview != null) {
                binding.cardPlacePreview.setVisibility(View.GONE);
            }
            selectedSpotForBooking = null;
        });
    }

    /**
     * Displays the preview card overlay for a clicked parking spot marker,
     * including amenities, description, and blocked intervals notice.
     *
     * @param spot Selected parking spot from Room Database
     */
    private void showSpotPreviewCard(ParkingPlaceEntity spot) {
        if (binding == null || spot == null) return;

        selectedSpotForBooking = spot;

        binding.textPreviewTitle.setText(spot.getTitle());
        String locationStr = String.format("%s, %s",
                spot.getCity() != null ? spot.getCity() : "",
                spot.getStreet() != null ? spot.getStreet() : "").trim();
        if (locationStr.startsWith(",")) {
            locationStr = locationStr.substring(1).trim();
        }
        if (locationStr.endsWith(",")) {
            locationStr = locationStr.substring(0, locationStr.length() - 1).trim();
        }

        if (!locationStr.isEmpty()) {
            binding.textPreviewRating.setText(String.format(Locale.getDefault(), "★ %.1f (%d) • %s",
                    spot.getRating(), spot.getRatingCount(), locationStr));
        } else {
            binding.textPreviewRating.setText(String.format(Locale.getDefault(), "★ %.1f (%d)",
                    spot.getRating(), spot.getRatingCount()));
        }
        binding.textPreviewPriceValue.setText(String.format(Locale.getDefault(), "%.0f", spot.getPrice()));

        // Make rating clickable to inspect spot reviews
        binding.textPreviewRating.setOnClickListener(v -> {
            if (spot.getId() != null) {
                showSpotReviewsDialog(spot.getId(), spot.getTitle(), spot.getRating(), spot.getRatingCount());
            }
        });

        binding.chipGroupPreviewAmenities.removeAllViews();
        boolean hasAmenities = false;
        if (spot.getAmenitiesCsv() != null && !spot.getAmenitiesCsv().trim().isEmpty()) {
            String[] amenities = spot.getAmenitiesCsv().split(",");
            for (String amenity : amenities) {
                String label = com.example.project.utils.AmenityUtils.formatAmenity(amenity);
                if (!label.isEmpty()) {
                    Chip chip = new Chip(this);
                    chip.setText(label);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    binding.chipGroupPreviewAmenities.addView(chip);
                    hasAmenities = true;
                }
            }
        }
        binding.chipGroupPreviewAmenities.setVisibility(hasAmenities ? View.VISIBLE : View.GONE);

        // Description under amenities (if present)
        String description = spot.getDescription();
        if ((description == null || description.trim().isEmpty()) && spot.getAccessInstructions() != null && !spot.getAccessInstructions().trim().isEmpty()) {
            description = spot.getAccessInstructions();
        }
        if (description != null && !description.trim().isEmpty()) {
            binding.textPreviewDescription.setText(description.trim());
            binding.textPreviewDescription.setVisibility(View.VISIBLE);
        } else {
            binding.textPreviewDescription.setVisibility(View.GONE);
        }

        // Blocked period notification badge
        List<BlockedPeriodEntity> spotBlocks = spotBlockedMap.get(spot.getId());
        boolean hasActiveBlock = false;
        StringBuilder sb = new StringBuilder();
        if (spotBlocks != null && !spotBlocks.isEmpty()) {
            for (BlockedPeriodEntity b : spotBlocks) {
                if (b == null || b.isExpired()) continue;
                if (b.isCurrentlyActive()) {
                    hasActiveBlock = true;
                }
                if (sb.length() > 0) sb.append("\n");
                String reason = (b.getReason() != null && !b.getReason().trim().isEmpty())
                        ? " (" + b.getReason().trim() + ")" : "";
                sb.append("⛔ ").append(b.getFormattedBlockedNotice()).append(reason);
            }
        }

        if (spot.isBlocked() || hasActiveBlock) {
            binding.btnReserveNow.setEnabled(false);
            binding.btnReserveNow.setText(R.string.btn_spot_currently_blocked);
            if (sb.length() > 0) {
                binding.textPreviewBlockedInfo.setText(sb.toString());
            } else {
                binding.textPreviewBlockedInfo.setText(R.string.warning_spot_currently_blocked);
            }
            binding.textPreviewBlockedInfo.setVisibility(View.VISIBLE);
        } else if (spot.isOccupied() || spot.isReserved()) {
            binding.btnReserveNow.setEnabled(false);
            binding.btnReserveNow.setText(R.string.status_occupied);
            binding.textPreviewBlockedInfo.setText(R.string.warning_spot_currently_occupied);
            binding.textPreviewBlockedInfo.setVisibility(View.VISIBLE);
        } else {
            if (sb.length() > 0) {
                binding.textPreviewBlockedInfo.setText(sb.toString());
                binding.textPreviewBlockedInfo.setVisibility(View.VISIBLE);
            } else {
                binding.textPreviewBlockedInfo.setVisibility(View.GONE);
            }
            binding.btnReserveNow.setEnabled(true);
            binding.btnReserveNow.setText(R.string.btn_reserve_now);
        }

        binding.cardPlacePreview.setVisibility(View.VISIBLE);
    }

    /**
     * Displays a modal dialog with user reviews for the selected parking spot.
     * Loads cached reviews from Room and fetches the latest page from REST backend.
     *
     * @param spotId      Unique spot identifier
     * @param spotTitle   Display title of the spot
     * @param rating      Current average rating
     * @param ratingCount Total count of reviews
     */
    private void showSpotReviewsDialog(@NonNull String spotId, @Nullable String spotTitle, double rating, int ratingCount) {
        DialogSpotReviewsBinding dialogBinding = DialogSpotReviewsBinding.inflate(LayoutInflater.from(this));

        if (spotTitle != null && !spotTitle.trim().isEmpty()) {
            dialogBinding.textDialogSpotTitle.setText(spotTitle);
        } else {
            dialogBinding.textDialogSpotTitle.setText(R.string.title_spot_reviews);
        }

        dialogBinding.textDialogSpotRating.setText(
                String.format(Locale.getDefault(), "★ %.1f (%d opinii)", rating, ratingCount)
        );

        ReviewAdapter reviewAdapter = new ReviewAdapter();
        dialogBinding.recyclerSpotReviews.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.recyclerSpotReviews.setAdapter(reviewAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnCloseReviews.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnDismissDialog.setOnClickListener(v -> dialog.dismiss());

        // Show progress while fetching from network
        dialogBinding.progressReviews.setVisibility(View.VISIBLE);

        // SSOT: Observe local Room cache via ReviewRepository
        ReviewRepository reviewRepo = ReviewRepository.getInstance(this);
        reviewRepo.getReviewsFromRoom(spotId).observe(this, cachedReviews -> {
            if (cachedReviews != null && !cachedReviews.isEmpty()) {
                reviewAdapter.setReviews(cachedReviews);
                dialogBinding.textNoReviews.setVisibility(View.GONE);
                dialogBinding.recyclerSpotReviews.setVisibility(View.VISIBLE);
            } else {
                dialogBinding.textNoReviews.setVisibility(View.VISIBLE);
                dialogBinding.recyclerSpotReviews.setVisibility(View.GONE);
            }
        });

        // Trigger remote sync via GET /api/spots/{spotId}/reviews
        reviewRepo.fetchSpotReviews(spotId, 0, 20).observe(this, resource -> {
            if (resource != null && resource.status != Resource.Status.LOADING) {
                dialogBinding.progressReviews.setVisibility(View.GONE);
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeCheckHandler.removeCallbacksAndMessages(null);
        googleMap = null;
        binding = null;
    }
}
