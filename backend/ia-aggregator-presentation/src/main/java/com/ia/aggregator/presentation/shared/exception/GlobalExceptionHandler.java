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
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business error: [{}] {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapBusinessErrorToStatus(ex.getErrorCode().getCode());

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(ex.getErrorCode().getCode(), ex.getMessage()));
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicalException(TechnicalException ex) {
        log.error("Technical error: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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

    private HttpStatus mapBusinessErrorToStatus(String errorCode) {
        if (errorCode == null) return HttpStatus.BAD_REQUEST;

        if (errorCode.startsWith("AUTH_001") || errorCode.startsWith("AUTH_002")
                || errorCode.startsWith("AUTH_003")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (errorCode.startsWith("AUTH_004") || errorCode.startsWith("AUTH_005")) {
            return HttpStatus.FORBIDDEN;
        }
        if (errorCode.contains("NOT_FOUND")) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode.contains("DUPLICATE") || errorCode.contains("ALREADY_EXISTS")) {
            return HttpStatus.CONFLICT;
        }
        if (errorCode.startsWith("BILL_005") || errorCode.startsWith("BILL_006")) {
            return HttpStatus.PAYMENT_REQUIRED;
        }

        return HttpStatus.BAD_REQUEST;
    }
}
