package com.example.project.network.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing a spot photo returned by
 * POST /api/spots/{id}/photos.
 */
public class SpotPhotoResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("storageKey")
    private String storageKey;

    @SerializedName("url")
    private String url;

    @SerializedName("position")
    private int position;

    /**
     * Default no-arg constructor required by Gson.
     */
    public SpotPhotoResponse() {
        this.id = "";
        this.storageKey = "";
        this.url = "";
        this.position = 0;
    }

    /**
     * Parameterized constructor.
     *
     * @param id         Unique photo identifier
     * @param storageKey Storage key in cloud storage
     * @param url        Public or signed URL of the photo
     * @param position   Display ordering position index
     */
    public SpotPhotoResponse(@Nullable String id,
                             @Nullable String storageKey,
                             @Nullable String url,
                             int position) {
        this.id = id != null ? id : "";
        this.storageKey = storageKey != null ? storageKey : "";
        this.url = url != null ? url : "";
        this.position = position;
    }

    @NonNull
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(@Nullable String id) {
        this.id = id != null ? id : "";
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
