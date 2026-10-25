package com.example.project.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityProfileDetailsBinding;
import com.example.project.databinding.DialogEditNameBinding;
import com.example.project.databinding.DialogEditPhoneBinding;
import com.example.project.model.UserProfile;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodel.ProfileViewModel;

import java.util.Locale;

/**
 * Activity presenting user profile details with photo editing and name/phone customization.
 */
public class ProfileDetailsActivity extends BaseActivity {

    private ActivityProfileDetailsBinding binding;
    private ProfileViewModel viewModel;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, R.string.permission_storage_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {
                            // Persistable permission might not be supported for external gallery intents
                        }
                        String uriString = imageUri.toString();
                        viewModel.updateUserAvatarUri(uriString);
                        sessionManager.updateAvatarUri(uriString);
                        binding.imgProfileDetailsAvatar.setImageURI(imageUri);
                        Toast.makeText(this, R.string.msg_photo_updated, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityProfileDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnChangePhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());
        binding.imgProfileDetailsAvatar.setOnClickListener(v -> checkPermissionAndOpenGallery());

        binding.cardPersonalInfo.setOnClickListener(v -> showEditNameDialog());
        binding.btnEditName.setOnClickListener(v -> showEditNameDialog());

        binding.layoutPhoneRow.setOnClickListener(v -> showEditPhoneDialog());
        binding.btnEditPhone.setOnClickListener(v -> showEditPhoneDialog());
    }

    private void observeViewModel() {
        viewModel.getUserProfileLiveData().observe(this, this::renderProfile);
    }

    private void renderProfile(UserProfile profile) {
        if (binding == null || profile == null) return;

        binding.textFullNameValue.setText(profile.getName());
        binding.textEmailValue.setText(profile.getEmail());
        binding.textPhoneValue.setText(profile.getPhone());

        if ("OWNER".equalsIgnoreCase(profile.getRole())) {
            binding.textRoleValue.setText(R.string.role_owner);
        } else {
            binding.textRoleValue.setText(R.string.role_driver);
        }

        binding.textRatingValue.setText(String.format(Locale.getDefault(), "★ %.1f", profile.getRating()));

        if (profile.getAvatarUri() != null && !profile.getAvatarUri().isEmpty()) {
            try {
                binding.imgProfileDetailsAvatar.setImageURI(Uri.parse(profile.getAvatarUri()));
            } catch (Exception e) {
                binding.imgProfileDetailsAvatar.setImageResource(profile.getAvatarResId());
            }
        } else {
            binding.imgProfileDetailsAvatar.setImageResource(profile.getAvatarResId());
        }
    }

    /**
     * Checks storage/media permissions and launches gallery or rationale dialog.
     */
    private void checkPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_permission_needed)
                    .setMessage(R.string.permission_profile_storage_rationale)
                    .setPositiveButton(R.string.btn_allow, (dialog, which) -> permissionLauncher.launch(permission))
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    /**
     * Opens system gallery to select a new avatar image.
     */
    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        photoPickerLauncher.launch(pickIntent);
    }

    /**
     * Displays dialog for separately editing user first name and last name.
     */
    private void showEditNameDialog() {
        DialogEditNameBinding dialogBinding = DialogEditNameBinding.inflate(LayoutInflater.from(this));

        UserProfile profile = viewModel.getUserProfileLiveData().getValue();
        if (profile != null) {
            dialogBinding.editFirstName.setText(profile.getFirstName());
            dialogBinding.editLastName.setText(profile.getLastName());
        } else {
            dialogBinding.editFirstName.setText(sessionManager.getFirstName());
            dialogBinding.editLastName.setText(sessionManager.getLastName());
        }

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String firstName = dialogBinding.editFirstName.getText() != null
                            ? dialogBinding.editFirstName.getText().toString().trim() : "";
                    String lastName = dialogBinding.editLastName.getText() != null
                            ? dialogBinding.editLastName.getText().toString().trim() : "";

                    if (firstName.isEmpty() && lastName.isEmpty()) {
                        Toast.makeText(this, R.string.error_name_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    viewModel.updateUserName(firstName, lastName);
                    sessionManager.updateUserName(firstName, lastName);
                    Toast.makeText(this, R.string.msg_name_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /**
     * Displays dialog for editing user phone number.
     */
    private void showEditPhoneDialog() {
        DialogEditPhoneBinding dialogBinding = DialogEditPhoneBinding.inflate(LayoutInflater.from(this));

        UserProfile profile = viewModel.getUserProfileLiveData().getValue();
        if (profile != null) {
            dialogBinding.editPhoneNumber.setText(profile.getPhone());
        } else {
            dialogBinding.editPhoneNumber.setText(sessionManager.getUserPhone());
        }

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String phone = dialogBinding.editPhoneNumber.getText() != null
                            ? dialogBinding.editPhoneNumber.getText().toString().trim() : "";

                    if (!phone.isEmpty()) {
                        viewModel.updateUserPhone(phone);
                        sessionManager.updateUserPhone(phone);
                        Toast.makeText(this, R.string.msg_phone_updated, Toast.LENGTH_SHORT).show();
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
