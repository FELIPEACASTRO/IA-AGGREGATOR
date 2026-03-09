package com.ia.aggregator.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PII detection and masking filter using configurable regex patterns.
 *
 * <p>Detects and masks: CPF, credit card numbers, email addresses, phone numbers.
 * Admin can add custom patterns via configuration.
 *
 * <p>Actions: MASK replaces with [REDACTED], BLOCK rejects the request.
 */
@Component
public class PiiDetectionFilter {

    private static final Logger log = LoggerFactory.getLogger(PiiDetectionFilter.class);
    private static final String REDACTED = "[REDACTED]";

    private final List<Pattern> piiPatterns;
    private final boolean enabled;
    private final String action; // "mask" or "block"

    public PiiDetectionFilter(
            @Value("${app.ai.guardrails.pii-detection.enabled:true}") boolean enabled,
            @Value("${app.ai.guardrails.pii-detection.action:mask}") String action,
            @Value("${app.ai.guardrails.pii-detection.custom-patterns:}") List<String> customPatterns) {
        this.enabled = enabled;
        this.action = action;
        this.piiPatterns = buildPatterns(customPatterns);
    }

    /**
     * Process text and mask PII if enabled.
     *
     * @param text the input text to scan
     * @return the processed text with PII masked, or the original if disabled
     * @throws PiiBlockedException if action is "block" and PII is detected
     */
    public String process(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (Pattern pattern : piiPatterns) {
            if (pattern.matcher(result).find()) {
                if ("block".equalsIgnoreCase(action)) {
                    log.warn("PII detected in input, blocking request");
                    throw new PiiBlockedException("Input contains personally identifiable information");
                }
                result = pattern.matcher(result).replaceAll(REDACTED);
            }
        }

        if (!result.equals(text)) {
            log.info("PII detected and masked in input");
        }
        return result;
    }

    /**
     * Check if text contains PII without modifying it.
     */
    public boolean containsPii(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return false;
        }
        return piiPatterns.stream().anyMatch(p -> p.matcher(text).find());
    }

    private List<Pattern> buildPatterns(List<String> customPatterns) {
        List<Pattern> patterns = new ArrayList<>();

        // CPF (Brazilian tax ID): 000.000.000-00
        patterns.add(Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}"));

        // Credit card numbers: 4 groups of 4 digits
        patterns.add(Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"));

        // Email addresses
        patterns.add(Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));

        // Phone numbers (Brazilian and international)
        patterns.add(Pattern.compile("\\+?\\d{1,3}[- ]?\\(?\\d{2,3}\\)?[- ]?\\d{4,5}[- ]?\\d{4}"));

        // SSN (US Social Security Number)
        patterns.add(Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));

        // Custom patterns from config
        if (customPatterns != null) {
            for (String custom : customPatterns) {
                if (custom != null && !custom.isBlank()) {
                    try {
                        patterns.add(Pattern.compile(custom));
                    } catch (Exception e) {
                        log.warn("Invalid custom PII pattern: {}", custom);
                    }
                }
            }
        }

        return patterns;
    }

    /**
     * Thrown when PII is detected and action is "block".
     */
    public static class PiiBlockedException extends RuntimeException {
        public PiiBlockedException(String message) {
            super(message);
        }
    }
}
