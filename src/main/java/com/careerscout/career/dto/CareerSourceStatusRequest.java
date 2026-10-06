package com.careerscout.career.dto;

import jakarta.validation.constraints.NotNull;

public record CareerSourceStatusRequest(@NotNull Boolean active) {
}
