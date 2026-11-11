package com.example.project.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.project.database.entity.ReservationEntity;
import com.example.project.model.Reservation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Shared filter and sorting helper for both driver History reservations ({@code ReservationsActivity})
 * and owner incoming/guest reservations ({@code OwnerReservationsActivity}).
 */
public final class ReservationFilterHelper {

    public enum DatePreset {
        ALL,
        TODAY,
        UPCOMING,
        LAST_7_DAYS,
        LAST_30_DAYS,
        CUSTOM
    }

    public enum DurationPreset {
        ALL,
        SHORT_LE_2H,
        MEDIUM_2_6H,
        LONG_GT_6H
    }

    public enum HistorySort {
        END_DESC,
        END_ASC,
        PRICE_DESC,
        PRICE_ASC,
        DURATION_DESC,
        DURATION_ASC
    }

    public enum OwnerSort {
        START_DESC,
        START_ASC,
        PRICE_DESC,
        PRICE_ASC
    }

    public static final class HistoryCriteria {
        private String statusFilter = "ALL";
        private DatePreset endDatePreset = DatePreset.ALL;
        @Nullable
        private Long customEndFromMs;
        @Nullable
        private Long customEndToMs;
        private DurationPreset durationPreset = DurationPreset.ALL;
        @Nullable
        private Double minPrice;
        @Nullable
        private Double maxPrice;
        private boolean onlyOverstay = false;
        private String query = "";
        private HistorySort sortBy = HistorySort.END_DESC;

        public void reset() {
            statusFilter = "ALL";
            endDatePreset = DatePreset.ALL;
            customEndFromMs = null;
            customEndToMs = null;
            durationPreset = DurationPreset.ALL;
            minPrice = null;
            maxPrice = null;
            onlyOverstay = false;
            query = "";
            sortBy = HistorySort.END_DESC;
        }

        public int countActiveFilters() {
            int count = 0;
            if (!"ALL".equals(statusFilter)) count++;
            if (endDatePreset != DatePreset.ALL) count++;
            if (durationPreset != DurationPreset.ALL) count++;
            if (minPrice != null || maxPrice != null) count++;
            if (onlyOverstay) count++;
            if (query != null && !query.trim().isEmpty()) count++;
            if (sortBy != HistorySort.END_DESC) count++;
            return count;
        }

        @NonNull
        public String getStatusFilter() {
            return statusFilter;
        }

        public void setStatusFilter(@Nullable String statusFilter) {
            this.statusFilter = statusFilter != null ? statusFilter : "ALL";
        }

        @NonNull
        public DatePreset getEndDatePreset() {
            return endDatePreset;
        }

        public void setEndDatePreset(@Nullable DatePreset endDatePreset) {
            this.endDatePreset = endDatePreset != null ? endDatePreset : DatePreset.ALL;
        }

        @Nullable
        public Long getCustomEndFromMs() {
            return customEndFromMs;
        }

        public void setCustomEndFromMs(@Nullable Long customEndFromMs) {
            this.customEndFromMs = customEndFromMs;
        }

        @Nullable
        public Long getCustomEndToMs() {
            return customEndToMs;
        }

        public void setCustomEndToMs(@Nullable Long customEndToMs) {
            this.customEndToMs = customEndToMs;
        }

        @NonNull
        public DurationPreset getDurationPreset() {
            return durationPreset;
        }

        public void setDurationPreset(@Nullable DurationPreset durationPreset) {
            this.durationPreset = durationPreset != null ? durationPreset : DurationPreset.ALL;
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

        public boolean isOnlyOverstay() {
            return onlyOverstay;
        }

        public void setOnlyOverstay(boolean onlyOverstay) {
            this.onlyOverstay = onlyOverstay;
        }

        @NonNull
        public String getQuery() {
            return query != null ? query : "";
        }

        public void setQuery(@Nullable String query) {
            this.query = query != null ? query.trim() : "";
        }

        @NonNull
        public HistorySort getSortBy() {
            return sortBy;
        }

        public void setSortBy(@Nullable HistorySort sortBy) {
            this.sortBy = sortBy != null ? sortBy : HistorySort.END_DESC;
        }
    }

    public static final class OwnerCriteria {
        private String statusFilter = "ALL";
        @Nullable
        private String spotKeyFilter;
        private DatePreset datePreset = DatePreset.ALL;
        @Nullable
        private Long customFromMs;
        @Nullable
        private Long customToMs;
        @Nullable
        private Double minPrice;
        @Nullable
        private Double maxPrice;
        private String query = "";
        private OwnerSort sortBy = OwnerSort.START_DESC;

        public void resetKeepStatus() {
            spotKeyFilter = null;
            datePreset = DatePreset.ALL;
            customFromMs = null;
            customToMs = null;
            minPrice = null;
            maxPrice = null;
            query = "";
            sortBy = OwnerSort.START_DESC;
        }

        public void resetAll() {
            statusFilter = "ALL";
            resetKeepStatus();
        }

        public int countExtraFilters() {
            int count = 0;
            if (spotKeyFilter != null && !spotKeyFilter.trim().isEmpty()) count++;
            if (datePreset != DatePreset.ALL) count++;
            if (minPrice != null || maxPrice != null) count++;
            if (query != null && !query.trim().isEmpty()) count++;
            if (sortBy != OwnerSort.START_DESC) count++;
            return count;
        }

        @NonNull
        public String getStatusFilter() {
            return statusFilter;
        }

        public void setStatusFilter(@Nullable String statusFilter) {
            this.statusFilter = statusFilter != null ? statusFilter : "ALL";
        }

        @Nullable
        public String getSpotKeyFilter() {
            return spotKeyFilter;
        }

        public void setSpotKeyFilter(@Nullable String spotKeyFilter) {
            this.spotKeyFilter = (spotKeyFilter == null || spotKeyFilter.trim().isEmpty()) ? null : spotKeyFilter.trim();
        }

        @NonNull
        public DatePreset getDatePreset() {
            return datePreset;
        }

        public void setDatePreset(@Nullable DatePreset datePreset) {
            this.datePreset = datePreset != null ? datePreset : DatePreset.ALL;
        }

        @Nullable
        public Long getCustomFromMs() {
            return customFromMs;
        }

        public void setCustomFromMs(@Nullable Long customFromMs) {
            this.customFromMs = customFromMs;
        }

        @Nullable
        public Long getCustomToMs() {
            return customToMs;
        }

        public void setCustomToMs(@Nullable Long customToMs) {
            this.customToMs = customToMs;
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

        @NonNull
        public String getQuery() {
            return query != null ? query : "";
        }

        public void setQuery(@Nullable String query) {
            this.query = query != null ? query.trim() : "";
        }

        @NonNull
        public OwnerSort getSortBy() {
            return sortBy;
        }

        public void setSortBy(@Nullable OwnerSort sortBy) {
            this.sortBy = sortBy != null ? sortBy : OwnerSort.START_DESC;
        }
    }

    private ReservationFilterHelper() {
    }

    @NonNull
    public static List<Reservation> filterAndSortHistory(@Nullable List<Reservation> source,
                                                         @NonNull HistoryCriteria criteria,
                                                         long nowMs) {
        List<Reservation> result = new ArrayList<>();
        if (source == null) return result;

        String foldedQuery = MapSpotFilterHelper.foldPolish(criteria.getQuery());

        for (Reservation r : source) {
            if (r == null) continue;

            if (!matchesHistoryStatus(r.getStatus(), criteria.getStatusFilter())) {
                continue;
            }

            long endMs = effectiveEndMs(r);
            if (!matchesDatePreset(endMs, criteria.getEndDatePreset(),
                    criteria.getCustomEndFromMs(), criteria.getCustomEndToMs(), nowMs)) {
                continue;
            }

            long durMs = computeDurationMs(r.getStartTimeMs(), r.getEndTimeMs(), r.getTimeRange());
            if (!matchesDurationPreset(durMs, criteria.getDurationPreset())) {
                continue;
            }

            if (criteria.getMinPrice() != null && r.getTotalPrice() < criteria.getMinPrice()) {
                continue;
            }
            if (criteria.getMaxPrice() != null && r.getTotalPrice() > criteria.getMaxPrice()) {
                continue;
            }
            if (criteria.isOnlyOverstay() && !r.isOverstayApplied()) {
                continue;
            }

            if (!foldedQuery.isEmpty() && !matchesQuery(foldedQuery,
                    r.getPlaceName(), r.getStreet(), r.getCity(), r.getVehicleInfo(), r.getCarPlate())) {
                continue;
            }

            result.add(r);
        }

        Collections.sort(result, historyComparator(criteria.getSortBy()));
        return result;
    }

    @NonNull
    public static List<ReservationEntity> filterAndSortOwner(@Nullable List<ReservationEntity> source,
                                                             @NonNull OwnerCriteria criteria,
                                                             long nowMs) {
        List<ReservationEntity> result = new ArrayList<>();
        if (source == null) return result;

        String foldedQuery = MapSpotFilterHelper.foldPolish(criteria.getQuery());

        for (ReservationEntity e : source) {
            if (e == null) continue;

            if (!matchesOwnerStatus(e.getStatus(), criteria.getStatusFilter())) {
                continue;
            }

            if (criteria.getSpotKeyFilter() != null) {
                String spotName = e.getPlaceName() != null ? e.getPlaceName().trim() : "";
                String spotId = e.getPlaceId() != null ? e.getPlaceId().trim() : "";
                if (!criteria.getSpotKeyFilter().equals(spotId) && !criteria.getSpotKeyFilter().equals(spotName)) {
                    continue;
                }
            }

            long startMs = e.getStartTimeMs() > 0 ? e.getStartTimeMs() : e.getEndTimeMs();
            if (!matchesDatePreset(startMs, criteria.getDatePreset(),
                    criteria.getCustomFromMs(), criteria.getCustomToMs(), nowMs)) {
                continue;
            }

            if (criteria.getMinPrice() != null && e.getTotalPrice() < criteria.getMinPrice()) {
                continue;
            }
            if (criteria.getMaxPrice() != null && e.getTotalPrice() > criteria.getMaxPrice()) {
                continue;
            }

            if (!foldedQuery.isEmpty() && !matchesQuery(foldedQuery,
                    e.getUserName(), e.getCarPlate(), e.getVehicleInfo(),
                    e.getPlaceName(), e.getStreet(), e.getCity())) {
                continue;
            }

            result.add(e);
        }

        Collections.sort(result, ownerComparator(criteria.getSortBy()));
        return result;
    }

    private static Comparator<Reservation> historyComparator(@NonNull HistorySort sort) {
        return (a, b) -> {
            switch (sort) {
                case END_ASC:
                    return Long.compare(effectiveEndMs(a), effectiveEndMs(b));
                case PRICE_DESC: {
                    int c = Double.compare(b.getTotalPrice(), a.getTotalPrice());
                    return c != 0 ? c : Long.compare(effectiveEndMs(b), effectiveEndMs(a));
                }
                case PRICE_ASC: {
                    int c = Double.compare(a.getTotalPrice(), b.getTotalPrice());
                    return c != 0 ? c : Long.compare(effectiveEndMs(b), effectiveEndMs(a));
                }
                case DURATION_DESC: {
                    long da = computeDurationMs(a.getStartTimeMs(), a.getEndTimeMs(), a.getTimeRange());
                    long db = computeDurationMs(b.getStartTimeMs(), b.getEndTimeMs(), b.getTimeRange());
                    int c = Long.compare(db, da);
                    return c != 0 ? c : Long.compare(effectiveEndMs(b), effectiveEndMs(a));
                }
                case DURATION_ASC: {
                    long da = computeDurationMs(a.getStartTimeMs(), a.getEndTimeMs(), a.getTimeRange());
                    long db = computeDurationMs(b.getStartTimeMs(), b.getEndTimeMs(), b.getTimeRange());
                    int c = Long.compare(da, db);
                    return c != 0 ? c : Long.compare(effectiveEndMs(b), effectiveEndMs(a));
                }
                case END_DESC:
                default:
                    return Long.compare(effectiveEndMs(b), effectiveEndMs(a));
            }
        };
    }

    private static Comparator<ReservationEntity> ownerComparator(@NonNull OwnerSort sort) {
        return (a, b) -> {
            switch (sort) {
                case START_ASC:
                    return Long.compare(a.getStartTimeMs(), b.getStartTimeMs());
                case PRICE_DESC: {
                    int c = Double.compare(b.getTotalPrice(), a.getTotalPrice());
                    return c != 0 ? c : Long.compare(b.getStartTimeMs(), a.getStartTimeMs());
                }
                case PRICE_ASC: {
                    int c = Double.compare(a.getTotalPrice(), b.getTotalPrice());
                    return c != 0 ? c : Long.compare(b.getStartTimeMs(), a.getStartTimeMs());
                }
                case START_DESC:
                default:
                    return Long.compare(b.getStartTimeMs(), a.getStartTimeMs());
            }
        };
    }

    private static boolean matchesHistoryStatus(@Nullable String rawStatus, @NonNull String filter) {
        if ("ALL".equals(filter)) return true;
        String s = rawStatus != null ? rawStatus.toUpperCase(Locale.ROOT) : "";
        switch (filter) {
            case "COMPLETED":
                return s.contains("COMPLET") || s.contains("ZAKOŃ") || s.contains("ZAKON");
            case "CANCELLED":
                return s.contains("CANCEL") || s.contains("ANUL");
            case "REJECTED":
                return s.contains("REJECT") || s.contains("ODRZU");
            case "EXPIRED":
                return s.contains("EXPIRE") || s.contains("WYGAS");
            case "NO_SHOW":
                return s.contains("NO_SHOW");
            default:
                return true;
        }
    }

    private static boolean matchesOwnerStatus(@Nullable String rawStatus, @NonNull String filter) {
        if ("ALL".equals(filter)) return true;
        String s = rawStatus != null ? rawStatus.toUpperCase(Locale.ROOT) : "";
        switch (filter) {
            case "PENDING":
                return "PENDING_APPROVAL".equals(s) || "PENDING_PAYMENT".equals(s);
            case "CONFIRMED":
                return "CONFIRMED".equals(s) || "AKTYWNA".equals(s);
            case "IN_PROGRESS":
                return "IN_PROGRESS".equals(s);
            case "COMPLETED":
                return s.contains("COMPLET") || s.contains("ZAKOŃ") || s.contains("ZAKON");
            case "CANCELLED_REJECTED":
                return s.contains("CANCEL") || s.contains("ANUL") || s.contains("REJECT") || s.contains("ODRZU");
            case "EXPIRED_NO_SHOW":
                return s.contains("EXPIRE") || s.contains("WYGAS") || s.contains("NO_SHOW");
            default:
                return true;
        }
    }

    private static boolean matchesDatePreset(long targetMs,
                                             @NonNull DatePreset preset,
                                             @Nullable Long customFromMs,
                                             @Nullable Long customToMs,
                                             long nowMs) {
        if (preset == DatePreset.ALL) return true;
        if (targetMs <= 0) return false;

        switch (preset) {
            case TODAY: {
                long dayStart = startOfDayMs(nowMs);
                long dayEnd = endOfDayMs(nowMs);
                return targetMs >= dayStart && targetMs <= dayEnd;
            }
            case UPCOMING:
                return targetMs >= nowMs;
            case LAST_7_DAYS:
                return targetMs >= (startOfDayMs(nowMs) - 6L * 86_400_000L) && targetMs <= endOfDayMs(nowMs);
            case LAST_30_DAYS:
                return targetMs >= (startOfDayMs(nowMs) - 29L * 86_400_000L) && targetMs <= endOfDayMs(nowMs);
            case CUSTOM: {
                if (customFromMs != null && targetMs < startOfDayMs(customFromMs)) return false;
                if (customToMs != null && targetMs > endOfDayMs(customToMs)) return false;
                return true;
            }
            default:
                return true;
        }
    }

    private static boolean matchesDurationPreset(long durMs, @NonNull DurationPreset preset) {
        if (preset == DurationPreset.ALL) return true;
        if (durMs <= 0) return false;
        long twoHours = 2L * 3_600_000L;
        long sixHours = 6L * 3_600_000L;
        switch (preset) {
            case SHORT_LE_2H:
                return durMs <= twoHours;
            case MEDIUM_2_6H:
                return durMs > twoHours && durMs <= sixHours;
            case LONG_GT_6H:
                return durMs > sixHours;
            default:
                return true;
        }
    }

    public static long computeDurationMs(long startMs, long endMs, @Nullable String timeRange) {
        if (startMs > 0 && endMs > startMs) {
            return endMs - startMs;
        }
        if (timeRange != null && timeRange.contains("-")) {
            String[] parts = timeRange.split("-");
            if (parts.length == 2) {
                int s = MapSpotFilterHelper.parseTimeToMinutes(parts[0], -1);
                int e = MapSpotFilterHelper.parseTimeToMinutes(parts[1], -1);
                if (s >= 0 && e > s) {
                    return (long) (e - s) * 60_000L;
                }
            }
        }
        return 0L;
    }

    private static long effectiveEndMs(@NonNull Reservation r) {
        return r.getEndTimeMs() > 0 ? r.getEndTimeMs() : r.getStartTimeMs();
    }

    private static boolean matchesQuery(@NonNull String foldedQuery, @Nullable String... fields) {
        if (fields == null) return false;
        for (String field : fields) {
            if (field != null && MapSpotFilterHelper.foldPolish(field).contains(foldedQuery)) {
                return true;
            }
        }
        return false;
    }

    public static long startOfDayMs(long tsMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tsMs);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long endOfDayMs(long tsMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tsMs);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
