package com.ybrainy.backend.dto.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String email,
        String name,
        String role
) {
}
