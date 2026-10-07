package com.careerscout.common.exception;

import com.careerscout.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return response(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", Map.of(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "You do not have permission to access this resource",
                Map.of(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(UpstreamServiceException.class)
    ResponseEntity<ApiResponse<Void>> handleUpstreamFailure(
            UpstreamServiceException exception, HttpServletRequest request) {
        logger.warn("Career page scan failed for {}: {}", request.getRequestURI(), exception.getMessage());
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiResponse<Void>> handleConflict(Exception exception, HttpServletRequest request) {
        String message = exception instanceof DuplicateResourceException
                ? exception.getMessage()
                : "The request conflicts with existing data";
        return response(HttpStatus.CONFLICT, message, Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        logger.error("Unexpected API error for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of(), request);
    }

    private ResponseEntity<ApiResponse<Void>> response(
            HttpStatus status, String message, Map<String, String> errors, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(message, errors, request.getRequestURI()));
    }
}
