package com.careerscout.user.controller;

import com.careerscout.common.api.ApiResponse;
import com.careerscout.security.AuthenticatedUser;
import com.careerscout.user.dto.ChangePasswordRequest;
import com.careerscout.user.dto.UpdateProfileRequest;
import com.careerscout.user.dto.UserResponse;
import com.careerscout.user.service.UserService;
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
@RequestMapping("/api/v1/users/me")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's account")
    ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest request) {
        return ApiResponse.success("User fetched successfully", userService.getCurrentUser(principal),
                request.getRequestURI());
    }

    @PutMapping
    @Operation(summary = "Update the authenticated user's name and email")
    ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest body,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request) {
        return ApiResponse.success("Profile updated successfully",
                userService.updateProfile(principal, body), request.getRequestURI());
    }

    @PatchMapping("/password")
    @Operation(summary = "Change the authenticated user's password and revoke refresh sessions")
    ApiResponse<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest body,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request) {
        userService.changePassword(principal, body);
        return ApiResponse.success("Password changed successfully", Map.of("status", "updated"),
                request.getRequestURI());
    }
}
