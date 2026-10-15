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
import com.example.project.databinding.ActivityLoginBinding;
import com.example.project.repository.AppRepository;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodel.AuthViewModel;

/**
 * Login Activity providing Email/Password and Google Authentication with session persistence.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        AppRepository.getInstance().init(getApplicationContext());
        sessionManager = new SessionManager(this);

        // Auto-login check: if user session exists, navigate directly to MainActivity
        if (sessionManager.isLoggedIn()) {
            AppRepository.getInstance().loginUser(sessionManager.getUserEmail(), sessionManager.getUserName());
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
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

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.editLoginEmail.getText() != null ? binding.editLoginEmail.getText().toString() : "";
            String password = binding.editLoginPassword.getText() != null ? binding.editLoginPassword.getText().toString() : "";
            viewModel.login(email, password);
        });

        binding.btnGoogleLogin.setOnClickListener(v -> viewModel.loginWithGoogle());

        binding.btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, R.string.link_forgot_password, Toast.LENGTH_SHORT).show()
        );
    }

    private void observeViewModel() {
        viewModel.getLoginSuccessLiveData().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                String email = binding.editLoginEmail.getText() != null ? binding.editLoginEmail.getText().toString() : "jan_kowalski@gmail.com";
                String name = "Jan Kowalski";

                AppRepository.getInstance().loginUser(email, name);
                sessionManager.createLoginSession(email, name);

                Toast.makeText(this, R.string.msg_login_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
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