package com.example.project.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.project.network.model.AvailabilityRuleRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model representing the availability configuration for a specific day of the week (1..7).
 * Supports full day availability ("Cały dzień" / 00:00 - 23:59) or specific time bounds.
 */
public class DayScheduleConfig implements Serializable {

    private int dayOfWeek; // 1 = Monday ... 7 = Sunday
    private String dayName;
    private String dayShort;
    private boolean enabled;
    private boolean allDay;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    public DayScheduleConfig(int dayOfWeek, String dayName, String dayShort) {
        this.dayOfWeek = dayOfWeek;
        this.dayName = dayName;
        this.dayShort = dayShort;
        this.enabled = false;
        this.allDay = false;
        this.startHour = 8;
        this.startMinute = 0;
        this.endHour = 20;
        this.endMinute = 0;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public String getDayName() {
        return dayName;
    }

    public String getDayShort() {
        return dayShort;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    @NonNull
    public String getFormattedStartTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute);
    }

    @NonNull
    public String getFormattedEndTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
    }

    /**
     * Converts this day's schedule into a backend AvailabilityRuleRequest if enabled.
     *
     * @return {@link AvailabilityRuleRequest} or null if day is closed/disabled
     */
    @Nullable
    public AvailabilityRuleRequest toRuleRequest() {
        if (!enabled) {
            return null;
        }
        if (allDay) {
            return new AvailabilityRuleRequest(dayOfWeek, "00:00", "23:59", null, null);
        } else {
            return new AvailabilityRuleRequest(dayOfWeek, getFormattedStartTime(), getFormattedEndTime(), null, null);
        }
    }

    /**
     * Applies an incoming rule request into this day's configuration.
     *
     * @param request Incoming {@link AvailabilityRuleRequest}
     */
    public void applyRuleRequest(@NonNull AvailabilityRuleRequest request) {
        this.enabled = true;
        String start = request.getStartTime() != null ? request.getStartTime().trim() : "08:00";
        String end = request.getEndTime() != null ? request.getEndTime().trim() : "20:00";

        if (start.startsWith("00:00") && (end.startsWith("23:59") || end.startsWith("24:00"))) {
            this.allDay = true;
            this.startHour = 0;
            this.startMinute = 0;
            this.endHour = 23;
            this.endMinute = 59;
        } else {
            this.allDay = false;
            parseTimes(start, end);
        }
    }

    private void parseTimes(@NonNull String start, @NonNull String end) {
        try {
            String[] sParts = start.split(":");
            if (sParts.length >= 2) {
                this.startHour = Integer.parseInt(sParts[0]);
                this.startMinute = Integer.parseInt(sParts[1]);
            }
            String[] eParts = end.split(":");
            if (eParts.length >= 2) {
                this.endHour = Integer.parseInt(eParts[0]);
                this.endMinute = Integer.parseInt(eParts[1]);
            }
        } catch (Exception ignored) {
            this.startHour = 8;
            this.startMinute = 0;
            this.endHour = 20;
            this.endMinute = 0;
        }
    }

    /**
     * Returns a default week (Monday through Sunday) initialized.
     *
     * @return List of 7 DayScheduleConfig objects
     */
    @NonNull
    public static List<DayScheduleConfig> createDefaultWeek() {
        List<DayScheduleConfig> list = new ArrayList<>();
        list.add(new DayScheduleConfig(1, "Poniedziałek", "Pn"));
        list.add(new DayScheduleConfig(2, "Wtorek", "Wt"));
        list.add(new DayScheduleConfig(3, "Środa", "Śr"));
        list.add(new DayScheduleConfig(4, "Czwartek", "Czw"));
        list.add(new DayScheduleConfig(5, "Piątek", "Pt"));
        list.add(new DayScheduleConfig(6, "Sobota", "Sob"));
        list.add(new DayScheduleConfig(7, "Niedziela", "Nd"));
        return list;
    }
}
