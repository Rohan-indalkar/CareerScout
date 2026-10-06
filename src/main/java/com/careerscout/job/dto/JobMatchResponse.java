package com.careerscout.job.dto;

import java.time.Instant;

public record JobMatchResponse(
        Long id,
        JobResponse job,
        Long searchProfileId,
        String searchProfileName,
        int matchScore,
        boolean matched,
        boolean positionMatched,
        boolean locationMatched,
        boolean experienceMatched,
        boolean skillsMatched,
        boolean keywordMatched,
        String matchExplanation,
        String rejectionReason,
        Instant matchedAt) {
}
