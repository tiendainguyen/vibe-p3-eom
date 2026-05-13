package com.example.eom.dto.user;

import com.example.eom.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "User details for admin management")
public record AdminUserResponse(

        @Schema(description = "User ID", example = "1")
        Long id,

        @Schema(description = "User email address", example = "user@example.com")
        String email,

        @Schema(description = "User role", example = "USER")
        Role role,

        @Schema(description = "Whether the account is active", example = "true")
        boolean active,

        @Schema(description = "Account creation timestamp")
        Instant createdAt
) {}
