package com.example.project.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.network.Resource;
import com.example.project.network.model.LoginResponse;
import com.example.project.network.model.RegisterRequest;
import com.example.project.network.model.RegisterResponse;
import com.example.project.network.model.UserProfileResponse;
import com.example.project.network.model.UserResponse;
import com.example.project.repository.AppRepository;
import com.example.project.repository.AuthRepository;

/**
 * ViewModel managing user authentication, registration, and profile retrieval workflows
 * backed by {@link AuthRepository} and {@link AppRepository}.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final AppRepository appRepository;

    private final MediatorLiveData<Resource<LoginResponse>> loginResultLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<RegisterResponse>> registerResultLiveData = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<UserProfileResponse>> userProfileResultLiveData = new MediatorLiveData<>();

    private final MutableLiveData<Boolean> loginSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    /**
     * Default constructor for AuthViewModel using singleton repositories.
     */
    public AuthViewModel() {
        this(AuthRepository.getInstance(), AppRepository.getInstance());
    }

    /**
     * Constructor allowing repository injection for testing and modularity.
     *
     * @param authRepository Authentication network repository
     * @param appRepository  Application central repository
     */
    public AuthViewModel(@NonNull AuthRepository authRepository, @NonNull AppRepository appRepository) {
        this.authRepository = authRepository;
        this.appRepository = appRepository;
    }

    /**
     * Gets LiveData with login operation state and response payload.
     *
     * @return LiveData containing Resource of LoginResponse
     */
    public LiveData<Resource<LoginResponse>> getLoginResultLiveData() {
        return loginResultLiveData;
    }

    /**
     * Gets LiveData with register operation state and response payload.
     *
     * @return LiveData containing Resource of RegisterResponse
     */
    public LiveData<Resource<RegisterResponse>> getRegisterResultLiveData() {
        return registerResultLiveData;
    }

    /**
     * Gets LiveData with current user profile operation state and response payload.
     *
     * @return LiveData containing Resource of UserProfileResponse
     */
    public LiveData<Resource<UserProfileResponse>> getUserProfileResultLiveData() {
        return userProfileResultLiveData;
    }

    /**
     * Gets backward-compatible boolean LiveData for login success.
     *
     * @return LiveData emitting true on successful login
     */
    public LiveData<Boolean> getLoginSuccessLiveData() {
        return loginSuccessLiveData;
    }

    /**
     * Gets backward-compatible boolean LiveData for registration success.
     *
     * @return LiveData emitting true on successful registration
     */
    public LiveData<Boolean> getRegisterSuccessLiveData() {
        return registerSuccessLiveData;
    }

    /**
     * Gets backward-compatible error message LiveData.
     *
     * @return LiveData emitting error message string
     */
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * Authenticates user against backend API using email and password.
     *
     * @param email    User email
     * @param password User plain text password
     */
    public void login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            String validationError = "Proszę podać email i hasło";
            errorMessageLiveData.setValue(validationError);
            loginResultLiveData.setValue(Resource.error(validationError, null));
            return;
        }

        loginResultLiveData.setValue(Resource.loading(null));
        LiveData<Resource<LoginResponse>> source = authRepository.login(email.trim(), password);
        loginResultLiveData.addSource(source, resource -> {
            loginResultLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null && resource.data.getUser() != null) {
                    appRepository.updateUserName(resource.data.getUser().getFullName());
                }
                loginSuccessLiveData.setValue(true);
                loginResultLiveData.removeSource(source);
            } else if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.setValue(resource.message);
                loginResultLiveData.removeSource(source);
            }
        });
    }

    /**
     * Registers a new user account with complete credentials and role.
     *
     * @param name            Full name (split into first and last name)
     * @param email           User email
     * @param password        Account password
     * @param confirmPassword Confirmed password matching check
     * @param phoneNumber     Contact phone number
     * @param role            Chosen role ("DRIVER" or "OWNER")
     */
    public void register(String name, String email, String password, String confirmPassword,
                         String phoneNumber, String role) {
        if (name == null || name.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            String validationError = "Proszę wypełnić wszystkie pola";
            errorMessageLiveData.setValue(validationError);
            registerResultLiveData.setValue(Resource.error(validationError, null));
            return;
        }

        if (!password.equals(confirmPassword)) {
            String mismatchError = "Hasła nie są identyczne";
            errorMessageLiveData.setValue(mismatchError);
            registerResultLiveData.setValue(Resource.error(mismatchError, null));
            return;
        }

        String trimmedName = name.trim();
        String firstName = trimmedName;
        String lastName = "-";
        int spaceIndex = trimmedName.indexOf(' ');
        if (spaceIndex > 0) {
            firstName = trimmedName.substring(0, spaceIndex).trim();
            lastName = trimmedName.substring(spaceIndex + 1).trim();
        }
        if (lastName.isEmpty()) {
            lastName = "-";
        }

        String phone = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber.trim() : "6832380753";
        String userRole = (role != null && !role.trim().isEmpty()) ? role.trim() : "DRIVER";

        RegisterRequest request = new RegisterRequest(
                email.trim(),
                password,
                firstName,
                lastName,
                phone,
                userRole
        );

        registerResultLiveData.setValue(Resource.loading(null));
        LiveData<Resource<RegisterResponse>> source = authRepository.register(request);
        registerResultLiveData.addSource(source, resource -> {
            registerResultLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    appRepository.updateUserName(resource.data.getFullName());
                }
                registerSuccessLiveData.setValue(true);
                registerResultLiveData.removeSource(source);
            } else if (resource.status == Resource.Status.ERROR) {
                errorMessageLiveData.setValue(resource.message);
                registerResultLiveData.removeSource(source);
            }
        });
    }

    /**
     * Convenience registration overload defaulting role to DRIVER.
     *
     * @param name            Full name
     * @param email           User email
     * @param password        Password
     * @param confirmPassword Confirmed password
     */
    public void register(String name, String email, String password, String confirmPassword) {
        register(name, email, password, confirmPassword, null, "DRIVER");
    }

    /**
     * Fetches current authenticated user profile using JWT token.
     *
     * @param token Bearer JWT authentication token
     */
    public void fetchCurrentUserProfile(String token) {
        if (token == null || token.trim().isEmpty()) {
            userProfileResultLiveData.setValue(Resource.error("Brak tokenu autoryzacji", null));
            return;
        }

        userProfileResultLiveData.setValue(Resource.loading(null));
        LiveData<Resource<UserProfileResponse>> source = authRepository.getCurrentUser(token);
        userProfileResultLiveData.addSource(source, resource -> {
            userProfileResultLiveData.setValue(resource);
            if (resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    appRepository.loginUser(resource.data.getEmail(), resource.data.getFullName());
                }
                userProfileResultLiveData.removeSource(source);
            } else if (resource.status == Resource.Status.ERROR) {
                userProfileResultLiveData.removeSource(source);
            }
        });
    }

    /**
     * Simulates Google Sign-In authentication.
     */
    public void loginWithGoogle() {
        UserResponse googleUser = new UserResponse(
                "google-mock-id",
                "jan.kowalski@gmail.com",
                "Jan",
                "Kowalski",
                "DRIVER",
                100,
                null
        );
        LoginResponse googleResponse = new LoginResponse("mock-google-token", "Bearer", 86400L, googleUser);

        appRepository.loginUser("jan.kowalski@gmail.com", "Jan Kowalski (Google)");
        loginResultLiveData.setValue(Resource.success(googleResponse));
        loginSuccessLiveData.setValue(true);
    }
}
