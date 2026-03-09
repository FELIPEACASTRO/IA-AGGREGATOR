package com.ia.aggregator.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Content output filter that scans LLM responses for blocked content.
 *
 * <p>Blocks responses containing competitor names, confidential info,
 * or toxic content based on admin-configurable keyword/regex lists.
 */
@Component
public class ContentOutputFilter {

    private static final Logger log = LoggerFactory.getLogger(ContentOutputFilter.class);

    private final boolean enabled;
    private final String action; // "block" or "redact"
    private final List<Pattern> blockedPatterns;

    public ContentOutputFilter(
            @Value("${app.ai.guardrails.content-filter.enabled:true}") boolean enabled,
            @Value("${app.ai.guardrails.content-filter.action:block}") String action,
            @Value("${app.ai.guardrails.content-filter.blocked-keywords:}") List<String> blockedKeywords,
            @Value("${app.ai.guardrails.content-filter.blocked-regex:}") List<String> blockedRegex) {
        this.enabled = enabled;
        this.action = action;
        this.blockedPatterns = buildPatterns(blockedKeywords, blockedRegex);
    }

    /**
     * Validate output content against blocked patterns.
     *
     * @param content the LLM response to validate
     * @return the validated content (may be modified if action is "redact")
     * @throws ContentBlockedException if action is "block" and blocked content is found
     */
    public String validate(String content) {
        if (!enabled || content == null || content.isEmpty()) {
            return content;
        }

        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(content).find()) {
                if ("block".equalsIgnoreCase(action)) {
                    log.warn("Blocked content detected in LLM output");
                    throw new ContentBlockedException("Response contains restricted content");
                }
                content = pattern.matcher(content).replaceAll("[FILTERED]");
            }
        }

        return content;
    }

    private List<Pattern> buildPatterns(List<String> keywords, List<String> regexPatterns) {
        List<Pattern> patterns = new ArrayList<>();

        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank()) {
                    patterns.add(Pattern.compile("(?i)\\b" + Pattern.quote(keyword.trim()) + "\\b"));
                }
            }
        }

        if (regexPatterns != null) {
            for (String regex : regexPatterns) {
                if (regex != null && !regex.isBlank()) {
                    try {
                        patterns.add(Pattern.compile(regex));
                    } catch (Exception e) {
                        log.warn("Invalid content filter regex: {}", regex);
                    }
                }
            }
        }

        return patterns;
    }

    public static class ContentBlockedException extends RuntimeException {
        public ContentBlockedException(String message) {
            super(message);
        }
    }
}
