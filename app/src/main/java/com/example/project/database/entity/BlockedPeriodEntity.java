package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a calendar blocked period for a parking spot.
 */
@Entity(tableName = "blocked_periods", indices = {@Index("spotId")})
public class BlockedPeriodEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String spotId;

    private String start; // ISO-8601 string
    private String end;   // ISO-8601 string
    private String reason;

    /**
     * Default no-arg constructor required by Room.
     */
    public BlockedPeriodEntity() {
        this.id = "";
        this.spotId = "";
        this.start = "";
        this.end = "";
        this.reason = "";
    }

    /**
     * Parameterized constructor.
     *
     * @param id     Unique block identifier
     * @param spotId Spot identifier
     * @param start  Start ISO-8601 timestamp
     * @param end    End ISO-8601 timestamp
     * @param reason Reason description
     */
    @Ignore
    public BlockedPeriodEntity(@NonNull String id, @NonNull String spotId, String start, String end, String reason) {
        this.id = id;
        this.spotId = spotId;
        this.start = start != null ? start : "";
        this.end = end != null ? end : "";
        this.reason = reason != null ? reason : "";
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

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Parses the start ISO-8601 string to epoch milliseconds.
     *
     * @return Epoch milliseconds or 0 if unparseable
     */
    @Ignore
    public long getStartMillis() {
        return parseIsoToMillis(start);
    }

    /**
     * Parses the end ISO-8601 string to epoch milliseconds.
     *
     * @return Epoch milliseconds or 0 if unparseable
     */
    @Ignore
    public long getEndMillis() {
        return parseIsoToMillis(end);
    }

    /**
     * Checks if this blocked period overlaps with a requested time interval [reqStartMs, reqEndMs).
     *
     * @param reqStartMs Requested start time in milliseconds
     * @param reqEndMs   Requested end time in milliseconds
     * @return True if overlapping, false otherwise
     */
    @Ignore
    public boolean isOverlapping(long reqStartMs, long reqEndMs) {
        if (isExpired()) return false;
        long bStart = getStartMillis();
        long bEnd = getEndMillis();
        if (bStart <= 0 || bEnd <= 0) return false;
        return reqStartMs < bEnd && reqEndMs > bStart;
    }

    /**
     * Checks if this block is currently active at this moment in time.
     *
     * @return True if system current time is within [start, end)
     */
    @Ignore
    public boolean isCurrentlyActive() {
        long now = System.currentTimeMillis();
        long bStart = getStartMillis();
        long bEnd = getEndMillis();
        return bStart > 0 && bEnd > 0 && now >= bStart && now < bEnd;
    }

    /**
     * Checks if this block has expired (i.e. end timestamp is in the past).
     *
     * @return True if block end time is less than or equal to current system time
     */
    @Ignore
    public boolean isExpired() {
        long bEnd = getEndMillis();
        return bEnd > 0 && bEnd <= System.currentTimeMillis();
    }

    /**
     * Checks if this block is active or in the future (i.e. not yet expired).
     *
     * @return True if block has not expired yet
     */
    @Ignore
    public boolean isActiveOrUpcoming() {
        return !isExpired();
    }

    /**
     * Returns a human-readable formatted string for this blocked period range.
     *
     * @return Formatted date-time interval (e.g. 2026-11-01 17:00 - 20:00)
     */
    @Ignore
    public String getFormattedRange() {
        long bStart = getStartMillis();
        long bEnd = getEndMillis();
        if (bStart <= 0 || bEnd <= 0) {
            String s = start != null ? start : "";
            String e = end != null ? end : "";
            return (s.isEmpty() && e.isEmpty()) ? "Termin zablokowany" : (s + " - " + e);
        }
        java.text.SimpleDateFormat sdfFull = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        java.text.SimpleDateFormat sdfDay = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

        String dayStart = sdfDay.format(new java.util.Date(bStart));
        String dayEnd = sdfDay.format(new java.util.Date(bEnd));
        if (dayStart.equals(dayEnd)) {
            return dayStart + " " + sdfTime.format(new java.util.Date(bStart)) + " - " + sdfTime.format(new java.util.Date(bEnd));
        } else {
            return sdfFull.format(new java.util.Date(bStart)) + " - " + sdfFull.format(new java.util.Date(bEnd));
        }
    }

    /**
     * Returns a human-readable notice describing the blocked timeframe.
     * For example: "18:00 - 20:00 zablokowane" (if today or same day)
     * or "2026-11-01 18:00 - 20:00 zablokowane".
     *
     * @return Formatted blocked notice string
     */
    @Ignore
    public String getFormattedBlockedNotice() {
        long bStart = getStartMillis();
        long bEnd = getEndMillis();
        if (bStart <= 0 || bEnd <= 0) {
            String s = start != null ? start : "";
            String e = end != null ? end : "";
            return (s.isEmpty() && e.isEmpty()) ? "zablokowane" : (s + " - " + e + " zablokowane");
        }
        java.text.SimpleDateFormat sdfDay = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        java.text.SimpleDateFormat sdfFull = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());

        String today = sdfDay.format(new java.util.Date());
        String dayStart = sdfDay.format(new java.util.Date(bStart));
        String dayEnd = sdfDay.format(new java.util.Date(bEnd));

        if (dayStart.equals(dayEnd)) {
            if (dayStart.equals(today)) {
                return sdfTime.format(new java.util.Date(bStart)) + " - " + sdfTime.format(new java.util.Date(bEnd)) + " zablokowane";
            } else {
                return dayStart + " " + sdfTime.format(new java.util.Date(bStart)) + " - " + sdfTime.format(new java.util.Date(bEnd)) + " zablokowane";
            }
        } else {
            return sdfFull.format(new java.util.Date(bStart)) + " - " + sdfFull.format(new java.util.Date(bEnd)) + " zablokowane";
        }
    }

    /**
     * Utility method converting an ISO-8601 string or numeric timestamp to epoch milliseconds.
     *
     * @param iso Date-time string in ISO-8601 format
     * @return Milliseconds timestamp
     */
    @Ignore
    public static long parseIsoToMillis(String iso) {
        if (iso == null || iso.trim().isEmpty()) return 0L;
        String clean = iso.trim();
        try {
            return java.time.Instant.parse(clean).toEpochMilli();
        } catch (Throwable ignored) {}
        try {
            return java.time.ZonedDateTime.parse(clean, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant()
                    .toEpochMilli();
        } catch (Throwable ignored) {}
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d = sdf.parse(clean);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d = sdf.parse(clean);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            java.util.Date d = sdf.parse(clean);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(clean);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(clean);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        try {
            return Long.parseLong(clean);
        } catch (Exception ignored) {}
        return 0L;
    }
}
