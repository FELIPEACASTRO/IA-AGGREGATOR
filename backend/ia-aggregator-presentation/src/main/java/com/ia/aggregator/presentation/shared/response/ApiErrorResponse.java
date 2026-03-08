package com.ia.aggregator.presentation.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response wrapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final List<FieldError> errors;
    private final String requestId;
    private final Instant timestamp;

    public ApiErrorResponse(boolean success, String errorCode, String message,
                             List<FieldError> errors, String requestId, Instant timestamp) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.errors = errors;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() { return success; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public List<FieldError> getErrors() { return errors; }
    public String getRequestId() { return requestId; }
    public Instant getTimestamp() { return timestamp; }

    public static ApiErrorResponse of(String errorCode, String message) {
        return new ApiErrorResponse(false, errorCode, message, null, null, Instant.now());
    }

    public static ApiErrorResponse of(String errorCode, String message, String requestId) {
        return new ApiErrorResponse(false, errorCode, message, null, requestId, Instant.now());
    }

    public static ApiErrorResponse validation(String message, List<FieldError> fieldErrors) {
        return new ApiErrorResponse(false, "VALIDATION_ERROR", message, fieldErrors, null, Instant.now());
    }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}
