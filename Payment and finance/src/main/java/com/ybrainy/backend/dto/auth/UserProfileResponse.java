package com.ybrainy.backend.dto.auth;

public record UserProfileResponse(
        String subject,
        String email,
        String name,
        String role
) {
}
