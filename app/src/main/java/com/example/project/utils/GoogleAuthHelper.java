package com.example.project.utils;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.project.R;
import com.example.project.databinding.DialogGoogleSigninBinding;
import com.example.project.databinding.DialogRequirePhoneBinding;
import com.example.project.network.model.UpdateProfileRequest;
import com.example.project.repository.AppRepository;
import com.example.project.repository.AuthRepository;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helper class for Google Sign-In account selection, name derivation,
 * and mandatory phone number collection after onboarding.
 */
public final class GoogleAuthHelper {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");

    private GoogleAuthHelper() {
        // Utility class
    }

    public interface OnGoogleAccountSelectedListener {
        void onSelected(@NonNull String email, @NonNull String firstName, @NonNull String lastName);
    }

    public interface OnPhoneSavedListener {
        void onPhoneSaved(@NonNull String phoneNumber);
    }

    /**
     * Extracts human-readable first name and last name from an email address.
     * E.g. "jan.kowalski@gmail.com" -> ["Jan", "Kowalski"].
     *
     * @param email User email address
     * @return Two-element array: [firstName, lastName]
     */
    @NonNull
    public static String[] extractNameFromEmail(@Nullable String email) {
        if (email == null || !email.contains("@")) {
            return new String[]{"Użytkownik", "Google"};
        }
        String username = email.substring(0, email.indexOf('@')).trim();
        String clean = username.replaceAll("[0-9_.-]+", " ").trim();
        if (clean.isEmpty()) {
            return new String[]{"Użytkownik", "Google"};
        }
        String[] parts = clean.split("\\s+");
        if (parts.length >= 2) {
            String first = capitalize(parts[0]);
            StringBuilder last = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (last.length() > 0) last.append(" ");
                last.append(capitalize(parts[i]));
            }
            return new String[]{first, last.toString()};
        } else {
            return new String[]{capitalize(parts[0]), "Google"};
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Creates an Intent to invoke Android system's native Google account chooser dialog.
     *
     * @return Intent for AccountManager.newChooseAccountIntent
     */
    @NonNull
    public static Intent createAccountChooserIntent() {
        return AccountManager.newChooseAccountIntent(
                null,
                null,
                new String[]{"com.google"},
                null,
                null,
                null,
                null
        );
    }

    /**
     * Displays a Google Sign-In account selection and input dialog.
     *
     * @param context               Activity context
     * @param prefilledEmail        Email preselected from system chooser, or null
     * @param systemPickerLauncher  Launcher for system account picker, or null
     * @param listener              Callback upon user confirmation
     */
    public static void showGoogleSignInDialog(@NonNull Context context,
                                              @Nullable String prefilledEmail,
                                              @Nullable ActivityResultLauncher<Intent> systemPickerLauncher,
                                              @NonNull OnGoogleAccountSelectedListener listener) {
        DialogGoogleSigninBinding dialogBinding = DialogGoogleSigninBinding.inflate(LayoutInflater.from(context));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (prefilledEmail != null && !prefilledEmail.trim().isEmpty()) {
            String email = prefilledEmail.trim();
            dialogBinding.editGoogleEmail.setText(email);
            String[] names = extractNameFromEmail(email);
            dialogBinding.editGoogleFirstName.setText(names[0]);
            dialogBinding.editGoogleLastName.setText(names[1]);
        }

        dialogBinding.editGoogleEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String email = s != null ? s.toString().trim() : "";
                if (email.contains("@")) {
                    String currentFirst = dialogBinding.editGoogleFirstName.getText() != null
                            ? dialogBinding.editGoogleFirstName.getText().toString().trim() : "";
                    String currentLast = dialogBinding.editGoogleLastName.getText() != null
                            ? dialogBinding.editGoogleLastName.getText().toString().trim() : "";
                    if (currentFirst.isEmpty() && currentLast.isEmpty()) {
                        String[] names = extractNameFromEmail(email);
                        dialogBinding.editGoogleFirstName.setText(names[0]);
                        dialogBinding.editGoogleLastName.setText(names[1]);
                    }
                }
            }
        });

        if (systemPickerLauncher != null) {
            dialogBinding.btnChooseSystemAccount.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    systemPickerLauncher.launch(createAccountChooserIntent());
                } catch (Exception e) {
                    Toast.makeText(context, "Konta Google niedostępne w systemie. Wpisz dane ręcznie.", Toast.LENGTH_SHORT).show();
                    showGoogleSignInDialog(context, null, null, listener);
                }
            });
        } else {
            dialogBinding.btnChooseSystemAccount.setVisibility(android.view.View.GONE);
        }

        dialogBinding.btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnCancelGoogle.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnConfirmGoogle.setOnClickListener(v -> {
            String email = dialogBinding.editGoogleEmail.getText() != null
                    ? dialogBinding.editGoogleEmail.getText().toString().trim() : "";
            String first = dialogBinding.editGoogleFirstName.getText() != null
                    ? dialogBinding.editGoogleFirstName.getText().toString().trim() : "";
            String last = dialogBinding.editGoogleLastName.getText() != null
                    ? dialogBinding.editGoogleLastName.getText().toString().trim() : "";

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                dialogBinding.editGoogleEmail.setError("Podaj poprawny adres e-mail");
                dialogBinding.editGoogleEmail.requestFocus();
                return;
            }

            if (first.isEmpty()) {
                String[] derived = extractNameFromEmail(email);
                first = derived[0];
                if (last.isEmpty()) last = derived[1];
            }
            if (last.isEmpty()) {
                last = "Google";
            }

            dialog.dismiss();
            listener.onSelected(email, first, last);
        });

        dialog.show();
    }

    /**
     * Displays a mandatory modal dialog requiring the user to provide their phone number
     * after completing or skipping the onboarding introduction.
     *
     * @param activity Hosting Activity
     * @param listener Callback when valid phone number is entered and saved
     */
    public static void showRequirePhoneDialog(@NonNull Activity activity, @NonNull OnPhoneSavedListener listener) {
        DialogRequirePhoneBinding dialogBinding = DialogRequirePhoneBinding.inflate(activity.getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        SessionManager sm = SessionManager.getInstance(activity);
        if (sm != null && !sm.getUserPhone().isEmpty()) {
            dialogBinding.editRequiredPhone.setText(sm.getUserPhone());
        }

        dialogBinding.btnSaveRequiredPhone.setOnClickListener(v -> {
            String phone = dialogBinding.editRequiredPhone.getText() != null
                    ? dialogBinding.editRequiredPhone.getText().toString().trim() : "";

            if (phone.isEmpty() || !PHONE_PATTERN.matcher(phone).matches() || phone.replaceAll("[^0-9]", "").length() < 7) {
                dialogBinding.editRequiredPhone.setError(activity.getString(R.string.error_invalid_phone));
                dialogBinding.editRequiredPhone.requestFocus();
                return;
            }

            // Save phone number locally in SessionManager and AppRepository
            if (sm != null) {
                sm.updateUserPhone(phone);
                sm.setNeedsPhonePrompt(false);
            }
            AppRepository.getInstance().updateUserPhone(phone);

            // Synchronize with backend API if user session exists
            if (sm != null && !sm.getAuthToken().isEmpty()) {
                UpdateProfileRequest req = new UpdateProfileRequest(
                        sm.getFirstName(), sm.getLastName(), phone, sm.getAvatarUri());
                AuthRepository.getInstance().updateProfile(req);
            }

            Toast.makeText(activity, R.string.msg_phone_saved, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            listener.onPhoneSaved(phone);
        });

        dialog.show();
    }
}
