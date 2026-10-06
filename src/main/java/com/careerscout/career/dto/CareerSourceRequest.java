package com.careerscout.career.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CareerSourceRequest(
        @NotBlank @Size(max = 150) String companyName,
        @NotBlank @Size(max = 2048) String careerUrl,
        @Min(5) @Max(10080) int scanIntervalMinutes) {
}
