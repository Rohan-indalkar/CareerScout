package com.careerscout.profile.dto;

import com.careerscout.profile.ExperienceLevel;

import java.time.Instant;
import java.util.List;

public record SearchProfileResponse(
        Long id,
        String name,
        String position,
        String location,
        ExperienceLevel experienceLevel,
        List<String> skills,
        List<String> keywords,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
