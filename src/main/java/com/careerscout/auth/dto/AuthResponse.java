package com.careerscout.auth.dto;

import com.careerscout.user.dto.UserResponse;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        UserResponse user) {
}
