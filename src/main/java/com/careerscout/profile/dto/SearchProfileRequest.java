package com.careerscout.profile.dto;

import com.careerscout.profile.ExperienceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SearchProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 150) String position,
        @NotBlank @Size(max = 150) String location,
        @NotNull ExperienceLevel experienceLevel,
        @Size(max = 30) List<@NotBlank @Size(max = 100) String> skills,
        @Size(max = 30) List<@NotBlank @Size(max = 100) String> keywords) {
}
