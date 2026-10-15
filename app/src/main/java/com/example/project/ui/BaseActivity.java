package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.repository.AppRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base Activity class providing common bottom navigation behavior and repository init.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRepository.getInstance().init(getApplicationContext());
    }

    /**
     * Configures the BottomNavigationView item selection listener and state.
     *
     * @param bottomNavigationView Target BottomNavigationView
     * @param currentItemId        ID of current active menu item
     */
    protected void setupBottomNavigation(BottomNavigationView bottomNavigationView, int currentItemId) {
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setSelectedItemId(currentItemId);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item == null) {
                return false;
            }

            int itemId = item.getItemId();
            if (itemId == currentItemId) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_search) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.nav_reservations) {
                intent = new Intent(this, ReservationsActivity.class);
            } else if (itemId == R.id.nav_places) {
                intent = new Intent(this, PlacesActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        bottomNavigationView.setOnItemReselectedListener(item -> {
            // No-op to avoid unnecessary activity re-creations
        });
    }
}