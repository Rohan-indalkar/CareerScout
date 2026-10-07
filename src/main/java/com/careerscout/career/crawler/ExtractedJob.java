package com.careerscout.career.crawler;

import java.util.Set;

public record ExtractedJob(
        String externalJobId,
        String title,
        String normalizedTitle,
        String companyName,
        String location,
        String experience,
        String description,
        String jobUrl,
        Set<String> skills,
        String contentHash) {
}
