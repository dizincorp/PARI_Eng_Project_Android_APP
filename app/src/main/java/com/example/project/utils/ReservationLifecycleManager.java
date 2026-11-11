package com.example.project.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.project.R;
import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.dao.ReservationDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.ReservationEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.CheckOutResponse;
import com.example.project.network.model.ReservationResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.ReservationRepository;
import com.example.project.repository.SpotRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates client-side reservation lifecycle automation without backend modifications:
 * <ul>
 *     <li>Transitions stalled {@code PENDING_APPROVAL}/{@code PENDING_PAYMENT} reservations past end time to {@code EXPIRED}.</li>
 *     <li>Transitions missed {@code CONFIRMED} reservations past end time without check-in to {@code NO_SHOW}.</li>
 *     <li>Performs automatic GPS coordinate-based check-in when the driver arrives at their reserved spot.</li>
 *     <li>Grants a 1-hour grace window on missed check-out with an urgent notification, hourly + 15% surcharge,
 *     and automatic check-out completion once the grace hour expires.</li>
 * </ul>
 */
public class ReservationLifecycleManager {

    private static final String TAG = "ReservationLifecycleMgr";
    private static final String PREFS_NAME = "reservation_lifecycle_prefs";

    public static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    public static final long EARLY_CHECK_IN_MS = 5L * 60L * 1000L;
    public static final float AUTO_CHECKIN_RADIUS_METERS = 60.0f;

    private static final long PERIODIC_INTERVAL_MS = 5000L;
    private static final long RETRY_COOLDOWN_MS = 30_000L;
    private static final long SPOT_FETCH_COOLDOWN_MS = 60_000L;
    private static final long LOCATION_STALE_MS = 10L * 60L * 1000L;

    private static final String KEY_AUTO_CHECKED_IN = "auto_checked_in_";
    private static final String KEY_URGENT_NOTIFIED = "urgent_notified_";
    private static final String KEY_OVERSTAY_APPLIED = "overstay_applied_";
    private static final String KEY_PENALIZED_PRICE = "penalized_price_";
    private static final String KEY_MANUAL_CHECKOUT = "manual_checkout_";
    private static final String KEY_AUTO_CHECKOUT_DONE = "auto_checkout_done_";

    private static ReservationLifecycleManager instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final ReservationDao reservationDao;
    private final ParkingPlaceDao parkingPlaceDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Set<String> checkInInFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> checkOutInFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastCheckInAttemptMs = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCheckOutAttemptMs = new ConcurrentHashMap<>();

    private volatile long lastSpotFetchAttemptMs = 0L;
    private volatile Location lastDriverLocation;
    private int foregroundActivityCount = 0;
    private boolean isLocationListening = false;

    private final Runnable periodicRunnable = new Runnable() {
        @Override
        public void run() {
            evaluateAsync();
            if (foregroundActivityCount > 0) {
                mainHandler.postDelayed(this, PERIODIC_INTERVAL_MS);
            }
        }
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            lastDriverLocation = location;
            evaluateAsync();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    };

    private ReservationLifecycleManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.reservationDao = db.reservationDao();
        this.parkingPlaceDao = db.parkingPlaceDao();
    }

    public static synchronized ReservationLifecycleManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ReservationLifecycleManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Resolves effective status on the client for time-elapsed reservations
     * when the backend still reports PENDING_APPROVAL/PENDING_PAYMENT or CONFIRMED.
     */
    @NonNull
    public static String computeEffectiveStatus(@Nullable String rawStatus, long endTimeMs, long now) {
        String status = rawStatus != null ? rawStatus.trim() : "";
        String upper = status.toUpperCase(Locale.ROOT);
        if (endTimeMs > 0 && now >= endTimeMs) {
            if (upper.contains("PENDING_APPROVAL") || upper.contains("PENDING_PAYMENT")) {
                return "EXPIRED";
            }
            if (upper.contains("CONFIRMED") || upper.contains("AKTYWNA") || upper.equals("ACTIVE")) {
                return "NO_SHOW";
            }
        }
        return status;
    }

    /**
     * Determines whether a reservation belongs in the History tab at {@code now}.
     */
    public static boolean isHistoryStatus(@Nullable String rawStatus, long endTimeMs, long now) {
        String effective = computeEffectiveStatus(rawStatus, endTimeMs, now).toUpperCase(Locale.ROOT);
        if (effective.contains("COMPLET")
                || effective.contains("ZAKOŃ")
                || effective.contains("ZAKON")
                || effective.contains("CANCEL")
                || effective.contains("ANUL")
                || effective.contains("REJECT")
                || effective.contains("ODRZU")
                || effective.contains("EXPIRE")
                || effective.contains("WYGAS")
                || effective.contains("NO_SHOW")) {
            return true;
        }
        if (effective.contains("IN_PROGRESS")) {
            return endTimeMs > 0 && now >= (endTimeMs + ONE_HOUR_MS);
        }
        return endTimeMs > 0 && now >= endTimeMs;
    }

    public synchronized boolean isOverstayApplied(@Nullable String reservationId) {
        if (reservationId == null || reservationId.isEmpty()) return false;
        return prefs.getBoolean(KEY_OVERSTAY_APPLIED + reservationId, false);
    }

    public synchronized double getStoredPenalizedPrice(@Nullable String reservationId, double fallback) {
        if (reservationId == null || reservationId.isEmpty()) return fallback;
        String stored = prefs.getString(KEY_PENALIZED_PRICE + reservationId, null);
        if (stored == null) return fallback;
        try {
            return Double.parseDouble(stored);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public synchronized void recordManualCheckOut(@Nullable String reservationId) {
        if (reservationId == null || reservationId.isEmpty()) return;
        prefs.edit().putBoolean(KEY_MANUAL_CHECKOUT + reservationId, true).apply();
        cancelAlarms(reservationId);
    }

    /**
     * Applies client-side expiration, no-show, and overstay surcharge rules onto an entity
     * (safe to call repeatedly during network mapping without double-charging).
     */
    public void applyLifecycleToEntity(@NonNull ReservationEntity entity, @Nullable ParkingPlaceEntity place) {
        if (entity.isOwnerIncoming()) {
            long now = System.currentTimeMillis();
            String upper = entity.getStatus() != null ? entity.getStatus().toUpperCase(Locale.ROOT) : "";
            if ((upper.contains("PENDING_APPROVAL") || upper.contains("PENDING_PAYMENT"))
                    && entity.getEndTimeMs() > 0 && now >= entity.getEndTimeMs()) {
                entity.setStatus("EXPIRED");
            }
            return;
        }

        long now = System.currentTimeMillis();
        String id = entity.getId();
        String effective = computeEffectiveStatus(entity.getStatus(), entity.getEndTimeMs(), now);
        entity.setStatus(effective);

        String upper = effective.toUpperCase(Locale.ROOT);
        if (upper.contains("IN_PROGRESS") && entity.getEndTimeMs() > 0 && now >= entity.getEndTimeMs()) {
            applyOverstaySurchargeLocked(entity, place);
        } else if ((upper.contains("COMPLET") || upper.contains("ZAKOŃ") || upper.contains("ZAKON"))
                && isOverstayApplied(id)) {
            double penalized = getStoredPenalizedPrice(id, entity.getTotalPrice());
            entity.setTotalPrice(Math.max(entity.getTotalPrice(), penalized));
            entity.setOverstayApplied(true);
        }
    }

    /**
     * Starts foreground location and lifecycle polling (called from {@code BaseActivity.onResume}).
     */
    public void startForegroundMonitoring() {
        mainHandler.post(() -> {
            foregroundActivityCount++;
            if (foregroundActivityCount == 1) {
                startLocationUpdates();
                mainHandler.removeCallbacks(periodicRunnable);
                mainHandler.post(periodicRunnable);
            } else {
                startLocationUpdates();
                evaluateAsync();
            }
        });
    }

    /**
     * Stops foreground location and lifecycle polling when no activities remain resumed.
     */
    public void stopForegroundMonitoring() {
        mainHandler.post(() -> {
            foregroundActivityCount = Math.max(0, foregroundActivityCount - 1);
            if (foregroundActivityCount == 0) {
                mainHandler.removeCallbacks(periodicRunnable);
                stopLocationUpdates();
            }
        });
    }

    public void evaluateAsync() {
        executor.execute(() -> evaluateInternal(null));
    }

    /**
     * Synchronous evaluation entry point used by {@link ReservationAlarmReceiver}.
     * Waits briefly for any triggered network check-out call before releasing the receiver.
     */
    public void evaluateFromAlarmSync() {
        CountDownLatch pendingNetworkLatch = new CountDownLatch(1);
        boolean startedAsyncCall = evaluateInternal(pendingNetworkLatch);
        if (startedAsyncCall) {
            try {
                pendingNetworkLatch.await(7, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean evaluateInternal(@Nullable CountDownLatch networkCompletionLatch) {
        boolean dispatchedNetworkCall = false;
        try {
            SessionManager sm = SessionManager.getInstance(appContext);
            String userEmail = (sm != null && sm.isLoggedIn()) ? sm.getUserEmail() : null;
            if (userEmail == null || userEmail.trim().isEmpty()) {
                userEmail = AppRepository.getInstance().getUserEmailLiveData().getValue();
            }
            if (userEmail == null || userEmail.trim().isEmpty()) {
                userEmail = sm != null ? sm.getUserEmail() : "";
            }
            userEmail = userEmail != null ? userEmail.trim() : "";
            long now = System.currentTimeMillis();

            // Also expire stale owner-incoming pending requests locally
            List<ReservationEntity> ownerList = reservationDao.getOwnerIncomingReservationsSync(userEmail);
            if (ownerList != null) {
                for (ReservationEntity ownerRes : ownerList) {
                    if (ownerRes == null) continue;
                    String st = ownerRes.getStatus() != null ? ownerRes.getStatus().toUpperCase(Locale.ROOT) : "";
                    if ((st.contains("PENDING_APPROVAL") || st.contains("PENDING_PAYMENT"))
                            && ownerRes.getEndTimeMs() > 0 && now >= ownerRes.getEndTimeMs()
                            && !"EXPIRED".equalsIgnoreCase(st)) {
                        ownerRes.setStatus("EXPIRED");
                        reservationDao.insertOrUpdate(ownerRes);
                    }
                }
            }

            List<ReservationEntity> driverList = reservationDao.getDriverReservationsSync(userEmail);
            if (driverList == null || driverList.isEmpty()) {
                return false;
            }

            boolean spotFetchAttemptedInPass = false;

            for (ReservationEntity entity : driverList) {
                if (entity == null || entity.isOwnerIncoming()) continue;

                String id = entity.getId();
                String origStatus = entity.getStatus() != null ? entity.getStatus().trim() : "";
                String upper = origStatus.toUpperCase(Locale.ROOT);
                long startMs = entity.getStartTimeMs();
                long endMs = entity.getEndTimeMs();
                ParkingPlaceEntity place = entity.getPlaceId() != null ? parkingPlaceDao.getPlaceById(entity.getPlaceId()) : null;
                String spotTitle = resolveSpotTitle(entity, place);

                // 1. Stalled PENDING_APPROVAL / PENDING_PAYMENT past end time -> EXPIRED
                if ((upper.contains("PENDING_APPROVAL") || upper.contains("PENDING_PAYMENT"))
                        && endMs > 0 && now >= endMs) {
                    if (!"EXPIRED".equalsIgnoreCase(origStatus)) {
                        entity.setStatus("EXPIRED");
                        reservationDao.insertOrUpdate(entity);
                        releaseSpotOccupancyIfIdle(place);
                    }
                    cancelAlarms(id);
                    continue;
                }

                // 2. Missed CONFIRMED / AKTYWNA / ACTIVE past end time without check-in -> NO_SHOW
                if ((upper.contains("CONFIRMED") || upper.contains("AKTYWNA") || upper.equals("ACTIVE"))
                        && endMs > 0 && now >= endMs) {
                    if (!"NO_SHOW".equalsIgnoreCase(origStatus)) {
                        entity.setStatus("NO_SHOW");
                        reservationDao.insertOrUpdate(entity);
                        releaseSpotOccupancyIfIdle(place);
                    }
                    cancelAlarms(id);
                    continue;
                }

                // 3. Auto Check-In via GPS for CONFIRMED / AKTYWNA / ACTIVE inside check-in window
                if (upper.contains("CONFIRMED") || upper.contains("AKTYWNA") || upper.equals("ACTIVE")) {
                    boolean inWindow = (startMs == 0 || now >= (startMs - EARLY_CHECK_IN_MS))
                            && (endMs == 0 || now < endMs);
                    if (inWindow) {
                        Location driverLoc = getBestCurrentLocation();
                        if (place == null || (place.getLatitude() == 0.0 && place.getLongitude() == 0.0)) {
                            if (!spotFetchAttemptedInPass && driverLoc != null) {
                                spotFetchAttemptedInPass = true;
                                maybeFetchSpotsNear(driverLoc);
                            }
                        } else if (driverLoc != null && isDriverAtSpot(driverLoc, place)) {
                            if (shouldAttemptAutoCheckIn(id, now)) {
                                performAutoCheckIn(entity, spotTitle, userEmail);
                            }
                        }
                    }
                    continue;
                }

                // 4. IN_PROGRESS: +1h Grace Period, Urgent Notification, Hourly + 15% Surcharge, Auto Check-Out
                if (upper.contains("IN_PROGRESS") && endMs > 0) {
                    long graceEndMs = endMs + ONE_HOUR_MS;
                    if (now < endMs) {
                        scheduleAlarm(id, ReservationAlarmReceiver.ACTION_URGENT_CHECKOUT, endMs);
                        scheduleAlarm(id, ReservationAlarmReceiver.ACTION_AUTO_CHECKOUT, graceEndMs);
                    } else {
                        boolean changed = applyOverstaySurchargeLocked(entity, place);
                        if (changed) {
                            reservationDao.insertOrUpdate(entity);
                        }

                        if (!prefs.getBoolean(KEY_URGENT_NOTIFIED + id, false)) {
                            prefs.edit().putBoolean(KEY_URGENT_NOTIFIED + id, true).apply();
                            NotificationHelper.showNotification(
                                    appContext,
                                    appContext.getString(R.string.notif_urgent_checkout_title),
                                    appContext.getString(R.string.notif_urgent_checkout_msg, spotTitle),
                                    true,
                                    "urgent_checkout_" + id
                            );
                        }

                        if (now < graceEndMs) {
                            scheduleAlarm(id, ReservationAlarmReceiver.ACTION_AUTO_CHECKOUT, graceEndMs);
                        } else if (shouldAttemptAutoCheckOut(id, now)) {
                            dispatchedNetworkCall = true;
                            performAutoCheckOut(entity, place, spotTitle, userEmail, networkCompletionLatch);
                        }
                    }
                } else {
                    cancelAlarms(id);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during reservation lifecycle evaluation", e);
        }
        return dispatchedNetworkCall;
    }

    private synchronized boolean applyOverstaySurchargeLocked(@NonNull ReservationEntity entity,
                                                              @Nullable ParkingPlaceEntity place) {
        String id = entity.getId();
        boolean alreadyApplied = prefs.getBoolean(KEY_OVERSTAY_APPLIED + id, false);
        if (alreadyApplied) {
            double penalized = getStoredPenalizedPrice(id, entity.getTotalPrice());
            boolean needsUpdate = !entity.isOverstayApplied() || entity.getTotalPrice() + 0.005 < penalized;
            if (needsUpdate) {
                entity.setTotalPrice(Math.max(entity.getTotalPrice(), penalized));
                entity.setOverstayApplied(true);
            }
            return needsUpdate;
        }

        double basePrice = Math.max(0.0, entity.getTotalPrice());
        double rate = entity.getHourlyRate() > 0.0
                ? entity.getHourlyRate()
                : (place != null && place.getPrice() > 0.0 ? place.getPrice() : 0.0);

        if (rate <= 0.0 && entity.getStartTimeMs() > 0 && entity.getEndTimeMs() > entity.getStartTimeMs()) {
            double durationHours = Math.max(1.0, (entity.getEndTimeMs() - entity.getStartTimeMs()) / 3600000.0);
            rate = basePrice / durationHours;
        }

        double surcharge = round2(rate * 1.15);
        double penalizedTotal = round2(basePrice + surcharge);

        prefs.edit()
                .putBoolean(KEY_OVERSTAY_APPLIED + id, true)
                .putString(KEY_PENALIZED_PRICE + id, String.format(Locale.US, "%.2f", penalizedTotal))
                .apply();

        entity.setTotalPrice(penalizedTotal);
        entity.setOverstayApplied(true);
        return true;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private boolean isDriverAtSpot(@NonNull Location driverLoc, @NonNull ParkingPlaceEntity place) {
        if (driverLoc.hasAccuracy() && driverLoc.getAccuracy() > 100.0f) {
            return false;
        }
        float[] distanceMeters = new float[1];
        Location.distanceBetween(
                driverLoc.getLatitude(),
                driverLoc.getLongitude(),
                place.getLatitude(),
                place.getLongitude(),
                distanceMeters
        );
        return distanceMeters[0] <= AUTO_CHECKIN_RADIUS_METERS;
    }

    private boolean shouldAttemptAutoCheckIn(@NonNull String id, long now) {
        if (prefs.getBoolean(KEY_AUTO_CHECKED_IN + id, false)) return false;
        if (checkInInFlight.contains(id)) return false;
        Long lastAttempt = lastCheckInAttemptMs.get(id);
        return lastAttempt == null || (now - lastAttempt) >= RETRY_COOLDOWN_MS;
    }

    private boolean shouldAttemptAutoCheckOut(@NonNull String id, long now) {
        if (prefs.getBoolean(KEY_MANUAL_CHECKOUT + id, false)) return false;
        if (prefs.getBoolean(KEY_AUTO_CHECKOUT_DONE + id, false)) return false;
        if (checkOutInFlight.contains(id)) return false;
        Long lastAttempt = lastCheckOutAttemptMs.get(id);
        return lastAttempt == null || (now - lastAttempt) >= RETRY_COOLDOWN_MS;
    }

    private void performAutoCheckIn(@NonNull ReservationEntity entity,
                                    @NonNull String spotTitle,
                                    @NonNull String userEmail) {
        final String id = entity.getId();
        final String pin = entity.getPinCode();
        final String qr = entity.getQrToken();
        final long endMs = entity.getEndTimeMs();

        checkInInFlight.add(id);
        lastCheckInAttemptMs.put(id, System.currentTimeMillis());

        mainHandler.post(() -> {
            ReservationRepository repo = ReservationRepository.getInstance(appContext);
            LiveData<Resource<ReservationResponse>> live = repo.checkInReservation(id, pin, qr, userEmail);
            live.observeForever(new Observer<Resource<ReservationResponse>>() {
                @Override
                public void onChanged(Resource<ReservationResponse> resource) {
                    if (resource == null || resource.isLoading()) return;
                    live.removeObserver(this);
                    checkInInFlight.remove(id);

                    if (resource.isSuccess()) {
                        prefs.edit().putBoolean(KEY_AUTO_CHECKED_IN + id, true).apply();
                        NotificationHelper.showNotification(
                                appContext,
                                appContext.getString(R.string.notif_auto_checkin_title),
                                appContext.getString(R.string.notif_auto_checkin_msg, spotTitle),
                                false,
                                "auto_checkin_" + id
                        );
                        if (endMs > 0) {
                            scheduleAlarm(id, ReservationAlarmReceiver.ACTION_URGENT_CHECKOUT, endMs);
                            scheduleAlarm(id, ReservationAlarmReceiver.ACTION_AUTO_CHECKOUT, endMs + ONE_HOUR_MS);
                        }
                    } else if (resource.getCode() == 409) {
                        // Already checked in on server; refresh reservations
                        repo.fetchReservations(userEmail);
                    }
                }
            });
        });
    }

    private void performAutoCheckOut(@NonNull ReservationEntity entity,
                                     @Nullable ParkingPlaceEntity place,
                                     @NonNull String spotTitle,
                                     @NonNull String userEmail,
                                     @Nullable CountDownLatch completionLatch) {
        final String id = entity.getId();
        checkOutInFlight.add(id);
        lastCheckOutAttemptMs.put(id, System.currentTimeMillis());

        mainHandler.post(() -> {
            ReservationRepository repo = ReservationRepository.getInstance(appContext);
            LiveData<Resource<CheckOutResponse>> live = repo.checkOutReservation(id, userEmail);
            live.observeForever(new Observer<Resource<CheckOutResponse>>() {
                @Override
                public void onChanged(Resource<CheckOutResponse> resource) {
                    if (resource == null || resource.isLoading()) return;
                    live.removeObserver(this);
                    checkOutInFlight.remove(id);

                    if (resource.isSuccess()) {
                        finishAutoCheckOutSuccess(id, spotTitle, repo, userEmail);
                        if (completionLatch != null) completionLatch.countDown();
                    } else if (resource.getCode() == 400 || resource.getCode() == 409) {
                        // Backend overdue job may have already auto-completed this session; reconcile with server
                        LiveData<Resource<ReservationResponse>> checkLive = repo.fetchReservationById(id, userEmail);
                        checkLive.observeForever(new Observer<Resource<ReservationResponse>>() {
                            @Override
                            public void onChanged(Resource<ReservationResponse> res) {
                                if (res == null || res.isLoading()) return;
                                checkLive.removeObserver(this);
                                if (res.isSuccess() && res.getData() != null) {
                                    String serverSt = res.getData().getStatus() != null
                                            ? res.getData().getStatus().toUpperCase(Locale.ROOT) : "";
                                    if (!serverSt.contains("IN_PROGRESS")) {
                                        finishAutoCheckOutSuccess(id, spotTitle, repo, userEmail);
                                    }
                                }
                                if (completionLatch != null) completionLatch.countDown();
                            }
                        });
                    } else {
                        if (completionLatch != null) completionLatch.countDown();
                    }
                }
            });
        });
    }

    private void finishAutoCheckOutSuccess(@NonNull String id,
                                           @NonNull String spotTitle,
                                           @NonNull ReservationRepository repo,
                                           @NonNull String userEmail) {
        prefs.edit().putBoolean(KEY_AUTO_CHECKOUT_DONE + id, true).apply();
        cancelAlarms(id);
        NotificationHelper.showNotification(
                appContext,
                appContext.getString(R.string.notif_auto_checkout_title),
                appContext.getString(R.string.notif_auto_checkout_msg, spotTitle),
                false,
                "auto_checkout_" + id
        );
        repo.fetchReservations(userEmail);
    }

    private void maybeFetchSpotsNear(@NonNull Location loc) {
        long now = System.currentTimeMillis();
        if (now - lastSpotFetchAttemptMs < SPOT_FETCH_COOLDOWN_MS) return;
        lastSpotFetchAttemptMs = now;
        mainHandler.post(() -> SpotRepository.getInstance(appContext)
                .fetchAndSaveSpots(loc.getLatitude(), loc.getLongitude(), 50000.0));
    }

    private void releaseSpotOccupancyIfIdle(@Nullable ParkingPlaceEntity place) {
        if (place == null) return;
        ReservationEntity activeOther = reservationDao.getActiveReservationForSpotSync(place.getId());
        if (activeOther == null && (place.isOccupied() || place.isCheckedIn())) {
            place.setOccupied(false);
            place.setCheckedIn(false);
            place.setDriverName("");
            place.setDriverVehicle("");
            parkingPlaceDao.insertOrUpdate(place);
        }
    }

    @NonNull
    private static String resolveSpotTitle(@NonNull ReservationEntity entity, @Nullable ParkingPlaceEntity place) {
        if (entity.getPlaceName() != null && !entity.getPlaceName().trim().isEmpty()) {
            return entity.getPlaceName().trim();
        }
        if (place != null && place.getTitle() != null && !place.getTitle().trim().isEmpty()) {
            return place.getTitle().trim();
        }
        return "Miejsce parkingowe";
    }

    private void startLocationUpdates() {
        if (isLocationListening || !hasLocationPermission()) return;
        LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5.0f, locationListener, Looper.getMainLooper());
                isLocationListening = true;
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 10.0f, locationListener, Looper.getMainLooper());
                isLocationListening = true;
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
    }

    private void stopLocationUpdates() {
        if (!isLocationListening) return;
        LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                lm.removeUpdates(locationListener);
            } catch (SecurityException ignored) {
            }
        }
        isLocationListening = false;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location getBestCurrentLocation() {
        if (!hasLocationPermission()) return null;
        Location best = lastDriverLocation;
        LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                best = pickBetterLocation(best, lm.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                best = pickBetterLocation(best, lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                best = pickBetterLocation(best, lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
            } catch (SecurityException ignored) {
            }
        }
        if (best != null && (System.currentTimeMillis() - best.getTime()) > LOCATION_STALE_MS) {
            return null;
        }
        return best;
    }

    @Nullable
    private static Location pickBetterLocation(@Nullable Location currentBest, @Nullable Location candidate) {
        if (candidate == null) return currentBest;
        if (currentBest == null) return candidate;
        return candidate.getTime() >= currentBest.getTime() ? candidate : currentBest;
    }

    private void scheduleAlarm(@NonNull String reservationId, @NonNull String action, long triggerAtMs) {
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pi = buildAlarmPendingIntent(reservationId, action);
        if (pi == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            }
        } catch (Exception ignored) {
        }
    }

    private void cancelAlarms(@NonNull String reservationId) {
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        try {
            PendingIntent pi1 = buildAlarmPendingIntent(reservationId, ReservationAlarmReceiver.ACTION_URGENT_CHECKOUT);
            PendingIntent pi2 = buildAlarmPendingIntent(reservationId, ReservationAlarmReceiver.ACTION_AUTO_CHECKOUT);
            if (pi1 != null) alarmManager.cancel(pi1);
            if (pi2 != null) alarmManager.cancel(pi2);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private PendingIntent buildAlarmPendingIntent(@NonNull String reservationId, @NonNull String action) {
        Intent intent = new Intent(appContext, ReservationAlarmReceiver.class);
        intent.setAction(action);
        intent.putExtra(ReservationAlarmReceiver.EXTRA_RESERVATION_ID, reservationId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        int requestCode = (reservationId + "_" + action).hashCode();
        return PendingIntent.getBroadcast(appContext, requestCode, intent, flags);
    }
}
