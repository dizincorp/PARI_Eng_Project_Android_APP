package com.example.project.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.project.database.entity.AvailabilityRuleEntity;
import com.example.project.database.entity.BlockedPeriodEntity;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.repository.SpotRepository;
import com.google.android.gms.maps.model.LatLng;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Helper encapsulating map spot filtering (price, dimensions, amenities, working hours, rating,
 * verification, auto-accept), distance calculation, and sorting around a searched address or center.
 */
public final class MapSpotFilterHelper {

    public enum HoursMode {
        ANY,
        OPEN_NOW,
        OPEN_24_7,
        CUSTOM_WINDOW
    }

    public enum SortMode {
        DISTANCE,
        PRICE_ASC,
        RATING_DESC
    }

    public static final class FilterCriteria {
        @Nullable
        private Double minPrice;
        @Nullable
        private Double maxPrice;
        private int minLengthCm = 0;
        private int minWidthCm = 0;
        private int minHeightCm = 0;
        private final Set<String> requiredAmenities = new HashSet<>();
        private HoursMode hoursMode = HoursMode.ANY;
        private int customDayOffset = 0; // 0 = today, 1 = tomorrow
        private int customStartMinutes = 8 * 60;
        private int customEndMinutes = 18 * 60;
        private boolean onlyVerified = false;
        private boolean onlyAutoAccept = false;
        private double minRating = 0.0;
        private double radiusMeters = 5000.0;
        private SortMode sortBy = SortMode.DISTANCE;

        public void reset() {
            minPrice = null;
            maxPrice = null;
            minLengthCm = 0;
            minWidthCm = 0;
            minHeightCm = 0;
            requiredAmenities.clear();
            hoursMode = HoursMode.ANY;
            customDayOffset = 0;
            customStartMinutes = 8 * 60;
            customEndMinutes = 18 * 60;
            onlyVerified = false;
            onlyAutoAccept = false;
            minRating = 0.0;
            radiusMeters = 5000.0;
            sortBy = SortMode.DISTANCE;
        }

        public FilterCriteria copy() {
            FilterCriteria c = new FilterCriteria();
            c.minPrice = this.minPrice;
            c.maxPrice = this.maxPrice;
            c.minLengthCm = this.minLengthCm;
            c.minWidthCm = this.minWidthCm;
            c.minHeightCm = this.minHeightCm;
            c.requiredAmenities.addAll(this.requiredAmenities);
            c.hoursMode = this.hoursMode;
            c.customDayOffset = this.customDayOffset;
            c.customStartMinutes = this.customStartMinutes;
            c.customEndMinutes = this.customEndMinutes;
            c.onlyVerified = this.onlyVerified;
            c.onlyAutoAccept = this.onlyAutoAccept;
            c.minRating = this.minRating;
            c.radiusMeters = this.radiusMeters;
            c.sortBy = this.sortBy;
            return c;
        }

        public int countActiveFilters() {
            int count = 0;
            if (minPrice != null) count++;
            if (maxPrice != null) count++;
            if (minLengthCm > 0 || minWidthCm > 0 || minHeightCm > 0) count++;
            if (!requiredAmenities.isEmpty()) count += requiredAmenities.size();
            if (hoursMode != HoursMode.ANY) count++;
            if (onlyVerified) count++;
            if (onlyAutoAccept) count++;
            if (minRating > 0.0) count++;
            if (Math.abs(radiusMeters - 5000.0) > 1.0) count++;
            return count;
        }

        @Nullable
        public Double getMinPrice() {
            return minPrice;
        }

        public void setMinPrice(@Nullable Double minPrice) {
            this.minPrice = minPrice;
        }

        @Nullable
        public Double getMaxPrice() {
            return maxPrice;
        }

        public void setMaxPrice(@Nullable Double maxPrice) {
            this.maxPrice = maxPrice;
        }

        public int getMinLengthCm() {
            return minLengthCm;
        }

        public void setMinLengthCm(int minLengthCm) {
            this.minLengthCm = Math.max(0, minLengthCm);
        }

        public int getMinWidthCm() {
            return minWidthCm;
        }

        public void setMinWidthCm(int minWidthCm) {
            this.minWidthCm = Math.max(0, minWidthCm);
        }

        public int getMinHeightCm() {
            return minHeightCm;
        }

        public void setMinHeightCm(int minHeightCm) {
            this.minHeightCm = Math.max(0, minHeightCm);
        }

        @NonNull
        public Set<String> getRequiredAmenities() {
            return requiredAmenities;
        }

        public void setRequiredAmenities(@Nullable Set<String> amenities) {
            requiredAmenities.clear();
            if (amenities != null) {
                for (String a : amenities) {
                    String canon = canonicalAmenity(a);
                    if (!canon.isEmpty()) {
                        requiredAmenities.add(canon);
                    }
                }
            }
        }

        @NonNull
        public HoursMode getHoursMode() {
            return hoursMode;
        }

        public void setHoursMode(@Nullable HoursMode hoursMode) {
            this.hoursMode = hoursMode != null ? hoursMode : HoursMode.ANY;
        }

        public int getCustomDayOffset() {
            return customDayOffset;
        }

        public void setCustomDayOffset(int customDayOffset) {
            this.customDayOffset = Math.max(0, customDayOffset);
        }

        public int getCustomStartMinutes() {
            return customStartMinutes;
        }

        public void setCustomStartMinutes(int customStartMinutes) {
            this.customStartMinutes = Math.max(0, Math.min(24 * 60 - 1, customStartMinutes));
        }

        public int getCustomEndMinutes() {
            return customEndMinutes;
        }

        public void setCustomEndMinutes(int customEndMinutes) {
            this.customEndMinutes = Math.max(1, Math.min(24 * 60, customEndMinutes));
        }

        public boolean isOnlyVerified() {
            return onlyVerified;
        }

        public void setOnlyVerified(boolean onlyVerified) {
            this.onlyVerified = onlyVerified;
        }

        public boolean isOnlyAutoAccept() {
            return onlyAutoAccept;
        }

        public void setOnlyAutoAccept(boolean onlyAutoAccept) {
            this.onlyAutoAccept = onlyAutoAccept;
        }

        public double getMinRating() {
            return minRating;
        }

        public void setMinRating(double minRating) {
            this.minRating = Math.max(0.0, minRating);
        }

        public double getRadiusMeters() {
            return radiusMeters;
        }

        public void setRadiusMeters(double radiusMeters) {
            this.radiusMeters = radiusMeters > 0 ? radiusMeters : 5000.0;
        }

        @NonNull
        public SortMode getSortBy() {
            return sortBy;
        }

        public void setSortBy(@Nullable SortMode sortBy) {
            this.sortBy = sortBy != null ? sortBy : SortMode.DISTANCE;
        }
    }

    private static final Map<String, String> AMENITY_CANONICAL_MAP = new HashMap<>();

    static {
        registerAmenity("CCTV", "CCTV", "MONITORING");
        registerAmenity("LIGHTING", "LIGHTING", "OSWIETLENIE");
        registerAmenity("EV_CHARGER", "EV_CHARGER", "EV", "EV CHARGING", "LADOWANIE EV");
        registerAmenity("COVERED", "COVERED", "ZADASZONE");
        registerAmenity("UNDERGROUND", "UNDERGROUND", "PODZIEMNY");
        registerAmenity("GATE", "GATE", "BRAMA", "BRAMA AUTOMATYCZNA");
        registerAmenity("DISABLED_ACCESS", "DISABLED_ACCESS", "NIEPELNOSPRAWNI", "DLA NIEPELNOSPRAWNYCH");
        registerAmenity("WIDE_SPACE", "WIDE_SPACE", "SZEROKIE MIEJSCE");
    }

    private static void registerAmenity(String canonical, String... aliases) {
        for (String alias : aliases) {
            AMENITY_CANONICAL_MAP.put(foldPolish(alias).toUpperCase(Locale.ROOT), canonical);
        }
    }

    private MapSpotFilterHelper() {
    }

    @NonNull
    public static String foldPolish(@Nullable String input) {
        if (input == null) return "";
        String replaced = input
                .replace('\u0142', 'l')
                .replace('\u0141', 'L');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    public static String canonicalAmenity(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String key = foldPolish(raw).toUpperCase(Locale.ROOT);
        String mapped = AMENITY_CANONICAL_MAP.get(key);
        return mapped != null ? mapped : key;
    }

    public static boolean matches(@Nullable ParkingPlaceEntity spot,
                                  @Nullable List<AvailabilityRuleEntity> rules,
                                  @Nullable List<BlockedPeriodEntity> blocks,
                                  @Nullable SpotRepository.SpotMetadata meta,
                                  @NonNull FilterCriteria criteria,
                                  @Nullable LatLng anchorPoint,
                                  @Nullable String cityFilter,
                                  long nowMs) {
        if (spot == null) return false;

        if (criteria.getMinPrice() != null && spot.getPrice() < criteria.getMinPrice()) {
            return false;
        }
        if (criteria.getMaxPrice() != null && spot.getPrice() > criteria.getMaxPrice()) {
            return false;
        }

        if (criteria.getMinLengthCm() > 0 && spot.getLengthCm() < criteria.getMinLengthCm()) {
            return false;
        }
        if (criteria.getMinWidthCm() > 0 && spot.getWidthCm() < criteria.getMinWidthCm()) {
            return false;
        }
        if (criteria.getMinHeightCm() > 0) {
            Integer h = spot.getHeightCm();
            if (h != null && h > 0 && h < criteria.getMinHeightCm()) {
                return false;
            }
        }

        if (!criteria.getRequiredAmenities().isEmpty()
                && !matchesAmenities(spot.getAmenitiesCsv(), criteria.getRequiredAmenities())) {
            return false;
        }

        if (criteria.isOnlyVerified() && (meta == null || !meta.isVerified())) {
            return false;
        }
        if (criteria.isOnlyAutoAccept() && (meta == null || !meta.isAutoAccept())) {
            return false;
        }

        if (criteria.getMinRating() > 0.0 && spot.getRating() < criteria.getMinRating()) {
            return false;
        }

        if (!matchesHoursFilter(rules, blocks, criteria, nowMs)) {
            return false;
        }

        if (cityFilter != null && !cityFilter.trim().isEmpty()) {
            String foldedCity = foldPolish(spot.getCity());
            String foldedFilter = foldPolish(cityFilter);
            if (!foldedCity.contains(foldedFilter)) {
                return false;
            }
        }

        if (anchorPoint != null) {
            double dist = distanceMeters(anchorPoint.latitude, anchorPoint.longitude,
                    spot.getLatitude(), spot.getLongitude());
            if (dist > criteria.getRadiusMeters()) {
                return false;
            }
        }

        return true;
    }

    public static boolean matchesAmenities(@Nullable String amenitiesCsv,
                                           @Nullable Set<String> requiredCanonical) {
        if (requiredCanonical == null || requiredCanonical.isEmpty()) return true;
        if (amenitiesCsv == null || amenitiesCsv.trim().isEmpty()) return false;

        Set<String> present = new HashSet<>();
        String[] parts = amenitiesCsv.split(",");
        for (String p : parts) {
            String canon = canonicalAmenity(p);
            if (!canon.isEmpty()) {
                present.add(canon);
            }
        }
        return present.containsAll(requiredCanonical);
    }

    public static boolean matchesHoursFilter(@Nullable List<AvailabilityRuleEntity> rules,
                                             @Nullable List<BlockedPeriodEntity> blocks,
                                             @NonNull FilterCriteria criteria,
                                             long nowMs) {
        HoursMode mode = criteria.getHoursMode();
        if (mode == HoursMode.ANY) {
            return true;
        }

        boolean hasRules = rules != null && !rules.isEmpty();

        if (mode == HoursMode.OPEN_24_7) {
            if (!hasRules) return true;
            for (int dow = 1; dow <= 7; dow++) {
                if (!coversMinuteRangeForDay(rules, dow, null, 0, 23 * 60 + 58)) {
                    return false;
                }
            }
            return true;
        }

        if (mode == HoursMode.OPEN_NOW) {
            if (!hasRules) return true;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(nowMs);
            int dow = isoDayOfWeek(cal);
            String dateStr = formatIsoDate(cal);
            int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            return coversMinuteRangeForDay(rules, dow, dateStr, nowMin, Math.min(24 * 60, nowMin + 1));
        }

        // CUSTOM_WINDOW
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs);
        cal.add(Calendar.DAY_OF_YEAR, criteria.getCustomDayOffset());
        int targetDow = isoDayOfWeek(cal);
        String targetDateStr = formatIsoDate(cal);

        int reqStart = criteria.getCustomStartMinutes();
        int reqEnd = criteria.getCustomEndMinutes();
        if (reqEnd <= reqStart) {
            reqEnd = Math.min(24 * 60, reqStart + 60);
        }

        if (hasRules && !coversMinuteRangeForDay(rules, targetDow, targetDateStr, reqStart, reqEnd)) {
            return false;
        }

        if (blocks != null && !blocks.isEmpty()) {
            Calendar startCal = (Calendar) cal.clone();
            startCal.set(Calendar.HOUR_OF_DAY, reqStart / 60);
            startCal.set(Calendar.MINUTE, reqStart % 60);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = (Calendar) cal.clone();
            if (reqEnd >= 24 * 60) {
                endCal.set(Calendar.HOUR_OF_DAY, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);
            } else {
                endCal.set(Calendar.HOUR_OF_DAY, reqEnd / 60);
                endCal.set(Calendar.MINUTE, reqEnd % 60);
                endCal.set(Calendar.SECOND, 0);
            }
            long winStart = startCal.getTimeInMillis();
            long winEnd = endCal.getTimeInMillis();

            for (BlockedPeriodEntity b : blocks) {
                if (b == null || b.isExpired()) continue;
                long bStart = b.getStartMillis();
                long bEnd = b.getEndMillis();
                if (bStart > 0 && bEnd > bStart && bStart < winEnd && bEnd > winStart) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean coversMinuteRangeForDay(@NonNull List<AvailabilityRuleEntity> rules,
                                                   int dayOfWeek,
                                                   @Nullable String isoDate,
                                                   int reqStartMin,
                                                   int reqEndMin) {
        List<int[]> intervals = new ArrayList<>();
        for (AvailabilityRuleEntity r : rules) {
            if (r == null || r.getDayOfWeek() != dayOfWeek) continue;
            if (isoDate != null && !isValidOnDate(r, isoDate)) continue;
            int s = parseTimeToMinutes(r.getStartTime(), 0);
            int e = parseTimeToMinutes(r.getEndTime(), 24 * 60);
            if (e >= 23 * 60 + 59) {
                e = 24 * 60;
            }
            if (e > s) {
                intervals.add(new int[]{s, e});
            }
        }
        if (intervals.isEmpty()) return false;

        Collections.sort(intervals, Comparator.comparingInt(a -> a[0]));
        int coveredUntil = -1;
        for (int[] iv : intervals) {
            if (coveredUntil < 0) {
                if (iv[0] > reqStartMin) return false;
                coveredUntil = iv[1];
            } else if (iv[0] <= coveredUntil) {
                coveredUntil = Math.max(coveredUntil, iv[1]);
            } else if (coveredUntil < reqEndMin) {
                return false;
            }
            if (coveredUntil >= reqEndMin) {
                return true;
            }
        }
        return coveredUntil >= reqEndMin;
    }

    private static boolean isValidOnDate(@NonNull AvailabilityRuleEntity r, @NonNull String isoDate) {
        String from = r.getValidFrom();
        if (from != null && !from.trim().isEmpty() && isoDate.compareTo(from.trim()) < 0) {
            return false;
        }
        String to = r.getValidTo();
        return to == null || to.trim().isEmpty() || isoDate.compareTo(to.trim()) <= 0;
    }

    public static int parseTimeToMinutes(@Nullable String timeStr, int fallbackMinutes) {
        if (timeStr == null || !timeStr.contains(":")) return fallbackMinutes;
        try {
            String[] parts = timeStr.trim().split(":");
            int h = Integer.parseInt(parts[0].trim());
            int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return Math.max(0, Math.min(24 * 60, h * 60 + m));
        } catch (Exception e) {
            return fallbackMinutes;
        }
    }

    @NonNull
    public static String formatMinutes(int minutes) {
        int clamped = Math.max(0, Math.min(24 * 60, minutes));
        if (clamped == 24 * 60) return "24:00";
        return String.format(Locale.getDefault(), "%02d:%02d", clamped / 60, clamped % 60);
    }

    @NonNull
    public static String formatWorkingHoursSummary(@Nullable List<AvailabilityRuleEntity> rules, long nowMs) {
        if (rules == null || rules.isEmpty()) {
            return "24/7";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs);
        int dow = isoDayOfWeek(cal);
        String dateStr = formatIsoDate(cal);

        StringBuilder sb = new StringBuilder();
        for (AvailabilityRuleEntity r : rules) {
            if (r == null || r.getDayOfWeek() != dow || !isValidOnDate(r, dateStr)) continue;
            String start = shortenTime(r.getStartTime());
            String end = shortenTime(r.getEndTime());
            if (!start.isEmpty() && !end.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(start).append("-").append(end);
            }
        }
        if (sb.length() == 0) {
            return "Nieczynne dziś";
        }
        return "Dziś: " + sb;
    }

    @NonNull
    private static String shortenTime(@Nullable String time) {
        if (time == null || time.trim().isEmpty()) return "";
        String t = time.trim();
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }

    public static int isoDayOfWeek(@NonNull Calendar cal) {
        int calDow = cal.get(Calendar.DAY_OF_WEEK);
        return calDow == Calendar.SUNDAY ? 7 : calDow - 1;
    }

    @NonNull
    private static String formatIsoDate(@NonNull Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(cal.getTimeInMillis()));
    }

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusMeters = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return earthRadiusMeters * c;
    }

    @NonNull
    public static String formatDistance(double meters) {
        if (meters < 1000.0) {
            return String.format(Locale.getDefault(), "%d m", Math.max(10, Math.round(meters / 10.0) * 10));
        }
        return String.format(Locale.getDefault(), "%.1f km", meters / 1000.0);
    }

    public static void sortSpots(@NonNull List<ParkingPlaceEntity> spots,
                                 @Nullable LatLng referencePoint,
                                 @NonNull SortMode sortMode) {
        Collections.sort(spots, (a, b) -> {
            if (sortMode == SortMode.PRICE_ASC) {
                int cmp = Double.compare(a.getPrice(), b.getPrice());
                if (cmp != 0) return cmp;
            } else if (sortMode == SortMode.RATING_DESC) {
                int cmp = Double.compare(b.getRating(), a.getRating());
                if (cmp != 0) return cmp;
            }
            if (referencePoint != null) {
                double da = distanceMeters(referencePoint.latitude, referencePoint.longitude, a.getLatitude(), a.getLongitude());
                double db = distanceMeters(referencePoint.latitude, referencePoint.longitude, b.getLatitude(), b.getLongitude());
                return Double.compare(da, db);
            }
            return Double.compare(a.getPrice(), b.getPrice());
        });
    }
}
