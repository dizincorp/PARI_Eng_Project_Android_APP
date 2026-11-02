package com.example.project.network.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) representing uploaded media details returned by
 * POST /api/media/upload and POST /api/users/me/avatar.
 */
public class MediaUploadResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("storageKey")
    private String storageKey;

    @SerializedName("url")
    private String url;

    @SerializedName("originalFilename")
    private String originalFilename;

    @SerializedName("mimeType")
    private String mimeType;

    @SerializedName("sizeBytes")
    private long sizeBytes;

    /**
     * Default no-arg constructor required by Gson.
     */
    public MediaUploadResponse() {
        this.id = "";
        this.storageKey = "";
        this.url = "";
        this.originalFilename = "";
        this.mimeType = "";
        this.sizeBytes = 0L;
    }

    /**
     * Parameterized constructor.
     *
     * @param id               Unique media identifier
     * @param storageKey       Backend storage key
     * @param url              Direct access or CDN URL
     * @param originalFilename Original file name
     * @param mimeType         MIME media type
     * @param sizeBytes        File size in bytes
     */
    public MediaUploadResponse(@Nullable String id,
                               @Nullable String storageKey,
                               @Nullable String url,
                               @Nullable String originalFilename,
                               @Nullable String mimeType,
                               long sizeBytes) {
        this.id = id != null ? id : "";
        this.storageKey = storageKey != null ? storageKey : "";
        this.url = url != null ? url : "";
        this.originalFilename = originalFilename != null ? originalFilename : "";
        this.mimeType = mimeType != null ? mimeType : "";
        this.sizeBytes = sizeBytes;
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

    @NonNull
    public String getOriginalFilename() {
        return originalFilename != null ? originalFilename : "";
    }

    public void setOriginalFilename(@Nullable String originalFilename) {
        this.originalFilename = originalFilename != null ? originalFilename : "";
    }

    @NonNull
    public String getMimeType() {
        return mimeType != null ? mimeType : "";
    }

    public void setMimeType(@Nullable String mimeType) {
        this.mimeType = mimeType != null ? mimeType : "";
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
