package com.example.project.network;

import androidx.annotation.NonNull;

import com.example.project.utils.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor responsible for retrieving the stored JWT Bearer token from {@link SessionManager}
 * and automatically injecting it into the HTTP Authorization header for network requests.
 */
public class AuthInterceptor implements Interceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionManager sessionManager;

    /**
     * Default constructor using the singleton SessionManager.
     */
    public AuthInterceptor() {
        this(SessionManager.getInstance());
    }

    /**
     * Parameterized constructor allowing custom SessionManager injection for testing and modularity.
     *
     * @param sessionManager SessionManager instance managing user session and tokens
     */
    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // If request already contains an Authorization header, proceed without modification
        if (originalRequest.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(originalRequest);
        }

        // Skip adding Authorization header to unauthenticated public authentication endpoints
        String path = originalRequest.url().encodedPath();
        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            return chain.proceed(originalRequest);
        }

        SessionManager manager = sessionManager != null ? sessionManager : SessionManager.getInstance();
        if (manager != null) {
            String token = manager.getAuthToken();
            if (token != null && !token.trim().isEmpty()) {
                String authHeader = token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX + token;
                Request authenticatedRequest = originalRequest.newBuilder()
                        .header(HEADER_AUTHORIZATION, authHeader)
                        .build();
                return chain.proceed(authenticatedRequest);
            }
        }

        return chain.proceed(originalRequest);
    }
}
