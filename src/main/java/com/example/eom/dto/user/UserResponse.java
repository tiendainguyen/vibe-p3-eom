package com.example.eom.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Authenticated user profile")
public record UserResponse(

        @Schema(description = "User ID", example = "1")
        Long id,

        @Schema(description = "User email address", example = "user@example.com")
        String email,

        @Schema(description = "User role: USER or ADMIN", example = "USER")
        String role,

        @Schema(description = "Account creation timestamp")
        Instant createdAt
) {}
