package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityProfileBinding;
import com.example.project.databinding.DialogEditProfileBinding;
import com.example.project.network.Resource;
import com.example.project.network.model.UserProfileResponse;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodel.ProfileViewModel;

/**
 * Activity for user profile management with local Room caching,
 * remote profile synchronization via /api/auth/me, and session termination.
 */
public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation(binding.bottomNavigation, R.id.nav_profile);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupClickListeners();
        observeViewModel();

        // Synchronize latest profile data from backend if authenticated
        String authToken = sessionManager.getAuthToken();
        if (!authToken.isEmpty()) {
            viewModel.fetchRemoteProfile(authToken);
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.textUserName.setOnClickListener(v -> showEditProfileDialog());

        binding.cardMyReservations.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ReservationsActivity.class);
            startActivity(intent);
        });

        binding.cardPaymentMethods.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, PaymentMethodsActivity.class);
            startActivity(intent);
        });

        binding.cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.cardLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Toast.makeText(this, R.string.msg_logout_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void observeViewModel() {
        viewModel.getUserProfileLiveData().observe(this, profile -> {
            if (binding == null || profile == null) return;

            binding.textUserName.setText(profile.getName());
            binding.textUserEmail.setText(profile.getEmail());
            binding.imgProfileAvatar.setImageResource(profile.getAvatarResId());
        });

        viewModel.getRemoteProfileLiveData().observe(this, resource -> {
            if (binding == null || resource == null) return;

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                UserProfileResponse data = resource.data;
                binding.textUserName.setText(data.getFullName());
                binding.textUserEmail.setText(data.getEmail());
            }
        });
    }

    private void showEditProfileDialog() {
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(this));

        if (binding != null) {
            dialogBinding.editUserName.setText(binding.textUserName.getText());
        }

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String newName = dialogBinding.editUserName.getText() != null ? dialogBinding.editUserName.getText().toString() : "";
                    if (!newName.trim().isEmpty()) {
                        viewModel.updateUserName(newName.trim());
                        sessionManager.updateUserName(newName.trim());
                        Toast.makeText(this, R.string.msg_profile_updated, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
