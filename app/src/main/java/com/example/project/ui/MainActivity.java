package com.example.project.ui;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapter.AddressSuggestionsAdapter;
import com.example.project.adapter.NearbySpotsAdapter;
import com.example.project.adapter.ReviewAdapter;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.databinding.ActivityMainBinding;
import com.example.project.databinding.DialogMapFiltersBinding;
import com.example.project.databinding.DialogNearbySpotsListBinding;
import com.example.project.databinding.DialogSpotReviewsBinding;
import com.example.project.network.Resource;
import com.example.project.repository.AvailabilityRepository;
import com.example.project.repository.ReviewRepository;
import com.example.project.repository.SpotRepository;
import com.example.project.utils.MapSpotFilterHelper;
import com.example.project.utils.ViewAnimationUtils;
import com.example.project.viewmodel.SearchViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, List<AvailabilityRuleEntity>> spotRulesMap = new HashMap<>();
    private final MapSpotFilterHelper.FilterCriteria filterCriteria = new MapSpotFilterHelper.FilterCriteria();
    private final List<ParkingPlaceEntity> latestFilteredSpots = new ArrayList<>();

    private ParkingPlaceEntity selectedSpotForBooking = null;
    @Nullable
    private LatLng searchedAnchorPoint = null;
    @Nullable
    private String searchedAnchorLabel = null;
    @Nullable
    private Marker searchedAnchorMarker = null;
    @Nullable
    private Circle searchRadiusCircle = null;
    @Nullable
    private String selectedCityFilter = null;
    @NonNull
    private LatLng activeQueryCenter = new LatLng(52.2297, 21.0122);
    @Nullable
    private LatLng lastIdleLoadedCenter = null;
    private boolean suppressNextCameraIdle = false;
    private boolean pendingSearchCameraFit = false;

    private static final long SUGGESTION_DEBOUNCE_MS = 380L;

    private NearbySpotsAdapter nearbyStripAdapter;
    private AddressSuggestionsAdapter suggestionsAdapter;
    private final Handler suggestionHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingSuggestionRunnable = null;
    private boolean suppressSuggestionQuery = false;
    private boolean suggestionsDismissedManually = false;
    @Nullable
    private NearbySpotsAdapter sheetListAdapter = null;
    @Nullable
    private TextView sheetListSubtitle = null;
    @Nullable
    private View sheetListEmptyView = null;
    @Nullable
    private RecyclerView sheetListRecycler = null;

    private static final Map<String, LatLng> CITY_CENTERS = new LinkedHashMap<>();
    static {
        CITY_CENTERS.put("Warszawa", new LatLng(52.2297, 21.0122));
        CITY_CENTERS.put("Kraków", new LatLng(50.0647, 19.9450));
        CITY_CENTERS.put("Lublin", new LatLng(51.2465, 22.5684));
        CITY_CENTERS.put("Wrocław", new LatLng(51.1079, 17.0385));
        CITY_CENTERS.put("Poznań", new LatLng(52.4064, 16.9252));
        CITY_CENTERS.put("Gdańsk", new LatLng(54.3520, 18.6466));
    }

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
                Boolean fine = grants.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarse = grants.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    enableMyLocationLayerIfPermitted();
                    moveToUserLocationOrFallback();
                } else {
                    moveToFallbackAnchor();
                }
            });

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
            LatLng reloadTarget = searchedAnchorPoint != null
                    ? searchedAnchorPoint
                    : googleMap.getCameraPosition().target;
            if (reloadTarget != null && (reloadTarget.latitude != 0.0 || reloadTarget.longitude != 0.0)) {
                viewModel.loadNearbySpots(reloadTarget.latitude, reloadTarget.longitude, filterCriteria.getRadiusMeters());
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
            String userEmail = sessionManager.getUserEmail();
            com.example.project.repository.VehicleRepository.getInstance(this).fetchVehicles(userEmail);
            com.example.project.repository.ReservationRepository.getInstance(this).fetchReservations(userEmail);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeCheckHandler.removeCallbacks(timeCheckRunnable);
        cancelPendingSuggestions();
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

        setupNearbyStrip();
        setupAddressSuggestions();
        setupSearchView();
        setupCityChips();
        setupClickListeners();
        observeViewModel();
        updateFilterBadge();
    }

    private void setupNearbyStrip() {
        if (binding == null) return;
        nearbyStripAdapter = new NearbySpotsAdapter(true, spot -> {
            focusSpotOnMap(spot);
            showSpotPreviewCard(spot);
        });
        binding.recyclerNearbyStrip.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerNearbyStrip.setAdapter(nearbyStripAdapter);
    }

    private void setupCityChips() {
        if (binding == null) return;
        binding.chipCityAll.setOnClickListener(v -> onCitySelected(null, binding.chipCityAll));
        binding.chipCityWarszawa.setOnClickListener(v -> onCitySelected("Warszawa", binding.chipCityWarszawa));
        binding.chipCityKrakow.setOnClickListener(v -> onCitySelected("Kraków", binding.chipCityKrakow));
        binding.chipCityLublin.setOnClickListener(v -> onCitySelected("Lublin", binding.chipCityLublin));
        binding.chipCityWroclaw.setOnClickListener(v -> onCitySelected("Wrocław", binding.chipCityWroclaw));
        binding.chipCityPoznan.setOnClickListener(v -> onCitySelected("Poznań", binding.chipCityPoznan));
        binding.chipCityGdansk.setOnClickListener(v -> onCitySelected("Gdańsk", binding.chipCityGdansk));
    }

    private void onCitySelected(@Nullable String city, @NonNull MaterialButton activeButton) {
        selectedCityFilter = city;
        searchedAnchorPoint = null;
        searchedAnchorLabel = null;
        selectedSpotForBooking = null;
        hideAddressSuggestions(true);
        if (binding != null) {
            binding.cardPlacePreview.setVisibility(View.GONE);
            if (city != null) {
                suppressSuggestionQuery = true;
                binding.searchView.setQuery("", false);
                suppressSuggestionQuery = false;
                binding.searchView.clearFocus();
            }
        }
        updateCityChipStyles(activeButton);

        if (city != null && CITY_CENTERS.containsKey(city)) {
            LatLng cityCenter = CITY_CENTERS.get(city);
            if (cityCenter != null) {
                activeQueryCenter = cityCenter;
                animateCameraSafely(cityCenter, 12.2f);
                viewModel.loadNearbySpots(cityCenter.latitude, cityCenter.longitude,
                        Math.max(filterCriteria.getRadiusMeters(), 12000.0));
            }
        } else {
            refreshMapDisplay();
        }
        refreshMapDisplay();
    }

    private void updateCityChipStyles(@NonNull MaterialButton selectedBtn) {
        if (binding == null) return;
        MaterialButton[] buttons = new MaterialButton[]{
                binding.chipCityAll,
                binding.chipCityWarszawa,
                binding.chipCityKrakow,
                binding.chipCityLublin,
                binding.chipCityWroclaw,
                binding.chipCityPoznan,
                binding.chipCityGdansk
        };
        int selectedBg = ContextCompat.getColor(this, R.color.brand_slate_950);
        int selectedText = ContextCompat.getColor(this, R.color.white);
        int unselectedBg = ContextCompat.getColor(this, R.color.card_background);
        int unselectedText = ContextCompat.getColor(this, R.color.brand_slate_700);
        int unselectedStroke = ContextCompat.getColor(this, R.color.brand_slate_200);

        for (MaterialButton btn : buttons) {
            boolean isSelected = btn == selectedBtn;
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isSelected ? selectedBg : unselectedBg));
            btn.setTextColor(isSelected ? selectedText : unselectedText);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(isSelected ? selectedBg : unselectedStroke));
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnMapFilters.setOnClickListener(v -> showMapFiltersBottomSheet());

        binding.btnOpenNearbyList.setOnClickListener(v -> showNearbyListBottomSheet());

        binding.btnClearAddressSearch.setOnClickListener(v -> clearAddressSearch(true));

        binding.cardRecenterMap.setOnClickListener(v -> {
            boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            if (hasFine || hasCoarse) {
                moveToUserLocationOrFallback();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        binding.btnReserveNow.setOnClickListener(v -> {
            if (selectedSpotForBooking != null) {
                if (!isSpotActiveAtCurrentMoment(selectedSpotForBooking)) {
                    Toast.makeText(this, R.string.error_spot_inactive, Toast.LENGTH_SHORT).show();
                    if (binding.cardPlacePreview != null) {
                        binding.cardPlacePreview.setVisibility(View.GONE);
                    }
                    selectedSpotForBooking = null;
                    updateBottomOverlaysVisibility();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, ReservePlaceActivity.class);
                intent.putExtra(ReservePlaceActivity.EXTRA_PLACE_ID, selectedSpotForBooking.getId());
                startActivity(intent);
            }
        });

        binding.btnClosePreview.setOnClickListener(v -> {
            ViewAnimationUtils.playBounceAnimation(v, () -> {
                if (binding.cardPlacePreview != null) {
                    binding.cardPlacePreview.setVisibility(View.GONE);
                }
                selectedSpotForBooking = null;
                updateBottomOverlaysVisibility();
            });
        });

        binding.btnPreviewReviews.setOnClickListener(v -> {
            if (selectedSpotForBooking != null) {
                showSpotReviewsDialog(
                        selectedSpotForBooking.getId(),
                        selectedSpotForBooking.getTitle(),
                        selectedSpotForBooking.getRating(),
                        selectedSpotForBooking.getRatingCount()
                );
            }
        });
    }

    private void setupAddressSuggestions() {
        if (binding == null) return;
        suggestionsAdapter = new AddressSuggestionsAdapter(this::onAddressSuggestionSelected);
        binding.recyclerSearchSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSearchSuggestions.setAdapter(suggestionsAdapter);
        binding.btnCloseSuggestions.setOnClickListener(v -> {
            suggestionsDismissedManually = true;
            hideAddressSuggestions(false);
        });
    }

    private void onAddressSuggestionSelected(@NonNull SearchViewModel.AddressSuggestion suggestion) {
        cancelPendingSuggestions();
        suggestionsDismissedManually = true;
        if (binding != null) {
            suppressSuggestionQuery = true;
            binding.searchView.setQuery(suggestion.getFullLabel(), false);
            suppressSuggestionQuery = false;
            binding.searchView.clearFocus();
            updateCityChipStyles(binding.chipCityAll);
        }
        hideAddressSuggestions(false);
        selectedCityFilter = null;
        viewModel.selectSuggestion(suggestion, filterCriteria.getRadiusMeters());
    }

    private void cancelPendingSuggestions() {
        if (pendingSuggestionRunnable != null) {
            suggestionHandler.removeCallbacks(pendingSuggestionRunnable);
            pendingSuggestionRunnable = null;
        }
    }

    private void hideAddressSuggestions(boolean clearViewModelSuggestions) {
        cancelPendingSuggestions();
        if (binding != null) {
            binding.cardSearchSuggestions.setVisibility(View.GONE);
            binding.textSuggestionsEmpty.setVisibility(View.GONE);
            binding.progressSuggestions.setVisibility(View.GONE);
            binding.textSuggestionsHeader.setText(R.string.search_suggestions_header);
        }
        if (clearViewModelSuggestions && viewModel != null) {
            viewModel.clearSuggestions();
        }
    }

    private void updateSuggestionsDropdownHeight(int count) {
        if (binding == null) return;
        ViewGroup.LayoutParams lp = binding.recyclerSearchSuggestions.getLayoutParams();
        int desiredHeight = count <= 4
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : Math.round(getResources().getDisplayMetrics().density * 280f);
        if (lp != null && lp.height != desiredHeight) {
            lp.height = desiredHeight;
            binding.recyclerSearchSuggestions.setLayoutParams(lp);
            binding.recyclerSearchSuggestions.requestLayout();
        }
    }

    private void setupSearchView() {
        if (binding == null || binding.searchView == null) {
            return;
        }

        binding.searchView.setSubmitButtonEnabled(true);
        binding.cardSearch.setOnClickListener(v -> binding.searchView.requestFocus());

        View searchText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && binding != null) {
                    CharSequence q = binding.searchView.getQuery();
                    if (q != null && q.toString().trim().length() >= 3
                            && suggestionsAdapter != null && suggestionsAdapter.getItemCount() > 0
                            && !suggestionsDismissedManually) {
                        binding.cardSearchSuggestions.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        View closeBtn = binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                suggestionsDismissedManually = false;
                suppressSuggestionQuery = true;
                binding.searchView.setQuery("", false);
                suppressSuggestionQuery = false;
                binding.searchView.clearFocus();
                clearAddressSearch(false);
            });
        }

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                cancelPendingSuggestions();
                if (query != null && !query.trim().isEmpty()) {
                    suggestionsDismissedManually = false;
                    if (binding != null) {
                        binding.textSuggestionsHeader.setText(R.string.search_suggestions_header);
                        binding.searchView.clearFocus();
                        updateCityChipStyles(binding.chipCityAll);
                    }
                    selectedCityFilter = null;
                    viewModel.submitAddressSearch(
                            getApplicationContext(),
                            query.trim(),
                            filterCriteria.getRadiusMeters(),
                            getBestDeviceLocationLatLng(),
                            getCurrentMapCenterLatLng());
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_empty_query, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (suppressSuggestionQuery) {
                    return false;
                }
                suggestionsDismissedManually = false;
                if (binding != null) {
                    binding.textSuggestionsHeader.setText(R.string.search_suggestions_header);
                }
                cancelPendingSuggestions();
                String trimmed = newText != null ? newText.trim() : "";
                if (trimmed.length() < 3) {
                    hideAddressSuggestions(true);
                    return false;
                }
                pendingSuggestionRunnable = () -> viewModel.fetchAddressSuggestions(
                        getApplicationContext(),
                        trimmed,
                        getBestDeviceLocationLatLng(),
                        getCurrentMapCenterLatLng());
                suggestionHandler.postDelayed(pendingSuggestionRunnable, SUGGESTION_DEBOUNCE_MS);
                return false;
            }
        });
    }

    private void clearAddressSearch(boolean clearTextField) {
        searchedAnchorPoint = null;
        searchedAnchorLabel = null;
        hideAddressSuggestions(true);
        if (clearTextField && binding != null) {
            suppressSuggestionQuery = true;
            binding.searchView.setQuery("", false);
            suppressSuggestionQuery = false;
            binding.searchView.clearFocus();
        }
        refreshMapDisplay();
    }

    @Nullable
    @SuppressWarnings("MissingPermission")
    private LatLng getBestDeviceLocationLatLng() {
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!hasFine && !hasCoarse) {
            return null;
        }
        Location best = null;
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return null;
            }
            List<String> providers = new ArrayList<>(lm.getProviders(true));
            if (!providers.contains(LocationManager.GPS_PROVIDER)) {
                providers.add(LocationManager.GPS_PROVIDER);
            }
            if (!providers.contains(LocationManager.NETWORK_PROVIDER)) {
                providers.add(LocationManager.NETWORK_PROVIDER);
            }
            if (!providers.contains(LocationManager.PASSIVE_PROVIDER)) {
                providers.add(LocationManager.PASSIVE_PROVIDER);
            }
            for (String provider : providers) {
                try {
                    Location loc = lm.getLastKnownLocation(provider);
                    if (loc == null || (loc.getLatitude() == 0.0 && loc.getLongitude() == 0.0)) {
                        continue;
                    }
                    if (best == null || loc.getTime() >= best.getTime()) {
                        best = loc;
                    }
                } catch (SecurityException | IllegalArgumentException ignored) {
                }
            }
        } catch (SecurityException ignored) {
        }
        return best != null ? new LatLng(best.getLatitude(), best.getLongitude()) : null;
    }

    @NonNull
    private LatLng getCurrentMapCenterLatLng() {
        if (googleMap != null && googleMap.getCameraPosition() != null
                && googleMap.getCameraPosition().target != null) {
            return googleMap.getCameraPosition().target;
        }
        return activeQueryCenter;
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocationLayerIfPermitted() {
        if (googleMap == null) return;
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (hasFine || hasCoarse) {
            try {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            } catch (SecurityException ignored) {
            }
        }
    }

    private void moveToUserLocationOrFallback() {
        LatLng userLatLng = getBestDeviceLocationLatLng();
        if (userLatLng != null) {
            activeQueryCenter = userLatLng;
            animateCameraSafely(userLatLng, 14f);
            viewModel.loadNearbySpots(userLatLng.latitude, userLatLng.longitude, filterCriteria.getRadiusMeters());
        } else {
            moveToFallbackAnchor();
        }
    }

    private void moveToFallbackAnchor() {
        LatLng target = searchedAnchorPoint != null ? searchedAnchorPoint : activeQueryCenter;
        animateCameraSafely(target, 13.5f);
        viewModel.loadNearbySpots(target.latitude, target.longitude, filterCriteria.getRadiusMeters());
    }

    private void animateCameraSafely(@NonNull LatLng target, float zoom) {
        if (googleMap == null) return;
        suppressNextCameraIdle = true;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom));
    }

    private void observeViewModel() {
        // Observe geocoding search results to pin address, zoom, and surface all nearby parkings
        viewModel.getSearchResultLiveData().observe(this, result -> {
            if (result == null) return;

            if (result.isSuccess()) {
                searchedAnchorPoint = result.getLatLng();
                searchedAnchorLabel = result.getTitle();
                activeQueryCenter = searchedAnchorPoint;
                selectedSpotForBooking = null;
                pendingSearchCameraFit = true;
                suggestionsDismissedManually = true;
                hideAddressSuggestions(false);
                if (binding != null) {
                    binding.cardPlacePreview.setVisibility(View.GONE);
                }
                refreshMapDisplay();
                fitCameraToSearchResults(searchedAnchorPoint, latestFilteredSpots);
            } else if (result.getErrorMessage() != null) {
                Toast.makeText(MainActivity.this, result.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsSuggestionsLoadingLiveData().observe(this, loading -> {
            if (binding == null) return;
            boolean isLoading = Boolean.TRUE.equals(loading);
            binding.progressSuggestions.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            CharSequence q = binding.searchView.getQuery();
            String trimmed = q != null ? q.toString().trim() : "";
            if (isLoading && trimmed.length() >= 3 && !suggestionsDismissedManually) {
                binding.textSuggestionsEmpty.setVisibility(View.GONE);
                binding.cardSearchSuggestions.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSuggestionsLiveData().observe(this, suggestions -> {
            if (binding == null || suggestionsAdapter == null) return;
            List<SearchViewModel.AddressSuggestion> safeList =
                    suggestions != null ? suggestions : Collections.emptyList();
            suggestionsAdapter.submitList(safeList);

            CharSequence q = binding.searchView.getQuery();
            String trimmed = q != null ? q.toString().trim() : "";
            boolean isLoading = Boolean.TRUE.equals(viewModel.getIsSuggestionsLoadingLiveData().getValue());

            if (!safeList.isEmpty() && !suggestionsDismissedManually && trimmed.length() >= 3) {
                updateSuggestionsDropdownHeight(safeList.size());
                binding.recyclerSearchSuggestions.setVisibility(View.VISIBLE);
                binding.textSuggestionsEmpty.setVisibility(View.GONE);
                binding.cardSearchSuggestions.setVisibility(View.VISIBLE);
            } else if (!suggestionsDismissedManually && trimmed.length() >= 3
                    && binding.searchView.hasFocus() && !isLoading) {
                binding.recyclerSearchSuggestions.setVisibility(View.GONE);
                binding.textSuggestionsEmpty.setVisibility(View.VISIBLE);
                binding.cardSearchSuggestions.setVisibility(View.VISIBLE);
            } else if (!isLoading) {
                binding.cardSearchSuggestions.setVisibility(View.GONE);
            }
        });

        viewModel.getSuggestionNoticeLiveData().observe(this, notice -> {
            if (notice == null || notice.trim().isEmpty() || binding == null) return;
            suggestionsDismissedManually = false;
            binding.textSuggestionsHeader.setText(notice);
            binding.textSuggestionsEmpty.setVisibility(View.GONE);
            binding.recyclerSearchSuggestions.setVisibility(View.VISIBLE);
            binding.cardSearchSuggestions.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, notice, Toast.LENGTH_SHORT).show();
            viewModel.consumeSuggestionNotice();
        });

        // Observe reactive parking spots from local Room Database (Single Source of Truth)
        viewModel.getSpotsLiveData().observe(this, spots -> {
            if (spots != null) {
                displaySpotsOnMap(spots);
                if (pendingSearchCameraFit && searchedAnchorPoint != null
                        && selectedSpotForBooking == null && !latestFilteredSpots.isEmpty()) {
                    pendingSearchCameraFit = false;
                    fitCameraToSearchResults(searchedAnchorPoint, latestFilteredSpots);
                }
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
            refreshMapDisplay();
            if (selectedSpotForBooking != null) {
                showSpotPreviewCard(selectedSpotForBooking);
            }
        });

        // Observe spot availability rules for working-hours filtering & card badges
        viewModel.getAvailabilityRulesLiveData().observe(this, rules -> {
            spotRulesMap.clear();
            if (rules != null) {
                for (AvailabilityRuleEntity rule : rules) {
                    if (rule == null || rule.getSpotId() == null) continue;
                    List<AvailabilityRuleEntity> list = spotRulesMap.get(rule.getSpotId());
                    if (list == null) {
                        list = new ArrayList<>();
                        spotRulesMap.put(rule.getSpotId(), list);
                    }
                    list.add(rule);
                }
            }
            refreshMapDisplay();
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

    private void refreshMapDisplay() {
        List<ParkingPlaceEntity> currentSpots = viewModel.getSpotsLiveData().getValue();
        displaySpotsOnMap(currentSpots != null ? currentSpots : Collections.emptyList());
    }

    /**
     * Renders filtered parking spot markers, optional searched-address pin/radius circle,
     * and updates nearby banner + horizontal strip + open nearby list bottom sheet.
     */
    private void displaySpotsOnMap(@Nullable List<ParkingPlaceEntity> spots) {
        if (googleMap == null || spots == null) return;

        googleMap.clear();
        markerSpotMap.clear();
        searchedAnchorMarker = null;
        searchRadiusCircle = null;

        long nowMs = System.currentTimeMillis();
        Map<String, SpotRepository.SpotMetadata> metadataMap = viewModel.getSpotMetadataMap();

        if (searchedAnchorPoint != null) {
            searchRadiusCircle = googleMap.addCircle(new CircleOptions()
                    .center(searchedAnchorPoint)
                    .radius(filterCriteria.getRadiusMeters())
                    .strokeColor(0xFF2563EB)
                    .strokeWidth(4f)
                    .fillColor(0x162563EB));

            searchedAnchorMarker = googleMap.addMarker(new MarkerOptions()
                    .position(searchedAnchorPoint)
                    .title(searchedAnchorLabel != null ? searchedAnchorLabel : getString(R.string.nearby_searched_address_label))
                    .snippet(getString(R.string.nearby_searched_address_label))
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        LatLng distanceRef = searchedAnchorPoint != null
                ? searchedAnchorPoint
                : (googleMap.getCameraPosition() != null ? googleMap.getCameraPosition().target : activeQueryCenter);

        LatLng filterRadiusAnchor = (searchedAnchorPoint != null || Math.abs(filterCriteria.getRadiusMeters() - 5000.0) > 1.0)
                ? distanceRef : null;

        List<ParkingPlaceEntity> filteredSpots = new ArrayList<>();
        for (ParkingPlaceEntity spot : spots) {
            if (!isSpotActiveAtCurrentMoment(spot)) {
                continue;
            }
            List<AvailabilityRuleEntity> rules = spotRulesMap.get(spot.getId());
            List<BlockedPeriodEntity> blocks = spotBlockedMap.get(spot.getId());
            SpotRepository.SpotMetadata meta = metadataMap.get(spot.getId());

            if (!MapSpotFilterHelper.matches(spot, rules, blocks, meta, filterCriteria, filterRadiusAnchor, selectedCityFilter, nowMs)) {
                continue;
            }
            filteredSpots.add(spot);
        }

        MapSpotFilterHelper.sortSpots(filteredSpots, distanceRef, filterCriteria.getSortBy());

        latestFilteredSpots.clear();
        latestFilteredSpots.addAll(filteredSpots);

        if (selectedSpotForBooking != null && !containsSpotId(filteredSpots, selectedSpotForBooking.getId())) {
            if (binding != null && binding.cardPlacePreview != null) {
                binding.cardPlacePreview.setVisibility(View.GONE);
            }
            selectedSpotForBooking = null;
        }

        for (ParkingPlaceEntity spot : filteredSpots) {
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

            List<AvailabilityRuleEntity> rules = spotRulesMap.get(spot.getId());
            String hoursSummary = MapSpotFilterHelper.formatWorkingHoursSummary(rules, nowMs);
            StringBuilder snippet = new StringBuilder();
            snippet.append(String.format(Locale.getDefault(), "%.2f zł/h", spot.getPrice()));
            if (distanceRef != null && (distanceRef.latitude != 0.0 || distanceRef.longitude != 0.0)) {
                double dist = MapSpotFilterHelper.distanceMeters(
                        distanceRef.latitude, distanceRef.longitude, spot.getLatitude(), spot.getLongitude());
                snippet.append(" • ").append(MapSpotFilterHelper.formatDistance(dist));
            }
            snippet.append(" • ").append(hoursSummary);
            if (upcomingBlock != null) {
                snippet.append(" • ⛔ ").append(upcomingBlock.getFormattedBlockedNotice());
            }

            LatLng pos = new LatLng(spot.getLatitude(), spot.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(spot.getTitle())
                    .snippet(snippet.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            if (marker != null) {
                markerSpotMap.put(marker.getId(), spot);
            }
        }

        if (nearbyStripAdapter != null) {
            nearbyStripAdapter.submitData(latestFilteredSpots, distanceRef, spotRulesMap);
        }
        if (sheetListAdapter != null) {
            sheetListAdapter.submitData(latestFilteredSpots, distanceRef, spotRulesMap);
            updateOpenListSheetHeader();
        }
        updateNearbyBannerAndStripVisibility();
    }

    private boolean containsSpotId(@NonNull List<ParkingPlaceEntity> list, @Nullable String id) {
        if (id == null) return false;
        for (ParkingPlaceEntity p : list) {
            if (p != null && id.equals(p.getId())) return true;
        }
        return false;
    }

    private void updateNearbyBannerAndStripVisibility() {
        if (binding == null) return;

        boolean hasSearchOrFilter = searchedAnchorPoint != null
                || selectedCityFilter != null
                || filterCriteria.countActiveFilters() > 0;

        if (hasSearchOrFilter) {
            String contextLabel;
            if (searchedAnchorLabel != null && !searchedAnchorLabel.trim().isEmpty()) {
                contextLabel = searchedAnchorLabel.trim();
            } else if (selectedCityFilter != null) {
                contextLabel = selectedCityFilter;
            } else {
                contextLabel = getString(R.string.nearby_list_title);
            }

            String radiusText = MapSpotFilterHelper.formatDistance(filterCriteria.getRadiusMeters());
            if (latestFilteredSpots.isEmpty()) {
                binding.textNearbyBanner.setText(getString(R.string.nearby_banner_empty, contextLabel, radiusText));
            } else {
                binding.textNearbyBanner.setText(getString(
                        R.string.nearby_banner_format, contextLabel, latestFilteredSpots.size(), radiusText));
            }
            binding.btnOpenNearbyList.setText(getString(R.string.nearby_list_btn, latestFilteredSpots.size()));
            binding.cardNearbyBanner.setVisibility(View.VISIBLE);
        } else {
            binding.cardNearbyBanner.setVisibility(View.GONE);
        }

        updateBottomOverlaysVisibility();
    }

    private void updateBottomOverlaysVisibility() {
        if (binding == null) return;
        boolean hasResultsToShow = !latestFilteredSpots.isEmpty()
                && (searchedAnchorPoint != null
                || selectedCityFilter != null
                || filterCriteria.countActiveFilters() > 0);
        boolean showStrip = hasResultsToShow && selectedSpotForBooking == null;
        binding.layoutNearbyStrip.setVisibility(showStrip ? View.VISIBLE : View.GONE);
    }

    private void fitCameraToSearchResults(@Nullable LatLng anchor, @NonNull List<ParkingPlaceEntity> spots) {
        if (googleMap == null || anchor == null) return;
        suppressNextCameraIdle = true;

        if (!spots.isEmpty()) {
            try {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(anchor);
                boolean hasDistinctPoint = false;
                for (ParkingPlaceEntity spot : spots) {
                    LatLng p = new LatLng(spot.getLatitude(), spot.getLongitude());
                    builder.include(p);
                    if (Math.abs(p.latitude - anchor.latitude) > 0.0005
                            || Math.abs(p.longitude - anchor.longitude) > 0.0005) {
                        hasDistinctPoint = true;
                    }
                }
                if (hasDistinctPoint) {
                    int padding = Math.round(getResources().getDisplayMetrics().density * 72);
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
                    return;
                }
            } catch (IllegalStateException ignored) {
            }
        }

        double r = filterCriteria.getRadiusMeters();
        float zoom = r <= 1500.0 ? 14.5f : (r <= 3500.0 ? 13.5f : (r <= 6000.0 ? 12.8f : (r <= 12000.0 ? 11.8f : 10.6f)));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(anchor, zoom));
    }

    private void focusSpotOnMap(@NonNull ParkingPlaceEntity spot) {
        if (googleMap == null) return;
        suppressNextCameraIdle = true;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(spot.getLatitude(), spot.getLongitude()), 15.2f));
    }

    private void updateFilterBadge() {
        if (binding == null) return;
        int count = filterCriteria.countActiveFilters();
        if (count > 0) {
            binding.badgeFilterCount.setText(String.valueOf(count));
            binding.badgeFilterCount.setVisibility(View.VISIBLE);
        } else {
            binding.badgeFilterCount.setVisibility(View.GONE);
        }
    }

    private void showMapFiltersBottomSheet() {
        hideAddressSuggestions(false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogMapFiltersBinding fb = DialogMapFiltersBinding.inflate(LayoutInflater.from(this));
        dialog.setContentView(fb.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        final int[] customStartMin = new int[]{filterCriteria.getCustomStartMinutes()};
        final int[] customEndMin = new int[]{filterCriteria.getCustomEndMinutes()};

        Runnable syncUiFromCriteria = () -> {
            switch (filterCriteria.getSortBy()) {
                case PRICE_ASC:
                    fb.chipSortPrice.setChecked(true);
                    break;
                case RATING_DESC:
                    fb.chipSortRating.setChecked(true);
                    break;
                case DISTANCE:
                default:
                    fb.chipSortDistance.setChecked(true);
                    break;
            }

            double r = filterCriteria.getRadiusMeters();
            if (r <= 1500.0) fb.chipRadius1km.setChecked(true);
            else if (r <= 3500.0) fb.chipRadius3km.setChecked(true);
            else if (r <= 6000.0) fb.chipRadius5km.setChecked(true);
            else if (r <= 12000.0) fb.chipRadius10km.setChecked(true);
            else fb.chipRadius25km.setChecked(true);

            fb.editPriceMin.setText(filterCriteria.getMinPrice() != null
                    ? formatCompactNumber(filterCriteria.getMinPrice()) : "");
            fb.editPriceMax.setText(filterCriteria.getMaxPrice() != null
                    ? formatCompactNumber(filterCriteria.getMaxPrice()) : "");
            fb.chipGroupPriceQuick.clearCheck();
            if (filterCriteria.getMinPrice() == null && filterCriteria.getMaxPrice() == null) {
                fb.chipPriceAny.setChecked(true);
            } else if (filterCriteria.getMinPrice() == null && filterCriteria.getMaxPrice() != null) {
                double mp = filterCriteria.getMaxPrice();
                if (Math.abs(mp - 5.0) < 0.01) fb.chipPrice5.setChecked(true);
                else if (Math.abs(mp - 10.0) < 0.01) fb.chipPrice10.setChecked(true);
                else if (Math.abs(mp - 15.0) < 0.01) fb.chipPrice15.setChecked(true);
                else if (Math.abs(mp - 25.0) < 0.01) fb.chipPrice25.setChecked(true);
            }

            switch (filterCriteria.getHoursMode()) {
                case OPEN_NOW:
                    fb.chipHoursNow.setChecked(true);
                    fb.layoutCustomHours.setVisibility(View.GONE);
                    break;
                case OPEN_24_7:
                    fb.chipHoursAllDay.setChecked(true);
                    fb.layoutCustomHours.setVisibility(View.GONE);
                    break;
                case CUSTOM_WINDOW:
                    fb.chipHoursCustom.setChecked(true);
                    fb.layoutCustomHours.setVisibility(View.VISIBLE);
                    break;
                case ANY:
                default:
                    fb.chipHoursAny.setChecked(true);
                    fb.layoutCustomHours.setVisibility(View.GONE);
                    break;
            }
            if (filterCriteria.getCustomDayOffset() == 1) {
                fb.chipDayTomorrow.setChecked(true);
            } else {
                fb.chipDayToday.setChecked(true);
            }
            customStartMin[0] = filterCriteria.getCustomStartMinutes();
            customEndMin[0] = filterCriteria.getCustomEndMinutes();
            fb.btnHourFrom.setText(getString(R.string.map_filter_hour_from) + ": "
                    + MapSpotFilterHelper.formatMinutes(customStartMin[0]));
            fb.btnHourTo.setText(getString(R.string.map_filter_hour_to) + ": "
                    + MapSpotFilterHelper.formatMinutes(customEndMin[0]));

            fb.editMinLengthCm.setText(filterCriteria.getMinLengthCm() > 0
                    ? String.valueOf(filterCriteria.getMinLengthCm()) : "");
            fb.editMinWidthCm.setText(filterCriteria.getMinWidthCm() > 0
                    ? String.valueOf(filterCriteria.getMinWidthCm()) : "");
            fb.editMinHeightCm.setText(filterCriteria.getMinHeightCm() > 0
                    ? String.valueOf(filterCriteria.getMinHeightCm()) : "");

            int len = filterCriteria.getMinLengthCm();
            int wid = filterCriteria.getMinWidthCm();
            if (len >= 550 && wid >= 260) fb.chipSizeLarge.setChecked(true);
            else if (len >= 500 && wid >= 250) fb.chipSizeStandard.setChecked(true);
            else if (len >= 450 && wid >= 220) fb.chipSizeCompact.setChecked(true);
            else fb.chipSizeAny.setChecked(true);

            Set<String> reqAmenities = filterCriteria.getRequiredAmenities();
            fb.chipAmenityCctv.setChecked(reqAmenities.contains("CCTV"));
            fb.chipAmenityLighting.setChecked(reqAmenities.contains("LIGHTING"));
            fb.chipAmenityEv.setChecked(reqAmenities.contains("EV_CHARGER"));
            fb.chipAmenityCovered.setChecked(reqAmenities.contains("COVERED"));
            fb.chipAmenityUnderground.setChecked(reqAmenities.contains("UNDERGROUND"));
            fb.chipAmenityGate.setChecked(reqAmenities.contains("GATE"));
            fb.chipAmenityDisabled.setChecked(reqAmenities.contains("DISABLED_ACCESS"));
            fb.chipAmenityWide.setChecked(reqAmenities.contains("WIDE_SPACE"));

            double minR = filterCriteria.getMinRating();
            if (minR >= 4.45) fb.chipRating45.setChecked(true);
            else if (minR >= 3.95) fb.chipRating40.setChecked(true);
            else if (minR >= 3.45) fb.chipRating35.setChecked(true);
            else fb.chipRatingAny.setChecked(true);

            fb.switchOnlyVerified.setChecked(filterCriteria.isOnlyVerified());
            fb.switchOnlyAutoAccept.setChecked(filterCriteria.isOnlyAutoAccept());
        };
        syncUiFromCriteria.run();

        fb.chipPriceAny.setOnClickListener(v -> {
            fb.editPriceMin.setText("");
            fb.editPriceMax.setText("");
        });
        fb.chipPrice5.setOnClickListener(v -> {
            fb.editPriceMin.setText("");
            fb.editPriceMax.setText("5");
        });
        fb.chipPrice10.setOnClickListener(v -> {
            fb.editPriceMin.setText("");
            fb.editPriceMax.setText("10");
        });
        fb.chipPrice15.setOnClickListener(v -> {
            fb.editPriceMin.setText("");
            fb.editPriceMax.setText("15");
        });
        fb.chipPrice25.setOnClickListener(v -> {
            fb.editPriceMin.setText("");
            fb.editPriceMax.setText("25");
        });

        fb.chipGroupHours.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean customSelected = checkedIds.contains(R.id.chip_hours_custom);
            fb.layoutCustomHours.setVisibility(customSelected ? View.VISIBLE : View.GONE);
        });

        fb.btnHourFrom.setOnClickListener(v -> new TimePickerDialog(
                this,
                (picker, hour, minute) -> {
                    customStartMin[0] = hour * 60 + minute;
                    fb.btnHourFrom.setText(getString(R.string.map_filter_hour_from) + ": "
                            + MapSpotFilterHelper.formatMinutes(customStartMin[0]));
                },
                customStartMin[0] / 60,
                customStartMin[0] % 60,
                true
        ).show());

        fb.btnHourTo.setOnClickListener(v -> new TimePickerDialog(
                this,
                (picker, hour, minute) -> {
                    customEndMin[0] = hour * 60 + minute;
                    fb.btnHourTo.setText(getString(R.string.map_filter_hour_to) + ": "
                            + MapSpotFilterHelper.formatMinutes(customEndMin[0]));
                },
                customEndMin[0] / 60,
                customEndMin[0] % 60,
                true
        ).show());

        fb.chipSizeAny.setOnClickListener(v -> {
            fb.editMinLengthCm.setText("");
            fb.editMinWidthCm.setText("");
            fb.editMinHeightCm.setText("");
        });
        fb.chipSizeCompact.setOnClickListener(v -> {
            fb.editMinLengthCm.setText("450");
            fb.editMinWidthCm.setText("220");
        });
        fb.chipSizeStandard.setOnClickListener(v -> {
            fb.editMinLengthCm.setText("500");
            fb.editMinWidthCm.setText("250");
        });
        fb.chipSizeLarge.setOnClickListener(v -> {
            fb.editMinLengthCm.setText("550");
            fb.editMinWidthCm.setText("260");
        });

        fb.btnCloseFilters.setOnClickListener(v -> dialog.dismiss());

        fb.btnResetFilters.setOnClickListener(v -> {
            filterCriteria.reset();
            syncUiFromCriteria.run();
        });

        fb.btnApplyFilters.setOnClickListener(v -> {
            int sortId = fb.chipGroupSort.getCheckedChipId();
            if (sortId == R.id.chip_sort_price) filterCriteria.setSortBy(MapSpotFilterHelper.SortMode.PRICE_ASC);
            else if (sortId == R.id.chip_sort_rating) filterCriteria.setSortBy(MapSpotFilterHelper.SortMode.RATING_DESC);
            else filterCriteria.setSortBy(MapSpotFilterHelper.SortMode.DISTANCE);

            int radId = fb.chipGroupRadius.getCheckedChipId();
            if (radId == R.id.chip_radius_1km) filterCriteria.setRadiusMeters(1000.0);
            else if (radId == R.id.chip_radius_3km) filterCriteria.setRadiusMeters(3000.0);
            else if (radId == R.id.chip_radius_10km) filterCriteria.setRadiusMeters(10000.0);
            else if (radId == R.id.chip_radius_25km) filterCriteria.setRadiusMeters(25000.0);
            else filterCriteria.setRadiusMeters(5000.0);

            filterCriteria.setMinPrice(parseOptionalDouble(fb.editPriceMin.getText().toString()));
            filterCriteria.setMaxPrice(parseOptionalDouble(fb.editPriceMax.getText().toString()));

            int hrsId = fb.chipGroupHours.getCheckedChipId();
            if (hrsId == R.id.chip_hours_now) filterCriteria.setHoursMode(MapSpotFilterHelper.HoursMode.OPEN_NOW);
            else if (hrsId == R.id.chip_hours_all_day) filterCriteria.setHoursMode(MapSpotFilterHelper.HoursMode.OPEN_24_7);
            else if (hrsId == R.id.chip_hours_custom) filterCriteria.setHoursMode(MapSpotFilterHelper.HoursMode.CUSTOM_WINDOW);
            else filterCriteria.setHoursMode(MapSpotFilterHelper.HoursMode.ANY);

            filterCriteria.setCustomDayOffset(fb.chipDayTomorrow.isChecked() ? 1 : 0);
            filterCriteria.setCustomStartMinutes(customStartMin[0]);
            filterCriteria.setCustomEndMinutes(customEndMin[0]);

            filterCriteria.setMinLengthCm(parseOptionalInt(fb.editMinLengthCm.getText().toString()));
            filterCriteria.setMinWidthCm(parseOptionalInt(fb.editMinWidthCm.getText().toString()));
            filterCriteria.setMinHeightCm(parseOptionalInt(fb.editMinHeightCm.getText().toString()));

            Set<String> amenities = new HashSet<>();
            if (fb.chipAmenityCctv.isChecked()) amenities.add("CCTV");
            if (fb.chipAmenityLighting.isChecked()) amenities.add("LIGHTING");
            if (fb.chipAmenityEv.isChecked()) amenities.add("EV_CHARGER");
            if (fb.chipAmenityCovered.isChecked()) amenities.add("COVERED");
            if (fb.chipAmenityUnderground.isChecked()) amenities.add("UNDERGROUND");
            if (fb.chipAmenityGate.isChecked()) amenities.add("GATE");
            if (fb.chipAmenityDisabled.isChecked()) amenities.add("DISABLED_ACCESS");
            if (fb.chipAmenityWide.isChecked()) amenities.add("WIDE_SPACE");
            filterCriteria.setRequiredAmenities(amenities);

            int rateId = fb.chipGroupRating.getCheckedChipId();
            if (rateId == R.id.chip_rating_45) filterCriteria.setMinRating(4.5);
            else if (rateId == R.id.chip_rating_40) filterCriteria.setMinRating(4.0);
            else if (rateId == R.id.chip_rating_35) filterCriteria.setMinRating(3.5);
            else filterCriteria.setMinRating(0.0);

            filterCriteria.setOnlyVerified(fb.switchOnlyVerified.isChecked());
            filterCriteria.setOnlyAutoAccept(fb.switchOnlyAutoAccept.isChecked());

            dialog.dismiss();
            updateFilterBadge();

            LatLng loadCenter = searchedAnchorPoint != null
                    ? searchedAnchorPoint
                    : (googleMap != null ? googleMap.getCameraPosition().target : activeQueryCenter);
            viewModel.loadNearbySpots(loadCenter.latitude, loadCenter.longitude, filterCriteria.getRadiusMeters());
            refreshMapDisplay();
            if (searchedAnchorPoint != null) {
                fitCameraToSearchResults(searchedAnchorPoint, latestFilteredSpots);
            }
        });

        dialog.show();
    }

    private void showNearbyListBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogNearbySpotsListBinding lb = DialogNearbySpotsListBinding.inflate(LayoutInflater.from(this));
        dialog.setContentView(lb.getRoot());
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        sheetListSubtitle = lb.textListSubtitle;
        sheetListEmptyView = lb.layoutNearbyListEmpty;
        sheetListRecycler = lb.recyclerNearbySpotsFull;

        String titleLabel = searchedAnchorLabel != null && !searchedAnchorLabel.trim().isEmpty()
                ? searchedAnchorLabel.trim()
                : (selectedCityFilter != null ? selectedCityFilter : getString(R.string.nearby_list_title));
        lb.textListAddressTitle.setText(titleLabel);

        sheetListAdapter = new NearbySpotsAdapter(false, spot -> {
            dialog.dismiss();
            focusSpotOnMap(spot);
            showSpotPreviewCard(spot);
        });
        lb.recyclerNearbySpotsFull.setLayoutManager(new LinearLayoutManager(this));
        lb.recyclerNearbySpotsFull.setAdapter(sheetListAdapter);

        LatLng ref = searchedAnchorPoint != null
                ? searchedAnchorPoint
                : (googleMap != null ? googleMap.getCameraPosition().target : activeQueryCenter);
        sheetListAdapter.submitData(latestFilteredSpots, ref, spotRulesMap);
        updateOpenListSheetHeader();

        lb.btnCloseNearbyList.setOnClickListener(v -> dialog.dismiss());
        lb.btnListOpenFilters.setOnClickListener(v -> showMapFiltersBottomSheet());
        lb.btnListExpandRadius.setOnClickListener(v -> {
            double cur = filterCriteria.getRadiusMeters();
            double next = cur < 3000.0 ? 3000.0 : (cur < 5000.0 ? 5000.0 : (cur < 10000.0 ? 10000.0 : 25000.0));
            filterCriteria.setRadiusMeters(next);
            updateFilterBadge();
            LatLng target = searchedAnchorPoint != null
                    ? searchedAnchorPoint
                    : (googleMap != null ? googleMap.getCameraPosition().target : activeQueryCenter);
            viewModel.loadNearbySpots(target.latitude, target.longitude, next);
            refreshMapDisplay();
            if (searchedAnchorPoint != null) {
                fitCameraToSearchResults(searchedAnchorPoint, latestFilteredSpots);
            }
        });

        dialog.setOnDismissListener(d -> {
            sheetListAdapter = null;
            sheetListSubtitle = null;
            sheetListEmptyView = null;
            sheetListRecycler = null;
        });

        dialog.show();
    }

    private void updateOpenListSheetHeader() {
        if (sheetListSubtitle != null) {
            String radiusStr = MapSpotFilterHelper.formatDistance(filterCriteria.getRadiusMeters());
            String anchor = searchedAnchorLabel != null && !searchedAnchorLabel.trim().isEmpty()
                    ? searchedAnchorLabel.trim()
                    : (selectedCityFilter != null ? selectedCityFilter : getString(R.string.nearby_list_title));
            sheetListSubtitle.setText(latestFilteredSpots.isEmpty()
                    ? getString(R.string.nearby_banner_empty, anchor, radiusStr)
                    : getString(R.string.nearby_banner_format, anchor, latestFilteredSpots.size(), radiusStr));
        }
        if (sheetListEmptyView != null && sheetListRecycler != null) {
            boolean empty = latestFilteredSpots.isEmpty();
            sheetListEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            sheetListRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    @Nullable
    private Double parseOptionalDouble(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            double val = Double.parseDouble(raw.trim().replace(',', '.'));
            return val >= 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseOptionalInt(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @NonNull
    private String formatCompactNumber(double val) {
        if (Math.abs(val - Math.round(val)) < 0.01) {
            return String.valueOf(Math.round(val));
        }
        return String.format(Locale.US, "%.2f", val);
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
        enableMyLocationLayerIfPermitted();

        LatLng defaultLocation = new LatLng(52.2297, 21.0122);
        activeQueryCenter = defaultLocation;
        lastIdleLoadedCenter = defaultLocation;
        suppressNextCameraIdle = true;
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        // Initial spot loading from backend into Room for default location
        viewModel.loadNearbySpots(defaultLocation.latitude, defaultLocation.longitude, filterCriteria.getRadiusMeters());

        // Dynamically load spots around camera center only after significant map pan
        googleMap.setOnCameraIdleListener(() -> {
            if (googleMap == null) return;
            if (suppressNextCameraIdle) {
                suppressNextCameraIdle = false;
                return;
            }
            LatLng center = googleMap.getCameraPosition().target;
            double threshold = Math.max(800.0, filterCriteria.getRadiusMeters() * 0.35);
            if (lastIdleLoadedCenter == null
                    || MapSpotFilterHelper.distanceMeters(
                    lastIdleLoadedCenter.latitude, lastIdleLoadedCenter.longitude,
                    center.latitude, center.longitude) > threshold) {
                lastIdleLoadedCenter = center;
                viewModel.loadNearbySpots(center.latitude, center.longitude, filterCriteria.getRadiusMeters());
            }
            if (searchedAnchorPoint == null) {
                refreshMapDisplay();
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
            suggestionsDismissedManually = true;
            hideAddressSuggestions(false);
            if (binding != null) {
                binding.searchView.clearFocus();
                if (binding.cardPlacePreview != null) {
                    binding.cardPlacePreview.setVisibility(View.GONE);
                }
            }
            selectedSpotForBooking = null;
            updateBottomOverlaysVisibility();
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

        // Verified badge visibility from SpotMetadata cache when available
        SpotRepository.SpotMetadata meta = spot.getId() != null
                ? viewModel.getSpotMetadataMap().get(spot.getId()) : null;
        binding.containerVerifiedBadge.setVisibility(
                (meta == null || meta.isVerified()) ? View.VISIBLE : View.GONE);

        // Working hours summary & distance badge
        List<AvailabilityRuleEntity> spotRules = spot.getId() != null ? spotRulesMap.get(spot.getId()) : null;
        binding.textPreviewHours.setText(MapSpotFilterHelper.formatWorkingHoursSummary(
                spotRules, System.currentTimeMillis()));

        LatLng refPoint = searchedAnchorPoint != null
                ? searchedAnchorPoint
                : (googleMap != null && googleMap.getCameraPosition() != null
                ? googleMap.getCameraPosition().target : null);
        if (refPoint != null && (refPoint.latitude != 0.0 || refPoint.longitude != 0.0)) {
            double dist = MapSpotFilterHelper.distanceMeters(
                    refPoint.latitude, refPoint.longitude, spot.getLatitude(), spot.getLongitude());
            binding.textPreviewDistance.setText(MapSpotFilterHelper.formatDistance(dist));
            binding.textPreviewDistance.setVisibility(View.VISIBLE);
        } else {
            binding.textPreviewDistance.setVisibility(View.GONE);
        }

        // Dimensions badge (if present)
        String dimensions = spot.getFormattedDimensions();
        if (dimensions != null && !dimensions.isEmpty()) {
            binding.textPreviewDimensions.setText(String.format("📐 %s", dimensions));
            binding.textPreviewDimensions.setVisibility(View.VISIBLE);
        } else {
            binding.textPreviewDimensions.setVisibility(View.GONE);
        }

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
        updateBottomOverlaysVisibility();
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
        suggestionHandler.removeCallbacksAndMessages(null);
        googleMap = null;
        binding = null;
    }
}
