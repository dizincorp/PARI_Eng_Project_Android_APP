package com.example.project.network.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for setting the authenticated user's avatar via
 * POST /api/users/me/avatar using an existing media storage key or URL.
 */
public class UpdateAvatarRequest {

    @SerializedName("storageKey")
    private String storageKey;

    @SerializedName("url")
    private String url;

    /**
     * Default no-arg constructor required by Gson.
     */
    public UpdateAvatarRequest() {
        this.storageKey = "";
        this.url = "";
    }

    /**
     * Parameterized constructor.
     *
     * @param storageKey Media storage key
     * @param url        Direct media URL
     */
    public UpdateAvatarRequest(@Nullable String storageKey, @Nullable String url) {
        this.storageKey = storageKey != null ? storageKey : "";
        this.url = url != null ? url : "";
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
}
