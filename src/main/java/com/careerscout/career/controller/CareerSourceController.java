package com.careerscout.career.controller;

import com.careerscout.career.dto.CareerSourceRequest;
import com.careerscout.career.dto.CareerSourceResponse;
import com.careerscout.career.dto.CareerSourceScanResponse;
import com.careerscout.career.dto.CareerSourceStatusRequest;
import com.careerscout.career.service.CareerPageScanService;
import com.careerscout.career.service.CareerSourceService;
import com.careerscout.common.api.ApiResponse;
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
@RequestMapping("/api/v1/career-sources")
@Tag(name = "Career Sources")
@SecurityRequirement(name = "bearerAuth")
public class CareerSourceController {
    private final CareerSourceService careerSourceService;
    private final CareerPageScanService careerPageScanService;

    public CareerSourceController(CareerSourceService careerSourceService,
                                  CareerPageScanService careerPageScanService) {
        this.careerSourceService = careerSourceService;
        this.careerPageScanService = careerPageScanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a company career page")
    ApiResponse<CareerSourceResponse> create(
            @Valid @RequestBody CareerSourceRequest body,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        return ApiResponse.success("Career source created successfully",
                careerSourceService.create(user, body), request.getRequestURI());
    }

    @GetMapping
    @Operation(summary = "List career pages owned by the authenticated user")
    ApiResponse<List<CareerSourceResponse>> findAll(
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success("Career sources fetched successfully",
                careerSourceService.findAll(user), request.getRequestURI());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one career page owned by the authenticated user")
    ApiResponse<CareerSourceResponse> findById(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        return ApiResponse.success("Career source fetched successfully",
                careerSourceService.findById(user, id), request.getRequestURI());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a career page")
    ApiResponse<CareerSourceResponse> update(
            @PathVariable Long id, @Valid @RequestBody CareerSourceRequest body,
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success("Career source updated successfully",
                careerSourceService.update(user, id, body), request.getRequestURI());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a career page")
    ApiResponse<CareerSourceResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody CareerSourceStatusRequest body,
            @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        return ApiResponse.success("Career source status updated successfully",
                careerSourceService.updateStatus(user, id, body.active()), request.getRequestURI());
    }

    @PostMapping("/{id}/scan")
    @Operation(summary = "Fetch a career page and ingest its discovered job postings")
    ApiResponse<CareerSourceScanResponse> scan(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        return ApiResponse.success("Career page scanned successfully",
                careerPageScanService.scan(user, id), request.getRequestURI());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a career page")
    ApiResponse<Map<String, String>> delete(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        careerSourceService.delete(user, id);
        return ApiResponse.success("Career source deleted successfully",
                Map.of("status", "deleted"), request.getRequestURI());
    }
}
