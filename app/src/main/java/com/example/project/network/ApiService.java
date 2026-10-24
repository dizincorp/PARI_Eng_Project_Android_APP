package com.example.project.network;

import com.example.project.network.model.LoginRequest;
import com.example.project.network.model.LoginResponse;
import com.example.project.network.model.RegisterRequest;
import com.example.project.network.model.RegisterResponse;
import com.example.project.network.model.UserProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
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

    /**
     * Retrieves the profile details of the currently authenticated user
     * using the automatic Authorization header injected by AuthInterceptor.
     *
     * @return Retrofit Call delivering the authenticated user's profile
     */
    @GET("api/auth/me")
    Call<UserProfileResponse> getCurrentUser();

    /**
     * Retrieves the profile details of the currently authenticated user.
     *
     * @param authToken Bearer authorization token header (e.g. "Bearer eyJhbGci...")
     * @return Retrofit Call delivering the authenticated user's profile
     */
    @GET("api/auth/me")
    Call<UserProfileResponse> getCurrentUser(
            @Header("Authorization") String authToken
    );
}
