package com.careerscout.health;

import com.careerscout.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Application health endpoints")
public class HealthController {
    @GetMapping
    @Operation(summary = "Check API availability")
    ApiResponse<Map<String, String>> health(HttpServletRequest request) {
        return ApiResponse.success("CareerScout API is running", Map.of("status", "UP"),
                request.getRequestURI());
    }
}
