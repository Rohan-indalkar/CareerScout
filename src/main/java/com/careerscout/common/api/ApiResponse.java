package com.careerscout.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Map<String, String> errors,
        Instant timestamp,
        String path) {

    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), path);
    }

    public static ApiResponse<Void> failure(String message, Map<String, String> errors, String path) {
        return new ApiResponse<>(false, message, null, errors, Instant.now(), path);
    }
}
