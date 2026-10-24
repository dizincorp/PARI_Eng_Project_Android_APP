package com.example.project.network.model;

/**
 * Data Transfer Object (DTO) representing the response from account registration at /api/auth/register.
 * Inherits all fields from {@link UserResponse}.
 */
public class RegisterResponse extends UserResponse {

    /**
     * Default no-argument constructor for Gson deserialization.
     */
    public RegisterResponse() {
        super();
    }

    /**
     * Parameterized constructor for RegisterResponse.
     *
     * @param id            Unique user identifier (UUID)
     * @param email         User email address
     * @param firstName     User first name
     * @param lastName      User last name
     * @param role          User role (e.g. "DRIVER", "OWNER")
     * @param loyaltyPoints Accumulated loyalty reward points
     * @param avatarUrl     Public avatar image URL (nullable)
     */
    public RegisterResponse(String id, String email, String firstName, String lastName,
                            String role, Integer loyaltyPoints, String avatarUrl) {
        super(id, email, firstName, lastName, role, loyaltyPoints, avatarUrl);
    }
}
