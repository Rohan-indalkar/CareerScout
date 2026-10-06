package com.careerscout.profile.controller;

import com.careerscout.common.api.ApiResponse;
import com.careerscout.profile.dto.SearchProfileRequest;
import com.careerscout.profile.dto.SearchProfileResponse;
import com.careerscout.profile.dto.SearchProfileStatusRequest;
import com.careerscout.profile.service.SearchProfileService;
import com.careerscout.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search-profiles")
@Tag(name = "Search Profiles")
@SecurityRequirement(name = "bearerAuth")
public class SearchProfileController {
    private final SearchProfileService profileService;

    public SearchProfileController(SearchProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a job search profile")
    ApiResponse<SearchProfileResponse> create(
            @Valid @RequestBody SearchProfileRequest body,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        return ApiResponse.success("Search profile created successfully",
                profileService.create(user, body), request.getRequestURI());
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's search profiles")
    ApiResponse<List<SearchProfileResponse>> findAll(
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success("Search profiles fetched successfully",
                profileService.findAll(user), request.getRequestURI());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owned search profile")
    ApiResponse<SearchProfileResponse> findById(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        return ApiResponse.success("Search profile fetched successfully",
                profileService.findById(user, id), request.getRequestURI());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an owned search profile")
    ApiResponse<SearchProfileResponse> update(
            @PathVariable Long id, @Valid @RequestBody SearchProfileRequest body,
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success("Search profile updated successfully",
                profileService.update(user, id, body), request.getRequestURI());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate an owned search profile")
    ApiResponse<SearchProfileResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody SearchProfileStatusRequest body,
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success("Search profile status updated successfully",
                profileService.updateStatus(user, id, body.active()), request.getRequestURI());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an owned search profile")
    ApiResponse<Map<String, String>> delete(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        profileService.delete(user, id);
        return ApiResponse.success("Search profile deleted successfully",
                Map.of("status", "deleted"), request.getRequestURI());
    }
}
