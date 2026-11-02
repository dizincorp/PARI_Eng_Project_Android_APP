package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing an uploaded photo associated with a parking spot.
 */
@Entity(
        tableName = "spot_photos",
        indices = {@Index(value = "spotId")}
)
public class SpotPhotoEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String spotId;

    @NonNull
    private String storageKey;

    @NonNull
    private String url;

    private int position;

    /**
     * Default no-arg constructor required by Room.
     */
    public SpotPhotoEntity() {
        this.id = "";
        this.spotId = "";
        this.storageKey = "";
        this.url = "";
        this.position = 0;
    }

    /**
     * Parameterized constructor.
     *
     * @param id         Unique photo identifier
     * @param spotId     Parent spot identifier
     * @param storageKey Storage key relative to backend media storage
     * @param url        Public or signed access URL
     * @param position   Display ordering position
     */
    public SpotPhotoEntity(@NonNull String id,
                           @NonNull String spotId,
                           @NonNull String storageKey,
                           @NonNull String url,
                           int position) {
        this.id = id;
        this.spotId = spotId;
        this.storageKey = storageKey;
        this.url = url;
        this.position = position;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(@NonNull String spotId) {
        this.spotId = spotId;
    }

    @NonNull
    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(@NonNull String storageKey) {
        this.storageKey = storageKey;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public void setUrl(@NonNull String url) {
        this.url = url;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
