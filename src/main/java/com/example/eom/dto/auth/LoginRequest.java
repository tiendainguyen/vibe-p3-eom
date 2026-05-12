package com.example.eom.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for user login")
public record LoginRequest(

        @Schema(description = "Registered email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Account password", example = "Secret@123")
        @NotBlank(message = "Password is required")
        String password
) {}
