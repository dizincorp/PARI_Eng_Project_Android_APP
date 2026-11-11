package com.example.project.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility helper to format backend amenity codes (e.g. CCTV, LIGHTING, EV_CHARGER, COVERED)
 * into localized, user-friendly labels for UI Chip badges across all spot listings.
 */
public final class AmenityUtils {

    private AmenityUtils() {
        // Prevent instantiation
    }

    /**
     * Converts a backend amenity code or raw text into a clean user-facing display label.
     *
     * @param code Raw amenity code or description
     * @return Formatted label (e.g. "Monitoring", "Oświetlenie", "EV Charging")
     */
    @NonNull
    public static String formatAmenity(@Nullable String code) {
        if (code == null || code.trim().isEmpty()) {
            return "";
        }
        String trimmed = code.trim();
        switch (trimmed.toUpperCase()) {
            case "CCTV":
            case "MONITORING":
                return "Monitoring";
            case "LIGHTING":
            case "OSWIETLENIE":
            case "OŚWIETLENIE":
                return "Oświetlenie";
            case "EV_CHARGER":
            case "EV":
            case "EV CHARGING":
            case "LADOWANIE EV":
            case "ŁADOWANIE EV":
                return "EV Charging";
            case "COVERED":
            case "ZADASZONE":
                return "Zadaszone";
            case "UNDERGROUND":
            case "PODZIEMNY":
                return "Podziemny";
            case "GATE":
            case "BRAMA":
            case "BRAMA AUTOMATYCZNA":
                return "Brama automatyczna";
            case "DISABLED_ACCESS":
            case "NIEPELNOSPRAWNI":
            case "DLA NIEPEŁNOSPRAWNYCH":
                return "Dla niepełnosprawnych";
            case "WIDE_SPACE":
            case "SZEROKIE MIEJSCE":
                return "Szerokie miejsce";
            default:
                if (trimmed.length() > 1) {
                    return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
                }
                return trimmed;
        }
    }

    /**
     * Parses a comma-separated amenities CSV string into formatted display labels.
     */
    @NonNull
    public static List<String> parseAmenitiesCsv(@Nullable String amenitiesCsv) {
        List<String> result = new ArrayList<>();
        if (amenitiesCsv == null || amenitiesCsv.trim().isEmpty()) {
            return result;
        }
        String[] parts = amenitiesCsv.split(",");
        for (String part : parts) {
            String formatted = formatAmenity(part);
            if (!formatted.isEmpty() && !result.contains(formatted)) {
                result.add(formatted);
            }
        }
        return result;
    }
}
