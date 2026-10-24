package com.example.project.network;

import com.example.project.network.model.LoginRequest;
import com.example.project.network.model.LoginResponse;
import com.example.project.network.model.RegisterRequest;
import com.example.project.network.model.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit REST API interface defining authentication and account endpoints.
 */
public interface ApiService {

    /**
     * Authenticates a user with email and password credentials.
     *
     * @param request Authentication request payload containing email and password
     * @return Retrofit Call delivering the login response containing JWT token and user profile
     */
    @POST("api/auth/login")
    Call<LoginResponse> login(
            @Body LoginRequest request
    );

    /**
     * Registers a new user account on the backend service.
     *
     * @param request Registration payload containing user credentials and details
     * @return Retrofit Call delivering the registered user profile
     */
    @POST("api/auth/register")
    Call<RegisterResponse> register(
            @Body RegisterRequest request
    );
}
