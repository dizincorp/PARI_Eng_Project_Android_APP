package com.example.project.network.model;

/**
 * Data Transfer Object (DTO) representing current user profile details returned by GET /api/auth/me.
 * Inherits all fields from {@link UserResponse}.
 */
public class UserProfileResponse extends UserResponse {

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public UserProfileResponse() {
        super();
    }

    /**
     * Parameterized constructor for UserProfileResponse.
     *
     * @param id            Unique user identifier (UUID)
     * @param email         User email address
     * @param firstName     User first name
     * @param lastName      User last name
     * @param role          User role (e.g. "DRIVER", "OWNER")
     * @param loyaltyPoints Accumulated loyalty reward points
     * @param avatarUrl     Public avatar image URL (nullable)
     */
    public UserProfileResponse(String id, String email, String firstName, String lastName,
                               String role, Integer loyaltyPoints, String avatarUrl) {
        super(id, email, firstName, lastName, role, loyaltyPoints, avatarUrl);
    }
}
