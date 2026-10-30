package com.example.project.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a paginated response of parking spot reviews.
 */
public class ReviewPageResponse {

    @SerializedName("content")
    private List<ReviewResponse> content;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    /**
     * Default constructor initializing an empty review list.
     */
    public ReviewPageResponse() {
        this.content = new ArrayList<>();
    }

    /**
     * Parameterized constructor for ReviewPageResponse.
     *
     * @param content       List of ReviewResponse items
     * @param totalElements Total count of reviews available on backend
     * @param totalPages    Total pages available
     * @param page          Current zero-based page index
     * @param size          Page size
     */
    public ReviewPageResponse(List<ReviewResponse> content, long totalElements,
                              int totalPages, int page, int size) {
        this.content = content != null ? content : new ArrayList<>();
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    /**
     * Gets the list of reviews on the current page.
     *
     * @return List of ReviewResponse objects
     */
    public List<ReviewResponse> getContent() {
        if (content == null) {
            content = new ArrayList<>();
        }
        return content;
    }

    /**
     * Sets the list of reviews on the current page.
     *
     * @param content List of ReviewResponse objects
     */
    public void setContent(List<ReviewResponse> content) {
        this.content = content != null ? content : new ArrayList<>();
    }

    /**
     * Gets the total number of reviews across all pages.
     *
     * @return Total review count
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * Sets the total number of reviews across all pages.
     *
     * @param totalElements Total review count
     */
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     * Gets total pages count.
     *
     * @return Total pages count
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Sets total pages count.
     *
     * @param totalPages Total pages count
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * Gets current page index (zero-based).
     *
     * @return Page index
     */
    public int getPage() {
        return page;
    }

    /**
     * Sets current page index.
     *
     * @param page Page index
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Gets page size.
     *
     * @return Page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets page size.
     *
     * @param size Page size
     */
    public void setSize(int size) {
        this.size = size;
    }
}
