package com.example.project.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.project.repository.AppRepository;

/**
 * ViewModel managing user authentication and account creation workflows.
 */
public class AuthViewModel extends ViewModel {

    private final AppRepository repository;
    private final MutableLiveData<Boolean> loginSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    /**
     * Default constructor for AuthViewModel.
     */
    public AuthViewModel() {
        this.repository = AppRepository.getInstance();
    }

    public LiveData<Boolean> getLoginSuccessLiveData() {
        return loginSuccessLiveData;
    }

    public LiveData<Boolean> getRegisterSuccessLiveData() {
        return registerSuccessLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    /**
     * Validates and logs in the user with email and password.
     *
     * @param email    User email
     * @param password User password
     */
    public void login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Proszę podać email i hasło");
            return;
        }

        // Simulate successful login
        repository.updateUserName("Jan Kowalski");
        loginSuccessLiveData.setValue(true);
    }

    /**
     * Validates and registers a new user.
     *
     * @param name            User full name
     * @param email           User email
     * @param password        Password
     * @param confirmPassword Confirmed password
     */
    public void register(String name, String email, String password, String confirmPassword) {
        if (name == null || name.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Proszę wypełnić wszystkie pola");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorMessageLiveData.setValue("Hasła nie są identyczne");
            return;
        }

        repository.updateUserName(name.trim());
        registerSuccessLiveData.setValue(true);
    }

    /**
     * Simulates Google Sign-In authentication.
     */
    public void loginWithGoogle() {
        repository.updateUserName("Jan Kowalski (Google)");
        loginSuccessLiveData.setValue(true);
    }
}