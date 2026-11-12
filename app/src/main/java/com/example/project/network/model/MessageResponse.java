package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

/**
 * Generic response payload delivering a status or confirmation message from backend endpoints.
 */
public class MessageResponse {

    @SerializedName("message")
    private String message;

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public MessageResponse() {
        this.message = "";
    }

    /**
     * Parameterized constructor.
     *
     * @param message Response message string
     */
    public MessageResponse(String message) {
        this.message = message != null ? message : "";
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
