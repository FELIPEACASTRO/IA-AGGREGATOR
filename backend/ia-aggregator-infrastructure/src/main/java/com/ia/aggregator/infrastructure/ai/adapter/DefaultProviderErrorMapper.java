package com.ia.aggregator.infrastructure.ai.adapter;

import com.ia.aggregator.application.ai.port.out.ProviderErrorMapper;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ProviderErrorMapper} that classifies provider
 * HTTP errors into named failure categories for structured logging and alerting.
 *
 * <p>Failure categories (per prompt specification):
 * <ul>
 *   <li>{@code auth} — 401/403 authentication or authorization failure</li>
 *   <li>{@code validation} — 400 bad request / invalid parameters</li>
 *   <li>{@code rate_limit} — 429 too many requests</li>
 *   <li>{@code upstream_4xx} — other 4xx from the provider</li>
 *   <li>{@code upstream_5xx} — 5xx server errors from the provider</li>
 *   <li>{@code timeout} — connect or read timeout (httpStatus=0)</li>
 *   <li>{@code parsing} — response parsing/deserialization failure (httpStatus=-1)</li>
 *   <li>{@code compliance_block} — threat intel blocked by compliance gate (httpStatus=-2)</li>
 * </ul>
 *
 * <p>The {@code failureCategory} appears in structured JSON logs and Micrometer tags.
 *
 * <p>Big O: O(1) per error classification — uses integer comparison.
 */
@Component
public class DefaultProviderErrorMapper implements ProviderErrorMapper {

    /** Sentinel value indicating a timeout (no HTTP status available). */
    public static final int STATUS_TIMEOUT = 0;

    /** Sentinel value indicating a response parsing failure. */
    public static final int STATUS_PARSING_ERROR = -1;

    /** Sentinel value indicating a compliance gate block. */
    public static final int STATUS_COMPLIANCE_BLOCK = -2;

    @Override
    public TechnicalException toTechnicalException(int httpStatus, String responseBody, String providerName) {
        String category = classifyFailureCategory(httpStatus);
        String detail = buildDetail(providerName, httpStatus, responseBody, category);

        return switch (category) {
            case "auth"             -> new TechnicalException(ErrorCode.AUTH_005, detail);
            case "validation"       -> new TechnicalException(ErrorCode.AI_007, detail);
            case "rate_limit"       -> new TechnicalException(ErrorCode.AI_003, detail);
            case "timeout"          -> new TechnicalException(ErrorCode.AI_002, detail);
            case "parsing"          -> new TechnicalException(ErrorCode.AI_002, detail);
            case "compliance_block" -> new TechnicalException(ErrorCode.AI_020, detail);
            case "upstream_4xx"     -> new TechnicalException(ErrorCode.AI_007, detail);
            default                 -> new TechnicalException(ErrorCode.AI_002, detail); // upstream_5xx + fallback
        };
    }

    /**
     * Classifies the HTTP status code (or sentinel) into a failure category string.
     *
     * @param httpStatus HTTP status or sentinel value
     * @return failure category string for structured logs
     */
    public String classifyFailureCategory(int httpStatus) {
        return switch (httpStatus) {
            case STATUS_TIMEOUT         -> "timeout";
            case STATUS_PARSING_ERROR   -> "parsing";
            case STATUS_COMPLIANCE_BLOCK -> "compliance_block";
            case 400                    -> "validation";
            case 401, 403               -> "auth";
            case 429                    -> "rate_limit";
            default -> {
                if (httpStatus >= 400 && httpStatus < 500) yield "upstream_4xx";
                if (httpStatus >= 500)                     yield "upstream_5xx";
                yield "unknown";
            }
        };
    }

    private String buildDetail(String providerName, int httpStatus, String responseBody, String category) {
        String statusStr = httpStatus > 0 ? "HTTP " + httpStatus : category;
        String bodySnippet = responseBody != null && !responseBody.isBlank()
                ? responseBody.substring(0, Math.min(200, responseBody.length()))
                : "(empty body)";
        return providerName + " [" + statusStr + "]: " + bodySnippet;
    }
}
