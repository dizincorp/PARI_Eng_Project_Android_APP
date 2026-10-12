package com.example.project;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

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