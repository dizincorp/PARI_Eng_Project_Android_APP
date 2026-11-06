package com.example.project.model;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * Model representing a single day cell within the Availability Calendar month grid,
 * capturing whether it is in the past, free (green), reserved (yellow), or blocked (red).
 */
public class CalendarDay {

    public enum Status {
        PAST,
        FREE_GREEN,
        RESERVED_YELLOW,
        BLOCKED_RED
    }

    private final Date date;
    private final String dateString; // "yyyy-MM-dd"
    private final int dayOfMonth;
    private final boolean isCurrentMonth;
    private final boolean isToday;
    private Status status;
    private boolean isSelected;

    public CalendarDay(@NonNull Date date,
                       @NonNull String dateString,
                       int dayOfMonth,
                       boolean isCurrentMonth,
                       boolean isToday,
                       @NonNull Status status) {
        this.date = date;
        this.dateString = dateString;
        this.dayOfMonth = dayOfMonth;
        this.isCurrentMonth = isCurrentMonth;
        this.isToday = isToday;
        this.status = status;
        this.isSelected = false;
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    @NonNull
    public String getDateString() {
        return dateString;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public boolean isToday() {
        return isToday;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public void setStatus(@NonNull Status status) {
        this.status = status;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
