package com.example.project.viewmodel;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.project.R;
import com.example.project.network.Resource;
import com.example.project.network.model.ParkingSpotResponse;
import com.example.project.repository.ParkingSpotRepository;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for map search screen managing async geocoding requests and
 * reactive parking spot discovery via {@link ParkingSpotRepository}.
 */
public class SearchViewModel extends ViewModel {

    /**
     * Internal parameter class representing search query coordinates.
     */
    private static class SearchQuery {
        final double lat;
        final double lng;
        final double radiusMeters;

        SearchQuery(double lat, double lng, double radiusMeters) {
            this.lat = lat;
            this.lng = lng;
            this.radiusMeters = radiusMeters;
        }
    }

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

    private final ParkingSpotRepository parkingSpotRepository;
    private final MutableLiveData<SearchResult> searchResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<SearchQuery> searchQueryLiveData = new MutableLiveData<>();
    private final LiveData<Resource<List<ParkingSpotResponse>>> spotsLiveData;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Default constructor initializing default ParkingSpotRepository singleton.
     */
    public SearchViewModel() {
        this(ParkingSpotRepository.getInstance());
    }

    /**
     * Constructor allowing custom repository injection for testability.
     *
     * @param parkingSpotRepository Parking spot repository instance
     */
    public SearchViewModel(@NonNull ParkingSpotRepository parkingSpotRepository) {
        this.parkingSpotRepository = parkingSpotRepository;

        // Reactive pipeline: whenever searchQueryLiveData changes, execute network search
        this.spotsLiveData = Transformations.switchMap(searchQueryLiveData, query -> {
            if (query == null) {
                return new MutableLiveData<>(Resource.success(Collections.emptyList()));
            }
            return this.parkingSpotRepository.searchSpots(
                    query.lat,
                    query.lng,
                    query.radiusMeters,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        });
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
     * Gets the reactive LiveData stream of parking spots fetched from backend service.
     *
     * @return LiveData containing Resource with list of ParkingSpotResponse
     */
    public LiveData<Resource<List<ParkingSpotResponse>>> getSpotsLiveData() {
        return spotsLiveData;
    }

    /**
     * Triggers loading nearby parking spots from the backend API for the given coordinates.
     *
     * @param lat          Latitude coordinate
     * @param lng          Longitude coordinate
     * @param radiusMeters Search radius in meters (defaults to 5000.0 if null)
     */
    public void loadNearbySpots(double lat, double lng, Double radiusMeters) {
        double radius = (radiusMeters != null && radiusMeters > 0) ? radiusMeters : 5000.0;
        searchQueryLiveData.setValue(new SearchQuery(lat, lng, radius));
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
                        // Automatically query parking spots around found location
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
