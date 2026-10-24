package com.example.project.network;

import com.example.project.network.model.LoginRequest;
import com.example.project.network.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit REST API interface defining authentication endpoints.
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
}
