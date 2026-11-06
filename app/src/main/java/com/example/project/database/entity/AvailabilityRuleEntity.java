package com.example.project.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a weekly availability rule for a parking spot.
 */
@Entity(tableName = "availability_rules", indices = {@Index("spotId")})
public class AvailabilityRuleEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String spotId;

    private int dayOfWeek; // 1 = Monday ... 7 = Sunday
    private String startTime; // "08:00"
    private String endTime; // "20:00"
    private String validFrom; // "YYYY-MM-DD"
    private String validTo; // "YYYY-MM-DD"

    /**
     * Default no-arg constructor required by Room.
     */
    public AvailabilityRuleEntity() {
        this.id = "";
        this.spotId = "";
        this.startTime = "08:00";
        this.endTime = "20:00";
    }

    /**
     * Parameterized constructor.
     *
     * @param id        Unique rule identifier
     * @param spotId    Parent parking spot identifier
     * @param dayOfWeek Day of week (1..7)
     * @param startTime Start time (HH:mm)
     * @param endTime   End time (HH:mm)
     * @param validFrom Start validity date
     * @param validTo   End validity date
     */
    @Ignore
    public AvailabilityRuleEntity(@NonNull String id, @NonNull String spotId, int dayOfWeek,
                                  String startTime, String endTime, String validFrom, String validTo) {
        this.id = id;
        this.spotId = spotId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime != null ? startTime : "08:00";
        this.endTime = endTime != null ? endTime : "20:00";
        this.validFrom = validFrom;
        this.validTo = validTo;
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

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    @Ignore
    public int getStartHour() {
        try {
            if (startTime != null && startTime.contains(":")) {
                return Integer.parseInt(startTime.split(":")[0].trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Ignore
    public int getStartMinute() {
        try {
            if (startTime != null && startTime.contains(":")) {
                return Integer.parseInt(startTime.split(":")[1].trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Ignore
    public int getEndHour() {
        try {
            if (endTime != null && endTime.contains(":")) {
                return Integer.parseInt(endTime.split(":")[0].trim());
            }
        } catch (Exception ignored) {}
        return 23;
    }

    @Ignore
    public int getEndMinute() {
        try {
            if (endTime != null && endTime.contains(":")) {
                return Integer.parseInt(endTime.split(":")[1].trim());
            }
        } catch (Exception ignored) {}
        return 59;
    }
}
