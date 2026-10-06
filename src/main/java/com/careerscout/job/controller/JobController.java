package com.careerscout.job.controller;

import com.careerscout.common.api.ApiResponse;
import com.careerscout.common.api.PaginatedApiResponse;
import com.careerscout.job.dto.JobMatchResponse;
import com.careerscout.job.dto.JobResponse;
import com.careerscout.job.dto.PageMetadata;
import com.careerscout.job.service.JobService;
import com.careerscout.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "List active jobs visible to the authenticated user")
    PaginatedApiResponse<JobResponse> findJobs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) @Min(0) @Max(100) Integer minScore,
            @RequestParam(required = false) @Min(1) Long careerSourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            HttpServletRequest request) {
        var result = jobService.findJobs(user, company, position, location, experience, skill, matched,
                minScore, careerSourceId, dateFrom, dateTo, page, size, false);
        return PaginatedApiResponse.success("Jobs fetched successfully", result.getContent(),
                metadata(result), request.getRequestURI());
    }

    @GetMapping("/new")
    @Operation(summary = "List newest discovered active jobs first")
    PaginatedApiResponse<JobResponse> findNewest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) @Min(0) @Max(100) Integer minScore,
            @RequestParam(required = false) @Min(1) Long careerSourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            HttpServletRequest request) {
        var result = jobService.findJobs(user, company, position, location, experience, skill, matched,
                minScore, careerSourceId, dateFrom, dateTo, page, size, true);
        return PaginatedApiResponse.success("Newest jobs fetched successfully", result.getContent(),
                metadata(result), request.getRequestURI());
    }

    @GetMapping("/matches")
    @Operation(summary = "List matched jobs with their profile-specific match details")
    PaginatedApiResponse<JobMatchResponse> findMatches(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) @Min(0) @Max(100) Integer minScore,
            @RequestParam(required = false) @Min(1) Long careerSourceId,
            @RequestParam(required = false) @Min(1) Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            HttpServletRequest request) {
        var result = jobService.findMatches(user, company, position, location, experience, minScore,
                careerSourceId, profileId, dateFrom, dateTo, page, size);
        return PaginatedApiResponse.success("Matched jobs fetched successfully", result.getContent(),
                metadata(result), request.getRequestURI());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active job belonging to one of the user's career sources")
    ApiResponse<JobResponse> findById(
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest request) {
        return ApiResponse.success("Job fetched successfully", jobService.findById(user, id),
                request.getRequestURI());
    }

    private static PageMetadata metadata(org.springframework.data.domain.Page<?> page) {
        return new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
