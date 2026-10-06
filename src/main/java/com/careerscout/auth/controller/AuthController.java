package com.careerscout.auth.controller;

import com.careerscout.auth.dto.AuthResponse;
import com.careerscout.auth.dto.LoginRequest;
import com.careerscout.auth.dto.RefreshTokenRequest;
import com.careerscout.auth.dto.RegisterRequest;
import com.careerscout.auth.service.AuthService;
import com.careerscout.common.api.ApiResponse;
import com.careerscout.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user account")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Account registered successfully", authService.register(request),
                httpRequest.getRequestURI());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access and refresh tokens")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Login successful", authService.login(request), httpRequest.getRequestURI());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token and issue a new token pair")
    ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                       HttpServletRequest httpRequest) {
        return ApiResponse.success("Tokens refreshed successfully",
                authService.refresh(request.refreshToken()), httpRequest.getRequestURI());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke the current access and refresh tokens")
    ApiResponse<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest) {
        authService.logout(principal.id(), principal.sessionId(), request.refreshToken());
        return ApiResponse.success("Logged out successfully", Map.of("status", "logged_out"),
                httpRequest.getRequestURI());
    }
}
