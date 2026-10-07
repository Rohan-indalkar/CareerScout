package com.careerscout.career.dto;

import java.time.Instant;

public record CareerSourceScanResponse(
        Long careerSourceId,
        int discoveredJobs,
        int createdJobs,
        int updatedJobs,
        int unchangedJobs,
        Instant scannedAt) {
}
