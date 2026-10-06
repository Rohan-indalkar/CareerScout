package com.careerscout.career.dto;

import java.time.Instant;

public record CareerSourceResponse(
        Long id,
        String companyName,
        String careerUrl,
        boolean active,
        int scanIntervalMinutes,
        Instant lastScannedAt,
        Instant createdAt,
        Instant updatedAt) {
}
