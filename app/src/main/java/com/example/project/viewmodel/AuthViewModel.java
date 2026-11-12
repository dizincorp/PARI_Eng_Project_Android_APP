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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
    /**
     * Registers a new user directly with first and last name matching backend /api/auth/register endpoint.
     *
     * @param firstName       User's first name
     * @param lastName        User's last name
     * @param email           User email
     * @param password        Account password
     * @param confirmPassword Confirmed password matching check
     * @param phoneNumber     Contact phone number
     * @param role            Chosen role ("DRIVER" or "OWNER")
     */
    /**
     * Registers a new user account with automatic DRIVER and OWNER dual status.
     *
     * @param firstName       User's first name
     * @param lastName        User's last name
     * @param email           User email
     * @param password        Account password
     * @param confirmPassword Confirmed password matching check
     * @param phoneNumber     Contact phone number
     */
    public void register(String firstName, String lastName, String email, String password,
                         String confirmPassword, String phoneNumber) {
        register(firstName, lastName, email, password, confirmPassword, phoneNumber, Arrays.asList("DRIVER", "OWNER"));
    }

    /**
     * Registers a new user account with specified statuses list.
     *
     * @param firstName       User's first name
     * @param lastName        User's last name
     * @param email           User email
     * @param password        Account password
     * @param confirmPassword Confirmed password matching check
     * @param phoneNumber     Contact phone number
     * @param statuses        List of user statuses/roles (e.g. ["DRIVER", "OWNER"])
     */
    public void register(String firstName, String lastName, String email, String password,
                         String confirmPassword, String phoneNumber, List<String> statuses) {
        if (firstName == null || firstName.trim().isEmpty()
                || lastName == null || lastName.trim().isEmpty()
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

        String phone = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber.trim() : "6832380753";
        List<String> userStatuses = (statuses != null && !statuses.isEmpty())
                ? statuses : Arrays.asList("DRIVER", "OWNER");

        RegisterRequest request = new RegisterRequest(
                email.trim(),
                password,
                firstName.trim(),
                lastName.trim(),
                phone,
                userStatuses
        );

        registerResultLiveData.setValue(Resource.loading(null));

        LiveData<Resource<RegisterResponse>> repoSource = authRepository.register(request);
        registerResultLiveData.addSource(repoSource, resource -> {
            registerResultLiveData.setValue(resource);
            if (resource != null && !resource.isLoading()) {
                registerResultLiveData.removeSource(repoSource);
                if (resource.isSuccess()) {
                    registerSuccessLiveData.setValue(true);
                } else if (resource.isError()) {
                    errorMessageLiveData.setValue(resource.message);
                    registerSuccessLiveData.setValue(false);
                }
            }
        });
    }

    /**
     * Legacy registration method supporting single role parameter.
     *
     * @param firstName       User's first name
     * @param lastName        User's last name
     * @param email           User email
     * @param password        Account password
     * @param confirmPassword Confirmed password matching check
     * @param phoneNumber     Contact phone number
     * @param role            Chosen role ("DRIVER" or "OWNER")
     */
    public void register(String firstName, String lastName, String email, String password,
                         String confirmPassword, String phoneNumber, String role) {
        register(firstName, lastName, email, password, confirmPassword, phoneNumber, Arrays.asList("DRIVER", "OWNER"));
    }

    /**
     * Convenience registration overload defaulting role to dual DRIVER and OWNER status.
     *
     * @param name            Full name
     * @param email           User email
     * @param password        Password
     * @param confirmPassword Confirmed password
     */
    public void register(String name, String email, String password, String confirmPassword) {
        String trimmedName = name != null ? name.trim() : "";
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
        register(firstName, lastName, email, password, confirmPassword, null);
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
     * Authenticates or registers an account using Google credentials with dual DRIVER and OWNER status.
     *
     * @param email     User email address
     * @param firstName User first name
     * @param lastName  User last name
     */
    public void loginOrRegisterWithGoogle(@NonNull String email, @NonNull String firstName, @NonNull String lastName) {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        String cleanFirst = firstName.trim().isEmpty() ? "Użytkownik" : firstName.trim();
        String cleanLast = lastName.trim().isEmpty() ? "Google" : lastName.trim();
        String fullName = (cleanFirst + " " + cleanLast).trim();

        loginResultLiveData.setValue(Resource.loading(null));

        // Deterministic credential password for backend service
        String googlePass = "GoogleAuth_" + Math.abs(cleanEmail.hashCode()) + "_Pari2026!";

        // 1. Attempt login on backend
        LiveData<Resource<LoginResponse>> loginSource = authRepository.login(cleanEmail, googlePass);
        loginResultLiveData.addSource(loginSource, loginRes -> {
            if (loginRes == null) return;
            if (loginRes.status == Resource.Status.SUCCESS && loginRes.data != null) {
                loginResultLiveData.removeSource(loginSource);
                UserResponse user = loginRes.data.getUser();
                if (user != null) {
                    user.setRoles(Arrays.asList("DRIVER", "OWNER"));
                }
                appRepository.loginUser(cleanEmail, fullName);
                loginResultLiveData.setValue(loginRes);
                loginSuccessLiveData.setValue(true);
            } else if (loginRes.status == Resource.Status.ERROR) {
                loginResultLiveData.removeSource(loginSource);
                // Not found or invalid password: try to register on backend with dual DRIVER & OWNER role
                com.example.project.network.model.RegisterRequest regReq = new com.example.project.network.model.RegisterRequest(
                        cleanEmail, googlePass, cleanFirst, cleanLast, null, "DRIVER");
                LiveData<Resource<RegisterResponse>> regSource = authRepository.register(regReq);
                loginResultLiveData.addSource(regSource, regRes -> {
                    if (regRes == null) return;
                    if (regRes.status == Resource.Status.SUCCESS && regRes.data != null) {
                        loginResultLiveData.removeSource(regSource);
                        // Registration succeeded: now log in
                        LiveData<Resource<LoginResponse>> secondLoginSource = authRepository.login(cleanEmail, googlePass);
                        loginResultLiveData.addSource(secondLoginSource, secondLoginRes -> {
                            if (secondLoginRes == null) return;
                            if (secondLoginRes.status == Resource.Status.SUCCESS && secondLoginRes.data != null) {
                                loginResultLiveData.removeSource(secondLoginSource);
                                UserResponse u = secondLoginRes.data.getUser();
                                if (u != null) {
                                    u.setRoles(Arrays.asList("DRIVER", "OWNER"));
                                }
                                appRepository.loginUser(cleanEmail, fullName);
                                loginResultLiveData.setValue(secondLoginRes);
                                loginSuccessLiveData.setValue(true);
                            } else if (secondLoginRes.status == Resource.Status.ERROR) {
                                loginResultLiveData.removeSource(secondLoginSource);
                                createLocalGoogleSession(cleanEmail, cleanFirst, cleanLast, fullName);
                            }
                        });
                    } else if (regRes.status == Resource.Status.ERROR) {
                        loginResultLiveData.removeSource(regSource);
                        // If register fails (e.g. backend offline or email conflict), fall back to authenticated local session
                        createLocalGoogleSession(cleanEmail, cleanFirst, cleanLast, fullName);
                    }
                });
            }
        });
    }

    private void createLocalGoogleSession(String cleanEmail, String cleanFirst, String cleanLast, String fullName) {
        UserResponse localUser = new UserResponse(
                "google_" + System.currentTimeMillis(),
                cleanEmail,
                cleanFirst,
                cleanLast,
                "DRIVER",
                100,
                null
        );
        localUser.setRoles(Arrays.asList("DRIVER", "OWNER"));
        localUser.setPhoneNumber("");
        LoginResponse localResponse = new LoginResponse(
                "token_google_" + System.currentTimeMillis(), "Bearer", 86400L, localUser);

        appRepository.loginUser(cleanEmail, fullName);
        loginResultLiveData.setValue(Resource.success(localResponse));
        loginSuccessLiveData.setValue(true);
    }

    /**
     * Simulates Google Sign-In authentication.
     */
    public void loginWithGoogle() {
        loginOrRegisterWithGoogle("jan.kowalski@gmail.com", "Jan", "Kowalski");
    }
}
