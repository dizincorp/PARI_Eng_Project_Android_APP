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
import com.example.project.network.Resource;
import com.example.project.network.model.LoginResponse;
import com.example.project.network.model.RegisterResponse;
import com.example.project.network.model.UserResponse;
import com.example.project.repository.AppRepository;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodel.AuthViewModel;

/**
 * Register Activity for new user account creation backed by REST API,
 * automatic JWT session initialization, and {@link SessionManager}.
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
        com.example.project.network.ApiClient.init(getApplicationContext());
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
            String firstName = binding.editRegFirstName.getText() != null ? binding.editRegFirstName.getText().toString().trim() : "";
            String lastName = binding.editRegLastName.getText() != null ? binding.editRegLastName.getText().toString().trim() : "";
            String email = binding.editRegEmail.getText() != null ? binding.editRegEmail.getText().toString().trim() : "";
            String phone = binding.editRegPhone.getText() != null ? binding.editRegPhone.getText().toString().trim() : "";
            String password = binding.editRegPassword.getText() != null ? binding.editRegPassword.getText().toString() : "";
            String confirmPassword = binding.editRegConfirmPassword.getText() != null ? binding.editRegConfirmPassword.getText().toString() : "";

            String selectedRole = (binding.radioRoleOwner != null && binding.radioRoleOwner.isChecked()) ? "OWNER" : "DRIVER";
            viewModel.register(firstName, lastName, email, password, confirmPassword, phone, selectedRole);
        });

        binding.btnGoogleRegister.setOnClickListener(v -> viewModel.loginWithGoogle());

        binding.btnGoToLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        // Observe registration endpoint response
        viewModel.getRegisterResultLiveData().observe(this, resource -> {
            if (resource == null || binding == null) return;

            switch (resource.status) {
                case LOADING:
                    binding.btnRegister.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.btnRegister.setEnabled(true);
                    if (resource.data != null) {
                        RegisterResponse user = resource.data;
                        String password = binding.editRegPassword.getText() != null
                                ? binding.editRegPassword.getText().toString()
                                : "";

                        // Attempt automatic login to acquire JWT token seamlessly
                        if (!user.getEmail().isEmpty() && !password.isEmpty()) {
                            viewModel.login(user.getEmail(), password);
                        } else {
                            // Fallback if password missing: store base session and navigate
                            sessionManager.createLoginSession("", user.getId(), user.getEmail(), user.getFullName(), user.getRole());
                            AppRepository.getInstance().loginUser(user.getEmail(), user.getFullName());
                            Toast.makeText(this, R.string.msg_registration_success, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, OnboardingActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                    break;
                case ERROR:
                    binding.btnRegister.setEnabled(true);
                    String errorMsg = resource.message != null ? resource.message : getString(R.string.error_invalid_credentials);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    break;
            }
        });

        // Observe login endpoint response (for seamless auto-login after registration)
        viewModel.getLoginResultLiveData().observe(this, resource -> {
            if (resource == null || binding == null) return;

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                LoginResponse loginResponse = resource.data;
                UserResponse user = loginResponse.getUser();

                String token = loginResponse.getToken();
                String userId = (user != null && user.getId() != null) ? user.getId() : "";
                String email = (user != null && user.getEmail() != null) ? user.getEmail() : "";
                String name = (user != null && user.getFullName() != null) ? user.getFullName() : "Jan Kowalski";
                String role = (user != null && user.getRole() != null) ? user.getRole() : "DRIVER";

                sessionManager.createLoginSession(token, userId, email, name, role);
                AppRepository.getInstance().loginUser(email, name);

                Toast.makeText(this, R.string.msg_registration_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, OnboardingActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
