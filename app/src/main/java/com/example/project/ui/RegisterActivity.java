package com.example.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.project.R;
import com.example.project.databinding.ActivityRegisterBinding;
import com.example.project.repository.AppRepository;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodel.AuthViewModel;

/**
 * Register Activity for new user account creation with session persistence.
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        AppRepository.getInstance().init(getApplicationContext());
        sessionManager = new SessionManager(this);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.editRegName.getText() != null ? binding.editRegName.getText().toString() : "";
            String email = binding.editRegEmail.getText() != null ? binding.editRegEmail.getText().toString() : "";
            String password = binding.editRegPassword.getText() != null ? binding.editRegPassword.getText().toString() : "";
            String confirmPassword = binding.editRegConfirmPassword.getText() != null ? binding.editRegConfirmPassword.getText().toString() : "";

            viewModel.register(name, email, password, confirmPassword);
        });

        binding.btnGoogleRegister.setOnClickListener(v -> viewModel.loginWithGoogle());

        binding.btnGoToLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getRegisterSuccessLiveData().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                String name = binding.editRegName.getText() != null ? binding.editRegName.getText().toString() : "Jan Kowalski";
                String email = binding.editRegEmail.getText() != null ? binding.editRegEmail.getText().toString() : "jan_kowalski@gmail.com";

                AppRepository.getInstance().loginUser(email, name);
                sessionManager.createLoginSession(email, name);

                Toast.makeText(this, R.string.msg_registration_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, OnboardingActivity.class);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getErrorMessageLiveData().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}