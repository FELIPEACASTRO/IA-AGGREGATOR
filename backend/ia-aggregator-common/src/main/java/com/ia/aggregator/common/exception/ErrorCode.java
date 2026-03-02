package com.ia.aggregator.common.exception;

/**
 * Error codes for the platform following the pattern: MODULE_NNN
 */
public enum ErrorCode {

    // AUTH (001-009)
    AUTH_001("AUTH_001", "Invalid credentials", 401),
    AUTH_002("AUTH_002", "Account locked", 423),
    AUTH_003("AUTH_003", "Email already registered", 409),
    AUTH_004("AUTH_004", "Invalid or expired token", 401),
    AUTH_005("AUTH_005", "Insufficient permissions", 403),
    AUTH_006("AUTH_006", "Account not verified", 403),
    AUTH_007("AUTH_007", "OAuth provider error", 502),
    AUTH_008("AUTH_008", "Session expired", 401),
    AUTH_009("AUTH_009", "Password does not meet requirements", 422),

    // BILLING (001-009)
    BILL_001("BILL_001", "Insufficient credits", 402),
    BILL_002("BILL_002", "Plan not found", 404),
    BILL_003("BILL_003", "Subscription already active", 409),
    BILL_004("BILL_004", "Payment processing failed", 502),
    BILL_005("BILL_005", "Invalid coupon code", 422),
    BILL_006("BILL_006", "Coupon expired", 410),
    BILL_007("BILL_007", "Credit transaction failed", 500),
    BILL_008("BILL_008", "Plan upgrade required", 403),
    BILL_009("BILL_009", "Invalid webhook signature", 401),

    // CHAT (001-004)
    CHAT_001("CHAT_001", "Conversation not found", 404),
    CHAT_002("CHAT_002", "Message limit exceeded", 429),
    CHAT_003("CHAT_003", "Invalid model for plan", 403),
    CHAT_004("CHAT_004", "Conversation archived", 410),

    // AI GATEWAY (001-007)
    AI_001("AI_001", "No suitable model found", 404),
    AI_002("AI_002", "Provider unavailable", 503),
    AI_003("AI_003", "Model rate limit exceeded", 429),
    AI_004("AI_004", "Context window exceeded", 413),
    AI_005("AI_005", "Streaming connection failed", 502),
    AI_006("AI_006", "Model deprecated", 410),
    AI_007("AI_007", "Invalid model configuration", 422),

    // PARTNERS (001-007)
    PART_001("PART_001", "Partner not found", 404),
    PART_002("PART_002", "Commission calculation failed", 500),
    PART_003("PART_003", "Payout minimum not reached", 422),
    PART_004("PART_004", "Fraud detected", 403),
    PART_005("PART_005", "Coupon usage limit reached", 422),
    PART_006("PART_006", "Invalid referral link", 404),
    PART_007("PART_007", "Partner application pending", 409),

    // TEAMS (001-006)
    TEAM_001("TEAM_001", "Team not found", 404),
    TEAM_002("TEAM_002", "Member limit exceeded", 422),
    TEAM_003("TEAM_003", "Invalid team role", 422),
    TEAM_004("TEAM_004", "Invitation expired", 410),
    TEAM_005("TEAM_005", "Already a team member", 409),
    TEAM_006("TEAM_006", "Team credit pool depleted", 402),

    // CONTENT (001-003)
    CONT_001("CONT_001", "Template not found", 404),
    CONT_002("CONT_002", "Document processing failed", 500),
    CONT_003("CONT_003", "Knowledge base limit exceeded", 422),

    // GENERAL (001-007)
    GEN_001("GEN_001", "Internal server error", 500),
    GEN_002("GEN_002", "Validation failed", 422),
    GEN_003("GEN_003", "Resource not found", 404),
    GEN_004("GEN_004", "Rate limit exceeded", 429),
    GEN_005("GEN_005", "Service unavailable", 503),
    GEN_006("GEN_006", "Method not allowed", 405),
    GEN_007("GEN_007", "Unsupported media type", 415);

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
