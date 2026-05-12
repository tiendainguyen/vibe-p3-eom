package com.example.eom.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned on successful registration or login")
public record AuthResponse(

        @Schema(description = "JWT Bearer token")
        String token,

        @Schema(description = "Authenticated user ID", example = "1")
        Long userId,

        @Schema(description = "Authenticated user email", example = "user@example.com")
        String email,

        @Schema(description = "User role: USER or ADMIN", example = "USER")
        String role
) {}
