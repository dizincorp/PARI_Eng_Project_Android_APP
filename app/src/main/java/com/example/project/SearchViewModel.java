package com.example.project;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchViewModel extends ViewModel {

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

    private final MutableLiveData<SearchResult> searchResultLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public LiveData<SearchResult> getSearchResultLiveData() {
        return searchResultLiveData;
    }

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

                    mainHandler.post(() -> searchResultLiveData.setValue(new SearchResult(latLng, title)));
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