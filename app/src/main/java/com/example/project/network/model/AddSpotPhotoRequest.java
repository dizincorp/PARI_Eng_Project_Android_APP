package com.example.project.network.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for associating an uploaded photo to a spot via
 * POST /api/spots/{id}/photos.
 */
public class AddSpotPhotoRequest {

    @SerializedName("storageKey")
    private String storageKey;

    @SerializedName("url")
    private String url;

    @SerializedName("position")
    private int position;

    /**
     * Default no-arg constructor required by Gson.
     */
    public AddSpotPhotoRequest() {
        this.storageKey = "";
        this.url = "";
        this.position = 0;
    }

    /**
     * Parameterized constructor.
     *
     * @param storageKey Storage key returned by /api/media/upload
     * @param url        Public or signed URL of the photo
     * @param position   Display ordering index (e.g. 0 for primary photo)
     */
    public AddSpotPhotoRequest(@Nullable String storageKey,
                               @Nullable String url,
                               int position) {
        this.storageKey = storageKey != null ? storageKey : "";
        this.url = url != null ? url : "";
        this.position = position;
    }

    @NonNull
    public String getStorageKey() {
        return storageKey != null ? storageKey : "";
    }

    public void setStorageKey(@Nullable String storageKey) {
        this.storageKey = storageKey != null ? storageKey : "";
    }

    @NonNull
    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(@Nullable String url) {
        this.url = url != null ? url : "";
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
