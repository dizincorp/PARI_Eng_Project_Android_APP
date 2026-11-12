package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.network.ApiClient;
import com.example.project.network.ApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.ChangePasswordRequest;
import com.example.project.network.model.LoginRequest;
import com.example.project.network.model.LoginResponse;
import com.example.project.network.model.MessageResponse;
import com.example.project.network.model.RegisterRequest;
import com.example.project.network.model.RegisterResponse;
import com.example.project.network.model.UpdateProfileRequest;
import com.example.project.network.model.UserProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository responsible for user authentication and registration API calls via {@link ApiService}.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private static AuthRepository instance;

    private final ApiService apiService;

    /**
     * Private constructor initializing default ApiService from ApiClient.
     */
    private AuthRepository() {
        this(ApiClient.getApiService());
    }

    /**
     * Constructor allowing custom ApiService injection for modularity and testing.
     *
     * @param apiService Retrofit ApiService instance
     */
    public AuthRepository(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Gets the singleton instance of AuthRepository.
     *
     * @return Synchronized AuthRepository instance
     */
    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public static synchronized AuthRepository getInstance(Context context) {
        return getInstance();
    }

    /**
     * Executes login request against backend service.
     *
     * @param email    User email
     * @param password User plain text password
     * @return LiveData containing Resource with LoginResponse
     */
    public LiveData<Resource<LoginResponse>> login(String email, String password) {
        MutableLiveData<Resource<LoginResponse>> resultLiveData = new MutableLiveData<>();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Proszę podać email i hasło", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));

        LoginRequest request = new LoginRequest(email.trim(), password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = (response.code() == 401 || response.code() == 403)
                            ? "Niepoprawny email lub hasło"
                            : "Błąd serwera: " + response.code();
                    Log.e(TAG, "Login failed with HTTP status: " + response.code());
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "Login network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Executes user registration request against backend service.
     *
     * @param request Registration payload containing user data
     * @return LiveData containing Resource with RegisterResponse
     */
    public LiveData<Resource<RegisterResponse>> register(RegisterRequest request) {
        MutableLiveData<Resource<RegisterResponse>> resultLiveData = new MutableLiveData<>();

        if (request == null) {
            resultLiveData.setValue(Resource.error("Dane rejestracji nie mogą być puste", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));

        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = (response.code() == 409)
                            ? "Konto z tym adresem email lub numerem telefonu już istnieje"
                            : "Błąd rejestracji: " + response.code();
                    Log.e(TAG, "Register failed with HTTP status: " + response.code());
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "Register network failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Overloaded helper to register user with individual fields.
     */
    public LiveData<Resource<RegisterResponse>> register(String firstName, String lastName, String email,
                                                        String password, String phone, String role) {
        return register(new RegisterRequest(email, password, firstName, lastName, phone, role));
    }

    /**
     * Fetches current user profile from backend using automatic JWT Bearer authorization from AuthInterceptor.
     *
     * @return LiveData containing Resource with UserProfileResponse
     */
    public LiveData<Resource<UserProfileResponse>> getCurrentUser() {
        MutableLiveData<Resource<UserProfileResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getCurrentUser().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Błąd pobierania profilu: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "getCurrentUser failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Fetches current user profile from backend using explicit JWT authorization token.
     *
     * @param token JWT authorization token
     * @return LiveData containing Resource with UserProfileResponse
     */
    public LiveData<Resource<UserProfileResponse>> getCurrentUser(String token) {
        MutableLiveData<Resource<UserProfileResponse>> resultLiveData = new MutableLiveData<>();

        if (token == null || token.trim().isEmpty()) {
            resultLiveData.setValue(Resource.error("Brak tokenu autoryzacji", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));

        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        apiService.getCurrentUser(authHeader).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Błąd pobierania profilu: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "getCurrentUser failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Updates user profile details via PUT /api/auth/profile.
     *
     * @param request UpdateProfileRequest containing new profile details
     * @return LiveData containing Resource with UserProfileResponse
     */
    public LiveData<Resource<UserProfileResponse>> updateProfile(UpdateProfileRequest request) {
        MutableLiveData<Resource<UserProfileResponse>> resultLiveData = new MutableLiveData<>();
        if (request == null) {
            resultLiveData.setValue(Resource.error("Brak danych profilu do aktualizacji", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));
        apiService.updateProfile(request).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Błąd aktualizacji profilu: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "updateProfile failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }

    /**
     * Changes user password via POST /api/auth/change-password.
     *
     * @param request ChangePasswordRequest containing current and new passwords
     * @return LiveData containing Resource with MessageResponse
     */
    public LiveData<Resource<MessageResponse>> changePassword(ChangePasswordRequest request) {
        MutableLiveData<Resource<MessageResponse>> resultLiveData = new MutableLiveData<>();
        if (request == null) {
            resultLiveData.setValue(Resource.error("Brak danych do zmiany hasła", null));
            return resultLiveData;
        }

        resultLiveData.setValue(Resource.loading(null));
        apiService.changePassword(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else if (response.code() == 400 || response.code() == 401) {
                    resultLiveData.setValue(Resource.error("Aktualne hasło jest niepoprawne", null));
                } else {
                    String errorMsg = "Błąd zmiany hasła: " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "changePassword failure: " + errorMsg, t);
                resultLiveData.setValue(Resource.error(errorMsg, null));
            }
        });

        return resultLiveData;
    }
}
