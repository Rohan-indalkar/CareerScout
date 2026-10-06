package com.careerscout.common.api;

import com.careerscout.job.dto.PageMetadata;

import java.time.Instant;
import java.util.List;

public record PaginatedApiResponse<T>(
        boolean success,
        String message,
        List<T> data,
        PageMetadata pagination,
        Instant timestamp,
        String path) {
    public static <T> PaginatedApiResponse<T> success(
            String message, List<T> data, PageMetadata pagination, String path) {
        return new PaginatedApiResponse<>(true, message, data, pagination, Instant.now(), path);
    }
}
