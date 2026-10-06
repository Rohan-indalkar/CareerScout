package com.careerscout.profile.dto;

import jakarta.validation.constraints.NotNull;

public record SearchProfileStatusRequest(@NotNull Boolean active) {
}
