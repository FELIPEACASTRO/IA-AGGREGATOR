package com.ia.aggregator.presentation.shared.exception;

import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.presentation.shared.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler that converts domain/business exceptions
 * to standardized API error responses.
 * Uses ErrorCode.httpStatus directly for correct HTTP status mapping.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        String message = ex.getDetail() != null ? ex.getDetail() : ex.getMessage();
        log.warn("Business error: [{}] {}", ex.getErrorCode().getCode(), message);

        HttpStatus status = HttpStatus.valueOf(ex.getErrorCode().getHttpStatus());

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(ex.getErrorCode().getCode(), message));
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicalException(TechnicalException ex) {
        log.error("Technical error: [{}] {}", ex.getErrorCode().getCode(), ex.getMessage(), ex);

        HttpStatus status = HttpStatus.valueOf(ex.getErrorCode().getHttpStatus());

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(ex.getErrorCode().getCode(), "An internal error occurred"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> new ApiErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validation("Validation failed", fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("GEN_001", "An unexpected error occurred"));
    }
}
