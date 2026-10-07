package com.careerscout.job.service;

public record JobMatchEvaluation(
        int score,
        boolean matched,
        boolean positionMatched,
        boolean locationMatched,
        boolean experienceMatched,
        boolean skillsMatched,
        boolean keywordMatched,
        String explanation,
        String rejectionReason) {
}
