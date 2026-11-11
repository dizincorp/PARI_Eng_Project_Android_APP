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
import com.example.project.database.AppDatabase;
import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.network.Resource;
import com.example.project.repository.SpotRepository;
import com.example.project.utils.MapSpotFilterHelper;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ViewModel for map search screen backed by {@link SpotRepository} (Room Database SSOT),
 * calendar blocked periods observation, and asynchronous Geocoder search operations.
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

    /**
     * Normalized address suggestion entry used for multi-city autocomplete and disambiguation,
     * prioritized by proximity to the user's current geolocation.
     */
    public static class AddressSuggestion {
        @NonNull
        private final String primaryText;
        @NonNull
        private final String city;
        @NonNull
        private final String secondaryText;
        @NonNull
        private final String fullLabel;
        @NonNull
        private final LatLng latLng;
        private final double distanceMeters;
        private final boolean nearUserLocation;

        public AddressSuggestion(@NonNull String primaryText,
                                 @NonNull String city,
                                 @NonNull String secondaryText,
                                 @NonNull String fullLabel,
                                 @NonNull LatLng latLng,
                                 double distanceMeters,
                                 boolean nearUserLocation) {
            this.primaryText = primaryText;
            this.city = city;
            this.secondaryText = secondaryText;
            this.fullLabel = fullLabel;
            this.latLng = latLng;
            this.distanceMeters = distanceMeters;
            this.nearUserLocation = nearUserLocation;
        }

        @NonNull
        public String getPrimaryText() {
            return primaryText;
        }

        @NonNull
        public String getCity() {
            return city;
        }

        @NonNull
        public String getSecondaryText() {
            return secondaryText;
        }

        @NonNull
        public String getFullLabel() {
            return fullLabel;
        }

        @NonNull
        public LatLng getLatLng() {
            return latLng;
        }

        public double getDistanceMeters() {
            return distanceMeters;
        }

        public boolean isNearUserLocation() {
            return nearUserLocation;
        }
    }

    private static class CachedSuggestions {
        final long timestampMs;
        @NonNull
        final List<AddressSuggestion> suggestions;

        CachedSuggestions(long timestampMs, @NonNull List<AddressSuggestion> suggestions) {
            this.timestampMs = timestampMs;
            this.suggestions = suggestions;
        }
    }

    private static final long SUGGESTION_CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final double NEAR_USER_RADIUS_METERS = 35_000.0;

    private final SpotRepository spotRepository;
    private final MutableLiveData<SearchResult> searchResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSearchingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<List<AddressSuggestion>> suggestionsLiveData =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> isSuggestionsLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> suggestionNoticeLiveData = new MutableLiveData<>(null);
    private final LiveData<List<ParkingPlaceEntity>> spotsLiveData;
    private final LiveData<List<BlockedPeriodEntity>> blockedPeriodsLiveData;
    private final LiveData<List<AvailabilityRuleEntity>> availabilityRulesLiveData;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ExecutorService suggestionExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger searchSequence = new AtomicInteger(0);
    private final AtomicInteger suggestionSequence = new AtomicInteger(0);

    private final Map<String, CachedSuggestions> suggestionCache =
            Collections.synchronizedMap(new LinkedHashMap<String, CachedSuggestions>(24, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedSuggestions> eldest) {
                    return size() > 24;
                }
            });

    @Nullable
    private volatile LatLng cachedCityCoords;
    @Nullable
    private volatile String cachedUserCity;

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
        AppDatabase db = AppDatabase.getInstance(application);
        this.blockedPeriodsLiveData = db.blockedPeriodDao().getAllBlockedPeriodsLiveData();
        this.availabilityRulesLiveData = db.availabilityRuleDao().getAllRulesLiveData();
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
     * Gets reactive LiveData stream of all blocked periods from local Room DB cache.
     *
     * @return LiveData emitting list of BlockedPeriodEntity
     */
    public LiveData<List<BlockedPeriodEntity>> getBlockedPeriodsLiveData() {
        return blockedPeriodsLiveData;
    }

    /**
     * Gets reactive LiveData stream of all spot availability rules from local Room DB cache.
     */
    public LiveData<List<AvailabilityRuleEntity>> getAvailabilityRulesLiveData() {
        return availabilityRulesLiveData;
    }

    /**
     * Exposes search-in-progress state so UI can provide subtle feedback.
     */
    public LiveData<Boolean> getIsSearchingLiveData() {
        return isSearchingLiveData;
    }

    public LiveData<List<AddressSuggestion>> getSuggestionsLiveData() {
        return suggestionsLiveData;
    }

    public LiveData<Boolean> getIsSuggestionsLoadingLiveData() {
        return isSuggestionsLoadingLiveData;
    }

    public LiveData<String> getSuggestionNoticeLiveData() {
        return suggestionNoticeLiveData;
    }

    public void clearSuggestions() {
        suggestionSequence.incrementAndGet();
        isSuggestionsLoadingLiveData.setValue(false);
        suggestionsLiveData.setValue(Collections.emptyList());
    }

    public void consumeSuggestionNotice() {
        suggestionNoticeLiveData.setValue(null);
    }

    /**
     * Returns in-memory metadata map (verified, autoAccept, realtimeStatusEnabled) by spot ID.
     */
    @NonNull
    public Map<String, SpotRepository.SpotMetadata> getSpotMetadataMap() {
        Map<String, SpotRepository.SpotMetadata> map = spotRepository.getSpotMetadataMap();
        return map != null ? map : Collections.emptyMap();
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
     * Searches for a location asynchronously on a background thread using a default 5 km radius.
     */
    public void searchLocation(@NonNull Context context, String query) {
        searchLocation(context, query, 5000.0);
    }

    /**
     * Multi-tier asynchronous address lookup delegating to {@link #submitAddressSearch}.
     */
    public void searchLocation(@NonNull Context context, String query, double radiusMeters) {
        submitAddressSearch(context, query, radiusMeters, null, null);
    }

    /**
     * Fetches multi-city autocomplete suggestions asynchronously, ordered with results
     * near {@code userLocation} first followed by all matching cities across Poland.
     */
    public void fetchAddressSuggestions(@NonNull Context context,
                                        @Nullable String query,
                                        @Nullable LatLng userLocation,
                                        @Nullable LatLng mapCenter) {
        String trimmedQuery = query != null ? query.trim() : "";
        if (trimmedQuery.length() < 3) {
            clearSuggestions();
            return;
        }

        final int requestSeq = suggestionSequence.incrementAndGet();

        SearchResult directCoords = tryParseDirectCoordinates(trimmedQuery);
        if (directCoords != null && directCoords.isSuccess()) {
            double dist = userLocation != null
                    ? MapSpotFilterHelper.distanceMeters(
                    userLocation.latitude, userLocation.longitude,
                    directCoords.getLatLng().latitude, directCoords.getLatLng().longitude)
                    : Double.NaN;
            List<AddressSuggestion> single = Collections.singletonList(new AddressSuggestion(
                    directCoords.getTitle(),
                    "",
                    trimmedQuery,
                    directCoords.getTitle(),
                    directCoords.getLatLng(),
                    dist,
                    !Double.isNaN(dist) && dist <= NEAR_USER_RADIUS_METERS));
            isSuggestionsLoadingLiveData.setValue(false);
            suggestionsLiveData.setValue(single);
            return;
        }

        String cacheKey = buildCacheKey(trimmedQuery, userLocation);
        CachedSuggestions cached = suggestionCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestampMs) <= SUGGESTION_CACHE_TTL_MS) {
            isSuggestionsLoadingLiveData.setValue(false);
            suggestionsLiveData.setValue(cached.suggestions);
            return;
        }

        isSuggestionsLoadingLiveData.setValue(true);
        final Context appContext = context.getApplicationContext();

        suggestionExecutor.execute(() -> {
            List<AddressSuggestion> candidates =
                    collectCandidatesSync(appContext, trimmedQuery, userLocation, mapCenter, null);
            suggestionCache.put(cacheKey, new CachedSuggestions(System.currentTimeMillis(), candidates));

            if (requestSeq != suggestionSequence.get()) {
                return;
            }
            mainHandler.post(() -> {
                if (requestSeq != suggestionSequence.get()) {
                    return;
                }
                isSuggestionsLoadingLiveData.setValue(false);
                suggestionsLiveData.setValue(candidates);
            });
        });
    }

    /**
     * Resolves a submitted search query: jumps immediately only when unambiguous (single city,
     * explicit city in query, or direct coordinates), or surfaces prioritized city suggestions
     * instead of jumping blindly when a street exists in multiple cities.
     */
    public void submitAddressSearch(@NonNull Context context,
                                    @Nullable String query,
                                    double radiusMeters,
                                    @Nullable LatLng userLocation,
                                    @Nullable LatLng mapCenter) {
        if (query == null || query.trim().isEmpty()) {
            searchResultLiveData.setValue(new SearchResult(context.getString(R.string.error_empty_query)));
            return;
        }

        final String trimmedQuery = query.trim();
        final double effectiveRadius = radiusMeters > 0 ? radiusMeters : 5000.0;
        final int requestSeq = searchSequence.incrementAndGet();
        suggestionSequence.incrementAndGet();
        isSuggestionsLoadingLiveData.setValue(false);
        isSearchingLiveData.setValue(true);

        final Context appContext = context.getApplicationContext();

        executorService.execute(() -> {
            SearchResult direct = tryParseDirectCoordinates(trimmedQuery);
            if (direct != null && direct.isSuccess()) {
                postDirectSearchSuccess(requestSeq, direct, effectiveRadius);
                return;
            }

            IOException[] geocoderError = new IOException[1];
            String cacheKey = buildCacheKey(trimmedQuery, userLocation);
            List<AddressSuggestion> candidates;
            CachedSuggestions cached = suggestionCache.get(cacheKey);
            if (cached != null
                    && !cached.suggestions.isEmpty()
                    && (System.currentTimeMillis() - cached.timestampMs) <= SUGGESTION_CACHE_TTL_MS) {
                candidates = cached.suggestions;
            } else {
                candidates = collectCandidatesSync(appContext, trimmedQuery, userLocation, mapCenter, geocoderError);
                if (!candidates.isEmpty()) {
                    suggestionCache.put(cacheKey, new CachedSuggestions(System.currentTimeMillis(), candidates));
                }
            }

            if (requestSeq != searchSequence.get()) {
                return;
            }

            mainHandler.post(() -> {
                if (requestSeq != searchSequence.get()) {
                    return;
                }
                isSearchingLiveData.setValue(false);

                if (candidates.isEmpty()) {
                    suggestionsLiveData.setValue(Collections.emptyList());
                    if (geocoderError[0] != null && geocoderError[0].getLocalizedMessage() != null) {
                        searchResultLiveData.setValue(new SearchResult(
                                appContext.getString(R.string.error_geocoding_failed,
                                        geocoderError[0].getLocalizedMessage())));
                    } else {
                        searchResultLiveData.setValue(new SearchResult(
                                appContext.getString(R.string.error_location_not_found)));
                    }
                    return;
                }

                AddressSuggestion explicitCityMatch = findExplicitCityMatch(trimmedQuery, candidates);
                Set<String> distinctCities = collectDistinctCities(candidates);
                boolean ambiguousMultiCity = distinctCities.size() >= 2 && explicitCityMatch == null;

                if (ambiguousMultiCity) {
                    suggestionsLiveData.setValue(candidates);
                    suggestionNoticeLiveData.setValue(
                            appContext.getString(R.string.search_ambiguous_city_hint));
                } else {
                    AddressSuggestion chosen = explicitCityMatch != null ? explicitCityMatch : candidates.get(0);
                    suggestionsLiveData.setValue(Collections.emptyList());
                    searchResultLiveData.setValue(new SearchResult(chosen.getLatLng(), chosen.getFullLabel()));
                    loadNearbySpots(chosen.getLatLng().latitude, chosen.getLatLng().longitude, effectiveRadius);
                }
            });
        });
    }

    /**
     * Immediately pins and searches parkings around an explicitly selected suggestion.
     */
    public void selectSuggestion(@NonNull AddressSuggestion suggestion, double radiusMeters) {
        searchSequence.incrementAndGet();
        suggestionSequence.incrementAndGet();
        isSearchingLiveData.setValue(false);
        isSuggestionsLoadingLiveData.setValue(false);
        suggestionsLiveData.setValue(Collections.emptyList());

        double effectiveRadius = radiusMeters > 0 ? radiusMeters : 5000.0;
        searchResultLiveData.setValue(new SearchResult(suggestion.getLatLng(), suggestion.getFullLabel()));
        loadNearbySpots(suggestion.getLatLng().latitude, suggestion.getLatLng().longitude, effectiveRadius);
    }

    private void postDirectSearchSuccess(int requestSeq, @NonNull SearchResult result, double effectiveRadius) {
        if (requestSeq != searchSequence.get()) {
            return;
        }
        mainHandler.post(() -> {
            if (requestSeq != searchSequence.get()) {
                return;
            }
            isSearchingLiveData.setValue(false);
            suggestionsLiveData.setValue(Collections.emptyList());
            searchResultLiveData.setValue(result);
            loadNearbySpots(result.getLatLng().latitude, result.getLatLng().longitude, effectiveRadius);
        });
    }

    @NonNull
    private List<AddressSuggestion> collectCandidatesSync(@NonNull Context context,
                                                          @NonNull String query,
                                                          @Nullable LatLng userLocation,
                                                          @Nullable LatLng mapCenter,
                                                          @Nullable IOException[] geocoderErrorOut) {
        List<RawCandidate> rawCandidates = new ArrayList<>();
        String userCity = userLocation != null ? resolveUserCitySync(context, userLocation) : "";
        String foldedQuery = MapSpotFilterHelper.foldPolish(query);

        // Tier A (Geolocation-first): city-qualified query + bounding-box Geocoder
        if (userLocation != null) {
            if (!userCity.isEmpty() && !containsFoldedWord(foldedQuery, MapSpotFilterHelper.foldPolish(userCity))) {
                queryGeocoderCandidates(context, query + ", " + userCity, null, rawCandidates, geocoderErrorOut);
            }
            queryGeocoderCandidates(context, query, userLocation, rawCandidates, geocoderErrorOut);
        }

        // Tier B1: Nationwide Android Geocoder (multi-city, up to 10 results)
        int beforeNationwideGeo = rawCandidates.size();
        queryGeocoderCandidates(context, query, null, rawCandidates, geocoderErrorOut);
        if (rawCandidates.size() - beforeNationwideGeo < 3
                && !foldedQuery.contains("polsk") && !foldedQuery.contains("poland")) {
            queryGeocoderCandidates(context, query + ", Polska", null, rawCandidates, geocoderErrorOut);
        }

        // Tier B2: Nationwide OpenStreetMap Nominatim (up to 15 cities/variants)
        int beforeNationwideOsm = rawCandidates.size();
        queryNominatimCandidates(query, true, null, 15, rawCandidates);
        if (rawCandidates.size() == beforeNationwideOsm) {
            queryNominatimCandidates(query, false, null, 10, rawCandidates);
        }

        // Tier A fallback over HTTP: only hit bounded / city-qualified Nominatim if user location has no local hit yet
        if (userLocation != null && !hasCandidateNearLocation(rawCandidates, userLocation, userCity)) {
            queryNominatimCandidates(query, true, userLocation, 6, rawCandidates);
            if (!userCity.isEmpty()
                    && !containsFoldedWord(foldedQuery, MapSpotFilterHelper.foldPolish(userCity))
                    && !hasCandidateNearLocation(rawCandidates, userLocation, userCity)) {
                queryNominatimCandidates(query + ", " + userCity, true, null, 5, rawCandidates);
            }
        }

        // Tier C: Local Room cached parking spots (street / city / title fallback)
        queryLocalRoomCandidates(query, rawCandidates);

        return deduplicateAndRankSuggestions(rawCandidates, query, userLocation, userCity, mapCenter);
    }

    private static class RawCandidate {
        @NonNull final String primaryText;
        @NonNull final String city;
        @NonNull final String secondaryText;
        @NonNull final LatLng latLng;

        RawCandidate(@NonNull String primaryText,
                     @NonNull String city,
                     @NonNull String secondaryText,
                     @NonNull LatLng latLng) {
            this.primaryText = primaryText;
            this.city = city;
            this.secondaryText = secondaryText;
            this.latLng = latLng;
        }
    }

    @NonNull
    private String resolveUserCitySync(@NonNull Context context, @NonNull LatLng userLocation) {
        LatLng prevCoords = cachedCityCoords;
        String prevCity = cachedUserCity;
        if (prevCoords != null && prevCity != null && !prevCity.isEmpty()) {
            double moved = MapSpotFilterHelper.distanceMeters(
                    prevCoords.latitude, prevCoords.longitude,
                    userLocation.latitude, userLocation.longitude);
            if (moved < 2500.0) {
                return prevCity;
            }
        }

        String resolvedCity = "";
        if (Geocoder.isPresent()) {
            try {
                Geocoder geocoder = new Geocoder(context, new Locale("pl", "PL"));
                List<Address> list = geocoder.getFromLocation(userLocation.latitude, userLocation.longitude, 1);
                if (list != null && !list.isEmpty()) {
                    resolvedCity = extractCityFromGeocoderAddress(list.get(0));
                }
            } catch (Exception ignored) {
            }
        }

        if (resolvedCity.isEmpty()) {
            resolvedCity = reverseCityViaNominatim(userLocation);
        }

        if (!resolvedCity.isEmpty()) {
            cachedCityCoords = userLocation;
            cachedUserCity = resolvedCity;
        }
        return resolvedCity;
    }

    @NonNull
    private String reverseCityViaNominatim(@NonNull LatLng coords) {
        HttpURLConnection conn = null;
        try {
            String urlStr = String.format(Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.6f&lon=%.6f&zoom=10&addressdetails=1",
                    coords.latitude, coords.longitude);
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);
            conn.setRequestProperty("User-Agent", "ParkShareAndroid/1.0");
            conn.setRequestProperty("Accept-Language", "pl,en");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "";
            }
            String body = readStream(conn);
            JSONObject obj = new JSONObject(body);
            JSONObject addr = obj.optJSONObject("address");
            return addr != null ? cleanCityName(optFirstNonEmpty(addr, "city", "town", "village", "municipality")) : "";
        } catch (Exception ignored) {
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void queryGeocoderCandidates(@NonNull Context context,
                                         @NonNull String query,
                                         @Nullable LatLng boxCenter,
                                         @NonNull List<RawCandidate> out,
                                         @Nullable IOException[] errorOut) {
        if (!Geocoder.isPresent()) {
            return;
        }
        try {
            Geocoder geocoder = new Geocoder(context, new Locale("pl", "PL"));
            List<Address> addresses;
            if (boxCenter != null) {
                double latDelta = 0.28;
                double lonDelta = 0.42;
                addresses = geocoder.getFromLocationName(
                        query,
                        6,
                        Math.max(-90.0, boxCenter.latitude - latDelta),
                        Math.max(-180.0, boxCenter.longitude - lonDelta),
                        Math.min(90.0, boxCenter.latitude + latDelta),
                        Math.min(180.0, boxCenter.longitude + lonDelta));
            } else {
                addresses = geocoder.getFromLocationName(query, 10);
            }
            if (addresses == null) {
                return;
            }
            for (Address addr : addresses) {
                if (addr == null) continue;
                RawCandidate candidate = buildCandidateFromGeocoder(addr, query);
                if (candidate != null) {
                    out.add(candidate);
                }
            }
        } catch (IOException e) {
            if (errorOut != null && errorOut[0] == null) {
                errorOut[0] = e;
            }
        } catch (Exception ignored) {
        }
    }

    private void queryNominatimCandidates(@NonNull String query,
                                          boolean countryPl,
                                          @Nullable LatLng boxCenter,
                                          int limit,
                                          @NonNull List<RawCandidate> out) {
        HttpURLConnection conn = null;
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            StringBuilder urlBuilder = new StringBuilder(
                    "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=")
                    .append(limit);
            if (countryPl) {
                urlBuilder.append("&countrycodes=pl");
            }
            if (boxCenter != null) {
                double latDelta = 0.30;
                double lonDelta = 0.45;
                urlBuilder.append(String.format(Locale.US,
                        "&viewbox=%.5f,%.5f,%.5f,%.5f&bounded=1",
                        boxCenter.longitude - lonDelta,
                        boxCenter.latitude + latDelta,
                        boxCenter.longitude + lonDelta,
                        boxCenter.latitude - latDelta));
            }
            urlBuilder.append("&q=").append(encoded);

            conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            conn.setRequestProperty("User-Agent", "ParkShareAndroid/1.0");
            conn.setRequestProperty("Accept-Language", "pl,en");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            JSONArray array = new JSONArray(readStream(conn));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) continue;
                RawCandidate candidate = buildCandidateFromNominatim(obj, query);
                if (candidate != null) {
                    out.add(candidate);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void queryLocalRoomCandidates(@NonNull String query, @NonNull List<RawCandidate> out) {
        List<ParkingPlaceEntity> localSpots = AppDatabase.getInstance(getApplication())
                .parkingPlaceDao().getAllPlacesSync();
        if (localSpots == null || localSpots.isEmpty()) {
            return;
        }
        String foldedQuery = MapSpotFilterHelper.foldPolish(query);
        if (foldedQuery.isEmpty()) {
            return;
        }
        String[] tokens = foldedQuery.split("\\s+");

        for (ParkingPlaceEntity spot : localSpots) {
            if (spot == null) continue;
            if (spot.getLatitude() == 0.0 && spot.getLongitude() == 0.0) continue;

            String haystack = MapSpotFilterHelper.foldPolish(
                    safeStr(spot.getTitle()) + " "
                            + safeStr(spot.getStreet()) + " "
                            + safeStr(spot.getCity()) + " "
                            + safeStr(spot.getPostalCode()));

            int matchedTokens = 0;
            for (String token : tokens) {
                if (!token.isEmpty() && haystack.contains(token)) {
                    matchedTokens++;
                }
            }
            if (matchedTokens == 0 || matchedTokens < Math.min(tokens.length, 2) && !haystack.contains(foldedQuery)) {
                if (tokens.length == 1 && matchedTokens == 1) {
                    // allow single-token exact match
                } else {
                    continue;
                }
            }

            String primary = !safeStr(spot.getStreet()).isEmpty()
                    ? safeStr(spot.getStreet())
                    : safeStr(spot.getTitle());
            String city = safeStr(spot.getCity());
            StringBuilder meta = new StringBuilder();
            if (!safeStr(spot.getTitle()).isEmpty() && !safeStr(spot.getTitle()).equalsIgnoreCase(primary)) {
                meta.append(safeStr(spot.getTitle()));
            }
            if (!safeStr(spot.getPostalCode()).isEmpty()) {
                if (meta.length() > 0) meta.append(", ");
                meta.append(safeStr(spot.getPostalCode()));
            }
            if (meta.length() == 0 && !city.isEmpty()) {
                meta.append(city);
            }
            out.add(new RawCandidate(
                    !primary.isEmpty() ? primary : query,
                    city,
                    meta.toString(),
                    new LatLng(spot.getLatitude(), spot.getLongitude())));
        }
    }

    @Nullable
    private RawCandidate buildCandidateFromGeocoder(@NonNull Address addr, @NonNull String query) {
        double lat = addr.getLatitude();
        double lng = addr.getLongitude();
        if (lat == 0.0 && lng == 0.0) {
            return null;
        }

        String thoroughfare = safeStr(addr.getThoroughfare());
        String subThoroughfare = safeStr(addr.getSubThoroughfare());
        String feature = safeStr(addr.getFeatureName());
        String city = extractCityFromGeocoderAddress(addr);

        String primary;
        if (!thoroughfare.isEmpty()) {
            primary = !subThoroughfare.isEmpty() ? thoroughfare + " " + subThoroughfare : thoroughfare;
        } else if (!feature.isEmpty()) {
            primary = feature;
        } else {
            String line = safeStr(addr.getAddressLine(0));
            if (!line.isEmpty()) {
                int comma = line.indexOf(',');
                primary = comma > 0 ? line.substring(0, comma).trim() : line;
            } else {
                primary = !city.isEmpty() ? city : query;
            }
        }

        List<String> metaParts = new ArrayList<>();
        String subLocality = safeStr(addr.getSubLocality());
        if (!subLocality.isEmpty() && !sameFolded(subLocality, city) && !sameFolded(subLocality, primary)) {
            metaParts.add(subLocality);
        }
        String postal = safeStr(addr.getPostalCode());
        if (!postal.isEmpty()) {
            metaParts.add(postal);
        }
        String admin = shortenVoivodeship(safeStr(addr.getAdminArea()));
        if (!admin.isEmpty() && !sameFolded(admin, city)) {
            metaParts.add(admin);
        }
        if (metaParts.isEmpty() && !city.isEmpty() && !sameFolded(city, primary)) {
            metaParts.add(city);
        }

        return new RawCandidate(primary, city, joinComma(metaParts), new LatLng(lat, lng));
    }

    @Nullable
    private RawCandidate buildCandidateFromNominatim(@NonNull JSONObject obj, @NonNull String query) {
        try {
            double lat = Double.parseDouble(obj.getString("lat"));
            double lon = Double.parseDouble(obj.getString("lon"));
            JSONObject addr = obj.optJSONObject("address");

            String city = "";
            String road = "";
            String houseNumber = "";
            String suburb = "";
            String postcode = "";
            String state = "";

            if (addr != null) {
                road = optFirstNonEmpty(addr, "road", "pedestrian", "residential",
                        "footway", "living_street", "square", "place", "street");
                houseNumber = optFirstNonEmpty(addr, "house_number");
                city = cleanCityName(optFirstNonEmpty(addr, "city", "town", "village", "municipality", "county"));
                suburb = optFirstNonEmpty(addr, "suburb", "city_district", "quarter", "neighbourhood");
                postcode = optFirstNonEmpty(addr, "postcode");
                state = shortenVoivodeship(optFirstNonEmpty(addr, "state"));
            }

            String primary;
            if (!road.isEmpty()) {
                primary = !houseNumber.isEmpty() ? road + " " + houseNumber : road;
            } else {
                String explicitName = safeStr(obj.optString("name"));
                if (!explicitName.isEmpty()) {
                    primary = explicitName;
                } else {
                    String display = safeStr(obj.optString("display_name"));
                    int comma = display.indexOf(',');
                    primary = comma > 0 ? display.substring(0, comma).trim() : (!city.isEmpty() ? city : query);
                }
            }

            List<String> metaParts = new ArrayList<>();
            if (!suburb.isEmpty() && !sameFolded(suburb, city) && !sameFolded(suburb, primary)) {
                metaParts.add(suburb);
            }
            if (!postcode.isEmpty()) {
                metaParts.add(postcode);
            }
            if (!state.isEmpty() && !sameFolded(state, city)) {
                metaParts.add(state);
            }
            if (metaParts.isEmpty() && !city.isEmpty() && !sameFolded(city, primary)) {
                metaParts.add(city);
            }

            return new RawCandidate(primary, city, joinComma(metaParts), new LatLng(lat, lon));
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private List<AddressSuggestion> deduplicateAndRankSuggestions(@NonNull List<RawCandidate> rawCandidates,
                                                                    @NonNull String query,
                                                                    @Nullable LatLng userLocation,
                                                                    @NonNull String userCity,
                                                                    @Nullable LatLng mapCenter) {
        if (rawCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<RawCandidate> merged = new ArrayList<>();
        for (RawCandidate incoming : rawCandidates) {
            int duplicateIndex = findDuplicateIndex(merged, incoming);
            if (duplicateIndex >= 0) {
                merged.set(duplicateIndex, mergePreferringRicher(merged.get(duplicateIndex), incoming));
            } else {
                merged.add(incoming);
            }
        }

        String foldedQuery = MapSpotFilterHelper.foldPolish(query);
        List<String> queryNumbers = extractNumberTokens(foldedQuery);
        List<RankedSuggestion> ranked = new ArrayList<>();

        for (RawCandidate c : merged) {
            double distMeters = Double.NaN;
            boolean nearUser = false;
            if (userLocation != null) {
                distMeters = MapSpotFilterHelper.distanceMeters(
                        userLocation.latitude, userLocation.longitude,
                        c.latLng.latitude, c.latLng.longitude);
                nearUser = distMeters <= NEAR_USER_RADIUS_METERS
                        || (!userCity.isEmpty() && !c.city.isEmpty() && sameFolded(c.city, userCity));
            }

            boolean cityMatchesQuery = !c.city.isEmpty()
                    && containsFoldedWord(foldedQuery, MapSpotFilterHelper.foldPolish(c.city));
            boolean numbersMatch = matchesAllNumbers(c.primaryText, queryNumbers);
            double sortDistance = !Double.isNaN(distMeters)
                    ? distMeters
                    : (mapCenter != null
                    ? MapSpotFilterHelper.distanceMeters(
                    mapCenter.latitude, mapCenter.longitude,
                    c.latLng.latitude, c.latLng.longitude)
                    : Double.MAX_VALUE);

            String fullLabel = buildFullLabel(c.primaryText, c.city);
            AddressSuggestion suggestion = new AddressSuggestion(
                    c.primaryText,
                    c.city,
                    c.secondaryText,
                    fullLabel,
                    c.latLng,
                    distMeters,
                    nearUser);
            ranked.add(new RankedSuggestion(suggestion, cityMatchesQuery, numbersMatch, sortDistance));
        }

        Collections.sort(ranked, new Comparator<RankedSuggestion>() {
            @Override
            public int compare(RankedSuggestion a, RankedSuggestion b) {
                if (a.suggestion.isNearUserLocation() != b.suggestion.isNearUserLocation()) {
                    return a.suggestion.isNearUserLocation() ? -1 : 1;
                }
                if (a.cityMatchesQuery != b.cityMatchesQuery) {
                    return a.cityMatchesQuery ? -1 : 1;
                }
                if (a.numbersMatch != b.numbersMatch) {
                    return a.numbersMatch ? -1 : 1;
                }
                int distCmp = Double.compare(a.sortDistance, b.sortDistance);
                if (distCmp != 0) {
                    return distCmp;
                }
                return a.suggestion.getCity().compareToIgnoreCase(b.suggestion.getCity());
            }
        });

        List<AddressSuggestion> result = new ArrayList<>();
        int maxCount = Math.min(ranked.size(), 15);
        for (int i = 0; i < maxCount; i++) {
            result.add(ranked.get(i).suggestion);
        }
        return result;
    }

    private static class RankedSuggestion {
        @NonNull final AddressSuggestion suggestion;
        final boolean cityMatchesQuery;
        final boolean numbersMatch;
        final double sortDistance;

        RankedSuggestion(@NonNull AddressSuggestion suggestion,
                         boolean cityMatchesQuery,
                         boolean numbersMatch,
                         double sortDistance) {
            this.suggestion = suggestion;
            this.cityMatchesQuery = cityMatchesQuery;
            this.numbersMatch = numbersMatch;
            this.sortDistance = sortDistance;
        }
    }

    private int findDuplicateIndex(@NonNull List<RawCandidate> existing, @NonNull RawCandidate incoming) {
        String inPrimary = MapSpotFilterHelper.foldPolish(incoming.primaryText);
        String inCity = MapSpotFilterHelper.foldPolish(incoming.city);
        for (int i = 0; i < existing.size(); i++) {
            RawCandidate cur = existing.get(i);
            double gap = MapSpotFilterHelper.distanceMeters(
                    cur.latLng.latitude, cur.latLng.longitude,
                    incoming.latLng.latitude, incoming.latLng.longitude);
            String curPrimary = MapSpotFilterHelper.foldPolish(cur.primaryText);
            String curCity = MapSpotFilterHelper.foldPolish(cur.city);
            if (gap < 300.0 && (curPrimary.equals(inPrimary) || curCity.equals(inCity) || inCity.isEmpty() || curCity.isEmpty())) {
                return i;
            }
            if (gap < 3000.0 && !inCity.isEmpty() && curCity.equals(inCity) && curPrimary.equals(inPrimary)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private RawCandidate mergePreferringRicher(@NonNull RawCandidate a, @NonNull RawCandidate b) {
        String primary = scorePrimaryText(b.primaryText) > scorePrimaryText(a.primaryText) ? b.primaryText : a.primaryText;
        String city = !a.city.isEmpty() ? a.city : b.city;
        if (!b.city.isEmpty() && !a.city.isEmpty() && b.city.length() < a.city.length() && !b.city.contains(" ")) {
            // Keep clean concise city if both non-empty and equivalent, else keep existing first non-empty
            city = a.city;
        }
        String secondary = b.secondaryText.length() > a.secondaryText.length() ? b.secondaryText : a.secondaryText;
        return new RawCandidate(primary, city, secondary, a.latLng);
    }

    private int scorePrimaryText(@NonNull String text) {
        int score = text.length();
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                score += 20;
                break;
            }
        }
        return score;
    }

    private boolean hasCandidateNearLocation(@NonNull List<RawCandidate> candidates,
                                             @NonNull LatLng userLocation,
                                             @NonNull String userCity) {
        for (RawCandidate c : candidates) {
            double d = MapSpotFilterHelper.distanceMeters(
                    userLocation.latitude, userLocation.longitude,
                    c.latLng.latitude, c.latLng.longitude);
            if (d <= NEAR_USER_RADIUS_METERS) {
                return true;
            }
            if (!userCity.isEmpty() && !c.city.isEmpty() && sameFolded(c.city, userCity)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private Set<String> collectDistinctCities(@NonNull List<AddressSuggestion> candidates) {
        Set<String> cities = new LinkedHashSet<>();
        for (AddressSuggestion s : candidates) {
            String folded = MapSpotFilterHelper.foldPolish(s.getCity());
            if (!folded.isEmpty()) {
                cities.add(folded);
            }
        }
        return cities;
    }

    @Nullable
    private AddressSuggestion findExplicitCityMatch(@NonNull String rawQuery,
                                                    @NonNull List<AddressSuggestion> candidates) {
        String foldedQuery = MapSpotFilterHelper.foldPolish(rawQuery);
        for (AddressSuggestion s : candidates) {
            String foldedCity = MapSpotFilterHelper.foldPolish(s.getCity());
            if (foldedCity.length() >= 3 && containsFoldedWord(foldedQuery, foldedCity)) {
                return s;
            }
        }
        return null;
    }

    private boolean containsFoldedWord(@NonNull String haystackFolded, @NonNull String wordFolded) {
        if (wordFolded.isEmpty() || haystackFolded.isEmpty()) {
            return false;
        }
        int idx = haystackFolded.indexOf(wordFolded);
        while (idx >= 0) {
            boolean leftBoundary = idx == 0 || !Character.isLetterOrDigit(haystackFolded.charAt(idx - 1));
            int endIdx = idx + wordFolded.length();
            boolean rightBoundary = endIdx == haystackFolded.length()
                    || !Character.isLetterOrDigit(haystackFolded.charAt(endIdx));
            if (leftBoundary && rightBoundary) {
                return true;
            }
            idx = haystackFolded.indexOf(wordFolded, idx + 1);
        }
        return false;
    }

    @NonNull
    private List<String> extractNumberTokens(@NonNull String foldedQuery) {
        List<String> numbers = new ArrayList<>();
        String[] tokens = foldedQuery.split("\\s+");
        for (String token : tokens) {
            if (!token.isEmpty() && Character.isDigit(token.charAt(0))) {
                numbers.add(token);
            }
        }
        return numbers;
    }

    private boolean matchesAllNumbers(@NonNull String primaryText, @NonNull List<String> numbers) {
        if (numbers.isEmpty()) {
            return true;
        }
        String foldedPrimary = MapSpotFilterHelper.foldPolish(primaryText);
        for (String num : numbers) {
            if (!containsFoldedWord(foldedPrimary, num)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private String buildFullLabel(@NonNull String primary, @NonNull String city) {
        if (city.isEmpty() || sameFolded(primary, city) || containsFoldedWord(
                MapSpotFilterHelper.foldPolish(primary), MapSpotFilterHelper.foldPolish(city))) {
            return primary;
        }
        return primary + ", " + city;
    }

    @NonNull
    private String buildCacheKey(@NonNull String trimmedQuery, @Nullable LatLng userLocation) {
        String folded = MapSpotFilterHelper.foldPolish(trimmedQuery);
        if (userLocation == null) {
            return folded + "|noloc";
        }
        return String.format(Locale.US, "%s|%.2f,%.2f", folded, userLocation.latitude, userLocation.longitude);
    }

    @NonNull
    private String extractCityFromGeocoderAddress(@NonNull Address addr) {
        String locality = safeStr(addr.getLocality());
        if (!locality.isEmpty()) {
            return cleanCityName(locality);
        }
        String subAdmin = safeStr(addr.getSubAdminArea());
        if (!subAdmin.isEmpty()) {
            return cleanCityName(subAdmin);
        }
        return cleanCityName(safeStr(addr.getAdminArea()));
    }

    @NonNull
    private String cleanCityName(@NonNull String city) {
        String cleaned = city.trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("gmina ")) {
            cleaned = cleaned.substring(6).trim();
        } else if (lower.startsWith("powiat ")) {
            cleaned = cleaned.substring(7).trim();
        }
        return cleaned;
    }

    @NonNull
    private String shortenVoivodeship(@NonNull String state) {
        if (state.isEmpty()) return "";
        String lower = state.toLowerCase(Locale.ROOT);
        if (lower.startsWith("województwo ")) {
            return "woj. " + state.substring("województwo ".length()).trim();
        }
        return state;
    }

    @NonNull
    private String readStream(@NonNull HttpURLConnection conn) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    @NonNull
    private String optFirstNonEmpty(@NonNull JSONObject obj, @NonNull String... keys) {
        for (String key : keys) {
            String val = safeStr(obj.optString(key));
            if (!val.isEmpty()) {
                return val;
            }
        }
        return "";
    }

    @NonNull
    private String joinComma(@NonNull List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
        return sb.toString();
    }

    private boolean sameFolded(@NonNull String a, @NonNull String b) {
        return !a.isEmpty() && MapSpotFilterHelper.foldPolish(a).equals(MapSpotFilterHelper.foldPolish(b));
    }

    @NonNull
    private String safeStr(@Nullable String s) {
        return s != null ? s.trim() : "";
    }

    @Nullable
    private SearchResult tryParseDirectCoordinates(@NonNull String query) {
        String[] parts = query.split(",");
        if (parts.length != 2) return null;
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0) {
                return new SearchResult(new LatLng(lat, lng),
                        String.format(Locale.US, "%.4f, %.4f", lat, lng));
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        suggestionExecutor.shutdown();
    }
}
