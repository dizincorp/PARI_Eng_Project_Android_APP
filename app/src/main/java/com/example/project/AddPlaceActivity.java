package com.example.project;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.databinding.ActivityAddPlaceBinding;

public class AddPlaceActivity extends AppCompatActivity {

    private ActivityAddPlaceBinding binding;
    private AddPlaceViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddPlaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AddPlaceViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        if (binding == null) {
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSubmitPlace.setOnClickListener(v -> {
            String street = binding.editStreet.getText() != null ? binding.editStreet.getText().toString() : "";
            String city = binding.editCity.getText() != null ? binding.editCity.getText().toString() : "";
            String placeNumber = binding.editPlaceNumber.getText() != null ? binding.editPlaceNumber.getText().toString() : "";

            viewModel.savePlace(street, city, placeNumber);
        });

        binding.btnUploadPhotos.setOnClickListener(v ->
                Toast.makeText(this, R.string.prompt_add_photos, Toast.LENGTH_SHORT).show()
        );
    }

    private void observeViewModel() {
        viewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.msg_place_added, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessageResId().observe(this, resId -> {
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}