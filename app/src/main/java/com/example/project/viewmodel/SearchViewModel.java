package com.example.project.viewmodel;

import android.app.Application;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.R;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.network.Resource;
import com.example.project.repository.SpotRepository;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for map search screen backed by {@link SpotRepository} (Room Database SSOT)
 * and asynchronous Geocoder search operations.
 */
public class SearchViewModel extends AndroidViewModel {

    /**
     * Wrapper class for geocoding search result state.
     */
    public static class SearchResult {
        private final LatLng latLng;
        private final String title;
        private final String errorMessage;

        public SearchResult(LatLng latLng, String title) {
            this.latLng = latLng;
            this.title = title;
            this.errorMessage = null;
        }

        public SearchResult(String errorMessage) {
            this.latLng = null;
            this.title = null;
            this.errorMessage = errorMessage;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public String getTitle() {
            return title;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return latLng != null && errorMessage == null;
        }
    }

    private final SpotRepository spotRepository;
    private final MutableLiveData<SearchResult> searchResultLiveData = new MutableLiveData<>();
    private final LiveData<List<ParkingPlaceEntity>> spotsLiveData;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Default constructor initializing dependencies using Android Application context.
     *
     * @param application Application instance
     */
    public SearchViewModel(@NonNull Application application) {
        this(application, SpotRepository.getInstance(application));
    }

    /**
     * Parameterized constructor allowing repository injection for testing.
     *
     * @param application    Application instance
     * @param spotRepository SpotRepository instance
     */
    public SearchViewModel(@NonNull Application application, @NonNull SpotRepository spotRepository) {
        super(application);
        this.spotRepository = spotRepository;
        // Single Source of Truth: spots are observed directly from local Room Database
        this.spotsLiveData = this.spotRepository.getSpotsFromRoom();
    }

    /**
     * Gets the geocoding search result LiveData.
     *
     * @return SearchResult LiveData
     */
    public LiveData<SearchResult> getSearchResultLiveData() {
        return searchResultLiveData;
    }

    /**
     * Gets the reactive LiveData stream of parking spots directly from Room Database.
     *
     * @return LiveData containing cached list of ParkingPlaceEntity items
     */
    public LiveData<List<ParkingPlaceEntity>> getSpotsLiveData() {
        return spotsLiveData;
    }

    /**
     * Gets the network sync operation status from repository.
     *
     * @return LiveData containing sync status Resource
     */
    public LiveData<Resource<Void>> getSyncStatusLiveData() {
        return spotRepository.getSyncStatusLiveData();
    }

    /**
     * Triggers background download of nearby parking spots from REST API and updates Room cache.
     *
     * @param lat          Latitude coordinate
     * @param lng          Longitude coordinate
     * @param radiusMeters Search radius in meters (defaults to 5000.0 if null)
     */
    public void loadNearbySpots(double lat, double lng, @Nullable Double radiusMeters) {
        double radius = (radiusMeters != null && radiusMeters > 0) ? radiusMeters : 5000.0;
        spotRepository.fetchAndSaveSpots(lat, lng, radius);
    }

    /**
     * Searches for a location asynchronously on a background thread.
     *
     * @param context Application context
     * @param query   Location name query string
     */
    public void searchLocation(@NonNull Context context, String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResultLiveData.setValue(new SearchResult(context.getString(R.string.error_empty_query)));
            return;
        }

        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    String title = address.getFeatureName() != null ? address.getFeatureName() : query;

                    mainHandler.post(() -> {
                        searchResultLiveData.setValue(new SearchResult(latLng, title));
                        loadNearbySpots(latLng.latitude, latLng.longitude, 5000.0);
                    });
                } else {
                    mainHandler.post(() -> searchResultLiveData.setValue(
                            new SearchResult(context.getString(R.string.error_location_not_found))));
                }
            } catch (IOException e) {
                String errorMsg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Unknown error";
                mainHandler.post(() -> searchResultLiveData.setValue(
                        new SearchResult(context.getString(R.string.error_geocoding_failed, errorMsg))));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
