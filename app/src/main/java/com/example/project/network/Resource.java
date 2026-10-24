package com.example.project.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic wrapper class representing a network resource state (loading, success, error) for LiveData.
 *
 * @param <T> Payload data type
 */
public class Resource<T> {

    /**
     * Enumeration of possible resource statuses.
     */
    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    @NonNull
    public final Status status;

    @Nullable
    public final T data;

    @Nullable
    public final String message;

    public Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    /**
     * Creates a successful Resource instance holding data.
     *
     * @param data Successful payload
     * @param <T>  Data type
     * @return Success Resource instance
     */
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    /**
     * Creates an error Resource instance with an error message and optional data.
     *
     * @param message Error description message
     * @param data    Optional fallback data
     * @param <T>     Data type
     * @return Error Resource instance
     */
    public static <T> Resource<T> error(@Nullable String message, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, message != null ? message : "Unknown error");
    }

    /**
     * Creates a loading Resource instance.
     *
     * @param data Optional current data
     * @param <T>  Data type
     * @return Loading Resource instance
     */
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    /**
     * Gets the current status of the resource.
     *
     * @return Resource status
     */
    @NonNull
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the payload data.
     *
     * @return Data payload, or null if unavailable
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Gets the error or status message.
     *
     * @return Status or error message
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * Checks if the resource is in a successful state.
     *
     * @return True if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the resource is currently loading.
     *
     * @return True if status is LOADING
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }

    /**
     * Checks if the resource encountered an error.
     *
     * @return True if status is ERROR
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
}
