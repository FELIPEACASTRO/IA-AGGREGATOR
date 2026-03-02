package com.ia.aggregator.presentation.shared.exception;

import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.presentation.shared.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── BusinessException ────────────────────────────────────────────────

    @Test
    void handleBusinessException_usesErrorCodeHttpStatus() {
        BusinessException ex = new BusinessException(ErrorCode.AUTH_003, "Email exists");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("AUTH_003", response.getBody().getErrorCode());
        assertEquals("Email exists", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void handleBusinessException_withoutDetail_usesDefaultMessage() {
        BusinessException ex = new BusinessException(ErrorCode.AUTH_001);

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("AUTH_001", response.getBody().getErrorCode());
        // detail is null → should fall back to getMessage() = defaultMessage
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void handleBusinessException_withDetail_usesDetail() {
        BusinessException ex = new BusinessException(ErrorCode.AUTH_001, "Custom detail msg");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Custom detail msg", response.getBody().getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"AUTH_003", "AUTH_005", "BILL_001", "GEN_003"})
    void handleBusinessException_mapsVariousHttpStatuses(ErrorCode code) {
        BusinessException ex = new BusinessException(code);

        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(ex);

        assertEquals(code.getHttpStatus(), response.getStatusCode().value());
        assertEquals(code.getCode(), response.getBody().getErrorCode());
    }

    // ── TechnicalException ───────────────────────────────────────────────

    @Test
    void handleTechnicalException_returnsGenericMessage() {
        TechnicalException ex = new TechnicalException(ErrorCode.GEN_001, "DB connection lost");

        ResponseEntity<ApiErrorResponse> response = handler.handleTechnicalException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("GEN_001", response.getBody().getErrorCode());
        // Should NOT expose internal detail
        assertEquals("An internal error occurred", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void handleTechnicalException_usesErrorCodeHttpStatus() {
        TechnicalException ex = new TechnicalException(ErrorCode.GEN_005, "service down");

        ResponseEntity<ApiErrorResponse> response = handler.handleTechnicalException(ex);

        assertEquals(503, response.getStatusCode().value());
    }

    // ── Validation ───────────────────────────────────────────────────────

    @Test
    void handleValidation_shouldReturnFieldErrors() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "registerUserCommand");
        bindingResult.addError(new FieldError("registerUserCommand", "email", "must not be blank"));
        bindingResult.addError(new FieldError("registerUserCommand", "password", "size must be between 8 and 128"));

        // Get any method to build MethodParameter (not actually used)
        Method method = this.getClass().getDeclaredMethod("handleValidation_shouldReturnFieldErrors");
        MethodParameter param = new MethodParameter(method, -1);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getErrors());
        assertEquals(2, response.getBody().getErrors().size());

        // Verify field errors
        assertTrue(response.getBody().getErrors().stream()
                .anyMatch(e -> "email".equals(e.getField()) && "must not be blank".equals(e.getMessage())));
        assertTrue(response.getBody().getErrors().stream()
                .anyMatch(e -> "password".equals(e.getField())));
    }

    // ── Unexpected Exception ─────────────────────────────────────────────

    @Test
    void handleUnexpected_shouldReturn500WithGenericMessage() {
        Exception ex = new NullPointerException("something broke");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("GEN_001", response.getBody().getErrorCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }
}
