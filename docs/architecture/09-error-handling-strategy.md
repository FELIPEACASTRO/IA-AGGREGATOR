# Section 9: Error Handling Strategy

---

## 9.1 Domain Exception Hierarchy

All exceptions extend a common base to enable unified handling. Domain exceptions are framework-free (no Spring dependencies in the domain layer).

```java
// common/exception/BaseException.java
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    public BaseException with(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Map<String, Object> getContext() { return Map.copyOf(context); }
}

// common/exception/BusinessException.java
// For business rule violations (4xx responses)
public class BusinessException extends BaseException {
    public BusinessException(ErrorCode code, String message) {
        super(code, message);
    }
}

// common/exception/TechnicalException.java
// For infrastructure/technical failures (5xx responses)
public class TechnicalException extends BaseException {
    public TechnicalException(ErrorCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

// common/exception/ErrorCode.java
public enum ErrorCode {
    // === Auth (AUTH_xxx) ===
    AUTH_INVALID_CREDENTIALS("AUTH_001", "Invalid credentials", 401),
    AUTH_TOKEN_EXPIRED("AUTH_002", "Access token expired", 401),
    AUTH_TOKEN_INVALID("AUTH_003", "Invalid token", 401),
    AUTH_REFRESH_TOKEN_INVALID("AUTH_004", "Invalid or revoked refresh token", 401),
    AUTH_INSUFFICIENT_PERMISSIONS("AUTH_005", "Insufficient permissions", 403),
    AUTH_USER_NOT_FOUND("AUTH_006", "User not found", 404),
    AUTH_USER_ALREADY_EXISTS("AUTH_007", "User with this email already exists", 409),
    AUTH_ACCOUNT_DISABLED("AUTH_008", "Account has been disabled", 403),
    AUTH_OAUTH_FAILED("AUTH_009", "OAuth authentication failed", 401),

    // === Billing (BILL_xxx) ===
    BILL_INSUFFICIENT_CREDITS("BILL_001", "Insufficient credits", 402),
    BILL_CREDIT_ACCOUNT_NOT_FOUND("BILL_002", "Credit account not found", 404),
    BILL_PAYMENT_FAILED("BILL_003", "Payment processing failed", 402),
    BILL_SUBSCRIPTION_NOT_FOUND("BILL_004", "Subscription not found", 404),
    BILL_PLAN_NOT_FOUND("BILL_005", "Plan not found", 404),
    BILL_INVALID_PAYMENT_METHOD("BILL_006", "Invalid payment method", 400),
    BILL_SUBSCRIPTION_ALREADY_ACTIVE("BILL_007", "Active subscription already exists", 409),
    BILL_INVOICE_NOT_FOUND("BILL_008", "Invoice not found", 404),
    BILL_WEBHOOK_SIGNATURE_INVALID("BILL_009", "Invalid webhook signature", 400),

    // === Chat (CHAT_xxx) ===
    CHAT_CONVERSATION_NOT_FOUND("CHAT_001", "Conversation not found", 404),
    CHAT_MESSAGE_TOO_LONG("CHAT_002", "Message exceeds maximum length", 400),
    CHAT_CONVERSATION_ARCHIVED("CHAT_003", "Conversation is archived", 409),
    CHAT_CONTENT_POLICY_VIOLATION("CHAT_004", "Message violates content policy", 400),

    // === AI Gateway (AI_xxx) ===
    AI_MODEL_NOT_FOUND("AI_001", "AI model not found", 404),
    AI_MODEL_UNAVAILABLE("AI_002", "AI model temporarily unavailable", 503),
    AI_PROVIDER_ERROR("AI_003", "AI provider returned an error", 502),
    AI_NO_SUITABLE_MODEL("AI_004", "No model matches the routing criteria", 404),
    AI_CONTEXT_TOO_LONG("AI_005", "Input exceeds model context window", 400),
    AI_PROVIDER_TIMEOUT("AI_006", "AI provider request timed out", 504),
    AI_COST_NOT_CONFIGURED("AI_007", "Model credit cost not configured", 500),

    // === Partners (PART_xxx) ===
    PART_NOT_FOUND("PART_001", "Partner not found", 404),
    PART_NOT_APPROVED("PART_002", "Partner account not yet approved", 403),
    PART_COUPON_NOT_FOUND("PART_003", "Coupon not found", 404),
    PART_COUPON_EXPIRED("PART_004", "Coupon has expired or is no longer valid", 410),
    PART_COUPON_LIMIT_REACHED("PART_005", "Coupon redemption limit reached", 410),
    PART_MINIMUM_PAYOUT_NOT_REACHED("PART_006", "Minimum payout threshold not reached", 400),
    PART_DUPLICATE_COUPON_CODE("PART_007", "Coupon code already exists", 409),

    // === Teams (TEAM_xxx) ===
    TEAM_NOT_FOUND("TEAM_001", "Team not found", 404),
    TEAM_CAPACITY_EXCEEDED("TEAM_002", "Team member limit reached", 400),
    TEAM_MEMBER_ALREADY_EXISTS("TEAM_003", "User is already a team member", 409),
    TEAM_CANNOT_REMOVE_OWNER("TEAM_004", "Cannot remove team owner", 400),
    TEAM_INSUFFICIENT_POOL_CREDITS("TEAM_005", "Insufficient credits in team pool", 400),
    TEAM_INVITATION_EXPIRED("TEAM_006", "Team invitation has expired", 410),

    // === Content (CONT_xxx) ===
    CONT_TEMPLATE_NOT_FOUND("CONT_001", "Template not found", 404),
    CONT_CATEGORY_NOT_FOUND("CONT_002", "Category not found", 404),
    CONT_DUPLICATE_SLUG("CONT_003", "Template slug already exists", 409),

    // === General (GEN_xxx) ===
    GEN_VALIDATION_ERROR("GEN_001", "Request validation failed", 400),
    GEN_RATE_LIMIT_EXCEEDED("GEN_002", "Rate limit exceeded", 429),
    GEN_INTERNAL_ERROR("GEN_003", "Internal server error", 500),
    GEN_SERVICE_UNAVAILABLE("GEN_004", "Service temporarily unavailable", 503),
    GEN_FRAUD_DETECTED("GEN_005", "Transaction flagged for security review", 403);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    ErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
    public int getHttpStatus() { return httpStatus; }
}
```

### Concrete Domain Exceptions

```java
// domain/billing/exception/InsufficientCreditsException.java
public class InsufficientCreditsException extends BusinessException {
    public InsufficientCreditsException(Credits available, Credits required) {
        super(ErrorCode.BILL_INSUFFICIENT_CREDITS,
            String.format("Insufficient credits. Available: %d, Required: %d",
                available.amount(), required.amount()));
        with("availableCredits", available.amount());
        with("requiredCredits", required.amount());
    }
}

// domain/partners/exception/CouponExpiredException.java
public class CouponExpiredException extends BusinessException {
    public CouponExpiredException(CouponCode code) {
        super(ErrorCode.PART_COUPON_EXPIRED,
            String.format("Coupon '%s' has expired or reached its redemption limit",
                code.value()));
        with("couponCode", code.value());
    }
}

// domain/aigateway/exception/NoSuitableModelException.java
public class NoSuitableModelException extends BusinessException {
    public NoSuitableModelException(RoutingCriteria criteria) {
        super(ErrorCode.AI_NO_SUITABLE_MODEL,
            "No AI model matches the given routing criteria");
        with("requiredCapabilities", criteria.requiredCapabilities());
        with("routingStrategy", criteria.toString());
    }
}

// domain/aigateway/exception/ProviderUnavailableException.java
public class ProviderUnavailableException extends TechnicalException {
    public ProviderUnavailableException(String provider, Throwable cause) {
        super(ErrorCode.AI_MODEL_UNAVAILABLE,
            String.format("AI provider '%s' is temporarily unavailable", provider), cause);
        with("provider", provider);
    }
}
```

---

## 9.2 Global Exception Handler

```java
// presentation/shared/advice/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // === Business Exceptions (4xx) ===

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        var errorCode = ex.getErrorCode();

        log.warn("Business error [{}]: {} | Context: {} | RequestId: {}",
            errorCode.getCode(), ex.getMessage(), ex.getContext(),
            request.getHeader("X-Request-Id"));

        var response = new ApiErrorResponse(
            false,
            new ApiError(
                errorCode.getCode(),
                ex.getMessage(),
                null,
                null
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(response);
    }

    // === Technical Exceptions (5xx) ===

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicalException(
            TechnicalException ex, HttpServletRequest request) {
        var errorCode = ex.getErrorCode();

        // Log full stack trace for technical errors
        log.error("Technical error [{}]: {} | Context: {}",
            errorCode.getCode(), ex.getMessage(), ex.getContext(), ex);

        var response = new ApiErrorResponse(
            false,
            new ApiError(
                errorCode.getCode(),
                errorCode.getDefaultMessage(),  // Don't expose internal details
                null,
                null
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(response);
    }

    // === Validation Errors (400) ===

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(
                fe.getField(),
                fe.getDefaultMessage(),
                fe.getRejectedValue()
            ))
            .toList();

        log.warn("Validation error: {} field errors", fieldErrors.size());

        var response = new ApiErrorResponse(
            false,
            new ApiError(
                ErrorCode.GEN_VALIDATION_ERROR.getCode(),
                "Request validation failed",
                String.format("%d field(s) have validation errors", fieldErrors.size()),
                fieldErrors
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        var fieldErrors = ex.getConstraintViolations().stream()
            .map(v -> new FieldError(
                extractFieldName(v.getPropertyPath()),
                v.getMessage(),
                v.getInvalidValue()
            ))
            .toList();

        var response = new ApiErrorResponse(
            false,
            new ApiError(
                ErrorCode.GEN_VALIDATION_ERROR.getCode(),
                "Constraint validation failed",
                null,
                fieldErrors
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity.badRequest().body(response);
    }

    // === Spring Security Exceptions ===

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        var response = new ApiErrorResponse(
            false,
            new ApiError(
                ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS.getCode(),
                "You do not have permission to perform this action",
                null, null
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity.status(403).body(response);
    }

    // === External Service Failures ===

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<ApiErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex) {
        log.error("Circuit breaker open: {}", ex.getMessage());

        var response = new ApiErrorResponse(
            false,
            new ApiError(
                ErrorCode.GEN_SERVICE_UNAVAILABLE.getCode(),
                "External service is temporarily unavailable. Please try again later.",
                null, null
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity.status(503)
            .header("Retry-After", "30")
            .body(response);
    }

    // === Catch-All (500) ===

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        // Log full details for unexpected errors
        log.error("Unexpected error", ex);

        var response = new ApiErrorResponse(
            false,
            new ApiError(
                ErrorCode.GEN_INTERNAL_ERROR.getCode(),
                "An unexpected error occurred. Please contact support if the problem persists.",
                null, null
            ),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity
            .status(500)
            .body(response);
    }

    // === HTTP-Level Errors ===

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {
        var response = new ApiErrorResponse(
            false,
            new ApiError("GEN_METHOD_NOT_ALLOWED",
                String.format("Method '%s' is not supported for this endpoint", ex.getMethod()),
                "Supported methods: " + String.join(", ",
                    Objects.requireNonNull(ex.getSupportedMethods())),
                null),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity.status(405).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex) {
        var response = new ApiErrorResponse(
            false,
            new ApiError("GEN_UNSUPPORTED_MEDIA_TYPE",
                "Content type not supported",
                "Supported: " + ex.getSupportedMediaTypes(), null),
            Instant.now().toString(),
            MDC.get("requestId")
        );

        return ResponseEntity.status(415).body(response);
    }

    private String extractFieldName(Path propertyPath) {
        String fullPath = propertyPath.toString();
        int lastDot = fullPath.lastIndexOf('.');
        return lastDot > 0 ? fullPath.substring(lastDot + 1) : fullPath;
    }
}
```

---

## 9.3 API Error Response Format

### Standard Error Shape

Every error response follows the same JSON structure:

```json
{
  "success": false,
  "error": {
    "code": "BILL_001",
    "message": "Insufficient credits. Available: 42, Required: 150",
    "detail": "Optional additional context for the developer",
    "fieldErrors": null
  },
  "timestamp": "2026-03-01T10:30:00Z",
  "requestId": "req-550e8400-e29b"
}
```

### Error Codes Follow a Convention

```
Format: MODULE_NNN

Modules:
  AUTH_ = Authentication/Authorization
  BILL_ = Billing/Credits
  CHAT_ = Chat/Conversations
  AI_   = AI Gateway/Models
  PART_ = Partners/Coupons
  TEAM_ = Teams
  CONT_ = Content/Templates
  GEN_  = General/Cross-cutting

HTTP Status Code Mapping:
  400 = Validation error, bad input
  401 = Authentication required or failed
  402 = Payment required (insufficient credits)
  403 = Authorization denied
  404 = Resource not found
  409 = Conflict (duplicate, already exists)
  410 = Gone (expired coupon, expired invitation)
  429 = Rate limit exceeded
  500 = Internal server error
  502 = Bad gateway (AI provider error)
  503 = Service unavailable (circuit breaker open)
  504 = Gateway timeout (AI provider timeout)
```

---

## 9.4 Retry Policies for External APIs

```java
// infrastructure/config/RetryPoliciesConfig.java
@Configuration
public class RetryPoliciesConfig {

    /**
     * AI Provider retry policy:
     * - 3 attempts with exponential backoff
     * - Retry on: timeout, 429 (rate limit), 500, 502, 503
     * - Do NOT retry on: 400, 401, 403, 404 (client errors)
     */
    @Bean
    public RetryRegistry aiProviderRetryRegistry() {
        var config = RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofMillis(500),  // initial wait
                2.0                       // multiplier
            ))
            .retryOnException(ex -> {
                if (ex instanceof WebClientResponseException wcre) {
                    int status = wcre.getStatusCode().value();
                    return status == 429 || status >= 500;
                }
                return ex instanceof TimeoutException
                    || ex instanceof ConnectException
                    || ex instanceof IOException;
            })
            .ignoreExceptions(
                BusinessException.class,
                IllegalArgumentException.class
            )
            .failAfterMaxAttempts(true)
            .build();

        return RetryRegistry.of(config);
    }

    /**
     * Payment provider retry policy:
     * - 2 attempts only (payments are sensitive)
     * - Retry on: timeout, network errors ONLY
     * - Do NOT retry on: any HTTP response (payment may have been processed)
     */
    @Bean
    public RetryRegistry paymentRetryRegistry() {
        var config = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(1))
            .retryOnException(ex ->
                ex instanceof TimeoutException || ex instanceof ConnectException)
            .ignoreExceptions(
                WebClientResponseException.class,  // Don't retry if we got a response
                BusinessException.class
            )
            .build();

        return RetryRegistry.of(config);
    }
}

// Retry with backoff for AI providers
// Retry timeline: attempt 1 -> wait 500ms -> attempt 2 -> wait 1000ms -> attempt 3 -> fail

// Retry with idempotency key for payments
// infrastructure/billing/adapter/StripePaymentAdapter.java
@Component
public class StripePaymentAdapter implements PaymentGateway {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Idempotency key ensures retries don't create duplicate charges
        var requestOptions = RequestOptions.builder()
            .setIdempotencyKey(request.idempotencyKey())
            .build();

        try {
            var intent = PaymentIntent.create(buildParams(request), requestOptions);
            return mapResult(intent);
        } catch (StripeException e) {
            if (isRetryable(e)) {
                throw new RetryablePaymentException(e);
            }
            throw new PaymentProcessingException("stripe", e.getMessage(), e);
        }
    }

    private boolean isRetryable(StripeException e) {
        // Only network-level errors are retryable
        return e instanceof ApiConnectionException;
    }
}
```

### Retry Summary by Service

| Service | Max Attempts | Backoff | Retry On | Idempotency |
|---------|-------------|---------|----------|-------------|
| OpenRouter | 3 | Exponential (500ms, 1s, 2s) | Timeout, 429, 5xx | Request ID |
| OpenAI Direct | 3 | Exponential (500ms, 1s, 2s) | Timeout, 429, 5xx | Request ID |
| Anthropic Direct | 3 | Exponential (500ms, 1s, 2s) | Timeout, 429, 529 | Request ID |
| Stripe | 2 | Fixed (1s) | Network errors only | Idempotency Key |
| Asaas | 2 | Fixed (1s) | Network errors only | Idempotency Key |
| Email (SMTP) | 3 | Fixed (2s) | Timeout, connection refused | N/A |
| Redis | 2 | Fixed (100ms) | Connection lost | N/A |

### Dead Letter Queue for Failed Operations

```java
// For operations that fail after all retries, store in a dead letter table
// for manual review and re-processing.

@Entity
@Table(name = "dead_letter_queue")
public class DeadLetterEntry {
    @Id
    private UUID id;
    private String operationType;     // "PAYMENT", "AI_COMPLETION", "COMMISSION"
    private String payload;           // JSON of original request
    private String errorMessage;
    private String stackTrace;
    private int attemptCount;
    private LocalDateTime firstAttempt;
    private LocalDateTime lastAttempt;
    private boolean resolved;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolution;        // "RETRIED", "SKIPPED", "MANUAL_FIX"
}
```
