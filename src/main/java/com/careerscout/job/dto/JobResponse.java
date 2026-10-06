package com.careerscout.job.dto;

import java.time.Instant;
import java.util.List;

public record JobResponse(
        Long id,
        Long careerSourceId,
        String externalJobId,
        String title,
        String normalizedTitle,
        String companyName,
        String location,
        String experience,
        String description,
        String jobUrl,
        List<String> skills,
        String contentHash,
        Instant firstSeenAt,
        Instant lastSeenAt,
        boolean active) {
}
