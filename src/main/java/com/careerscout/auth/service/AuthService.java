package com.careerscout.auth.service;

import com.careerscout.auth.dto.AuthResponse;
import com.careerscout.auth.dto.LoginRequest;
import com.careerscout.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(Long userId, String sessionId, String refreshToken);
    void revokeSessions(Long userId);
}
