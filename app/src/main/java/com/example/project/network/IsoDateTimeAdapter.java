package com.example.project.network;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Custom Gson TypeAdapters for robust serialization and deserialization of ISO-8601 timestamps.
 * Supports {@link Date}, {@link LocalDateTime}, and {@link ZonedDateTime} across multiple
 * backend date format variants (with or without millisecond fractions, UTC 'Z', and timezone offsets).
 */
public class IsoDateTimeAdapter {

    private static final String TAG = "IsoDateTimeAdapter";

    /**
     * Standard ISO-8601 UTC date formats used as fallbacks for {@link Date} deserialization.
     */
    private static final String[] ISO_PATTERNS = new String[]{
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
    };

    /**
     * Gson Adapter for {@link Date}.
     */
    public static class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return new JsonPrimitive(formatter.format(src));
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            // Case 1: Numeric timestamp (epoch milliseconds or seconds)
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                long val = json.getAsLong();
                // If seconds instead of milliseconds (e.g. Unix timestamp < year 2100)
                if (val < 100000000000L) {
                    val *= 1000L;
                }
                return new Date(val);
            }

            String dateStr = json.getAsString();
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }

            dateStr = dateStr.trim();

            // Try standard ISO patterns
            for (String pattern : ISO_PATTERNS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return sdf.parse(dateStr);
                } catch (ParseException ignored) {
                    // Try next pattern
                }
            }

            // Fallback: try parsing as numeric string
            try {
                long val = Long.parseLong(dateStr);
                if (val < 100000000000L) {
                    val *= 1000L;
                }
                return new Date(val);
            } catch (NumberFormatException ignored) {
            }

            Log.e(TAG, "Unparseable date string received: " + dateStr);
            return null;
        }
    }

    /**
     * Gson Adapter for {@link LocalDateTime}.
     */
    public static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            String dateStr = json.getAsString();
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }

            dateStr = dateStr.trim();

            try {
                // Try ISO_DATE_TIME first (handles 2026-10-25T17:24:55.992Z or with offset)
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e1) {
                try {
                    // Try parsing as Instant and converting to system default zone
                    Instant instant = Instant.parse(dateStr);
                    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                } catch (Exception e2) {
                    try {
                        return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e3) {
                        Log.e(TAG, "Unparseable LocalDateTime: " + dateStr, e3);
                        return null;
                    }
                }
            }
        }
    }

    /**
     * Gson Adapter for {@link ZonedDateTime}.
     */
    public static class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {

        @Override
        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            String dateStr = json.getAsString();
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }

            dateStr = dateStr.trim();

            try {
                return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e1) {
                try {
                    Instant instant = Instant.parse(dateStr);
                    return instant.atZone(ZoneId.of("UTC"));
                } catch (Exception e2) {
                    try {
                        LocalDateTime local = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        return local.atZone(ZoneOffset.UTC);
                    } catch (Exception e3) {
                        Log.e(TAG, "Unparseable ZonedDateTime: " + dateStr, e3);
                        return null;
                    }
                }
            }
        }
    }
}
