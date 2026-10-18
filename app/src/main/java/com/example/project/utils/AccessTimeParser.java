package com.example.project.utils;

/**
 * Utility class for parsing owner access time range strings (e.g. "12:00 - 14:00").
 */
public class AccessTimeParser {

    /**
     * Extracts allowed start hour from access time string.
     *
     * @param accessTime Access time string
     * @return Start hour (0-23)
     */
    public static int getStartHour(String accessTime) {
        try {
            if (accessTime != null && accessTime.contains("-")) {
                String startPart = accessTime.split("-")[0].trim();
                return Integer.parseInt(startPart.split(":")[0].trim());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Extracts allowed start minute from access time string.
     *
     * @param accessTime Access time string
     * @return Start minute (0-59)
     */
    public static int getStartMinute(String accessTime) {
        try {
            if (accessTime != null && accessTime.contains("-")) {
                String startPart = accessTime.split("-")[0].trim();
                String[] parts = startPart.split(":");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1].trim());
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Extracts allowed end hour from access time string.
     *
     * @param accessTime Access time string
     * @return End hour (0-23)
     */
    public static int getEndHour(String accessTime) {
        try {
            if (accessTime != null && accessTime.contains("-")) {
                String endPart = accessTime.split("-")[1].trim();
                return Integer.parseInt(endPart.split(":")[0].trim());
            }
        } catch (Exception ignored) {
        }
        return 23;
    }

    /**
     * Extracts allowed end minute from access time string.
     *
     * @param accessTime Access time string
     * @return End minute (0-59)
     */
    public static int getEndMinute(String accessTime) {
        try {
            if (accessTime != null && accessTime.contains("-")) {
                String endPart = accessTime.split("-")[1].trim();
                String[] parts = endPart.split(":");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1].trim());
                }
            }
        } catch (Exception ignored) {
        }
        return 59;
    }
}