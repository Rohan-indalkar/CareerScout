package com.careerscout.user.dto;

import com.careerscout.user.Role;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, Instant createdAt, Instant updatedAt) {
}
