package com.example.project.ui;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivitySettingsBinding;
import com.example.project.viewmodel.SettingsViewModel;

/**
 * Activity for app settings including accessibility, dark mode, and language.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupClickListeners();
        setupFontSizeSeekBar();
        observeViewModel();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardNotifications.setOnClickListener(v ->
                Toast.makeText(this, R.string.label_notifications, Toast.LENGTH_SHORT).show()
        );

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        binding.cardLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
    }

    private void setupFontSizeSeekBar() {
        if (binding == null) return;

        binding.seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newSizeSp = 13f + (progress * 2f); // 13sp to 21sp
                binding.textPreviewSample.setTextSize(newSizeSp);
                viewModel.setFontSize(newSizeSp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void showLanguageSelectionDialog() {
        String[] languages = new String[]{getString(R.string.lang_polski), getString(R.string.lang_english)};

        new AlertDialog.Builder(this)
                .setTitle(R.string.label_language)
                .setItems(languages, (dialog, which) -> {
                    String selectedLang = languages[which];
                    viewModel.setLanguage(selectedLang);
                    Toast.makeText(this, selectedLang, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void observeViewModel() {
        viewModel.getUserProfileLiveData().observe(this, profile -> {
            if (binding == null || profile == null) return;
            binding.textSettingsUserName.setText(profile.getName());
            binding.imgSettingsAvatar.setImageResource(profile.getAvatarResId());
        });

        viewModel.getLanguageLiveData().observe(this, lang -> {
            if (binding != null && lang != null) {
                binding.textSelectedLanguage.setText(lang);
            }
        });

        viewModel.getFontSizeLiveData().observe(this, sizeSp -> {
            if (binding != null && sizeSp != null) {
                binding.textPreviewSample.setTextSize(sizeSp);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}