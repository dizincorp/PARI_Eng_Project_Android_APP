package com.example.project.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.project.network.ApiClient;
import com.example.project.repository.AppRepository;

import java.util.concurrent.Executors;

/**
 * BroadcastReceiver woken by {@link android.app.AlarmManager} when a reservation reaches
 * its scheduled checkout deadline (urgent notification + overstay surcharge) or
 * the end of its 1-hour grace period (automatic checkout).
 */
public class ReservationAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_URGENT_CHECKOUT = "com.example.project.ACTION_URGENT_CHECKOUT";
    public static final String ACTION_AUTO_CHECKOUT = "com.example.project.ACTION_AUTO_CHECKOUT";
    public static final String EXTRA_RESERVATION_ID = "extra_reservation_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        final PendingResult pendingResult = goAsync();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppRepository.getInstance().init(appContext);
                ApiClient.init(appContext);
                ReservationLifecycleManager.getInstance(appContext).evaluateFromAlarmSync();
            } finally {
                pendingResult.finish();
            }
        });
    }
}
