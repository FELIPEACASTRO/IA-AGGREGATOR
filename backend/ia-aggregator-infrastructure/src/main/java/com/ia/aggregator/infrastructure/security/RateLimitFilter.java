package com.ia.aggregator.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Token Bucket rate limiter backed by Redis.
 *
 * <p>Limits requests per user (authenticated) or per IP (anonymous).
 * Uses a Lua script for atomic token consumption.
 *
 * <p>Returns HTTP 429 with Retry-After header when limit is exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxRequestsPerMinute;
    private final int maxRequestsPerHour;
    private final DefaultRedisScript<Long> tokenBucketScript;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.security.rate-limit.requests-per-minute:60}") int maxRequestsPerMinute,
            @Value("${app.security.rate-limit.requests-per-hour:600}") int maxRequestsPerHour) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxRequestsPerHour = maxRequestsPerHour;
        this.tokenBucketScript = createTokenBucketScript();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String identifier = resolveIdentifier(request);
        String minuteKey = RATE_LIMIT_PREFIX + "min:" + identifier;
        String hourKey = RATE_LIMIT_PREFIX + "hr:" + identifier;

        try {
            // Check per-minute limit
            Long minuteResult = redisTemplate.execute(
                    tokenBucketScript,
                    List.of(minuteKey),
                    String.valueOf(maxRequestsPerMinute),
                    "60"
            );

            if (minuteResult != null && minuteResult == 0L) {
                writeRateLimitResponse(response, 60);
                return;
            }

            // Check per-hour limit
            Long hourResult = redisTemplate.execute(
                    tokenBucketScript,
                    List.of(hourKey),
                    String.valueOf(maxRequestsPerHour),
                    "3600"
            );

            if (hourResult != null && hourResult == 0L) {
                writeRateLimitResponse(response, 3600);
                return;
            }
        } catch (Exception e) {
            // If Redis is down, allow the request (fail-open for availability)
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveIdentifier(HttpServletRequest request) {
        // Try to extract userId from already-authenticated context
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof
                com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser user) {
            return "user:" + user.getUserId();
        }

        // Fallback to IP
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isEmpty())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return "ip:" + ip;
    }

    private void writeRateLimitResponse(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        Map<String, Object> body = Map.of(
                "success", false,
                "error", Map.of(
                        "code", "GEN_004",
                        "message", "Rate limit exceeded",
                        "retryAfter", retryAfterSeconds
                ),
                "timestamp", Instant.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Lua script for atomic token bucket.
     * KEYS[1] = bucket key
     * ARGV[1] = max tokens (capacity)
     * ARGV[2] = window in seconds (TTL)
     *
     * Returns 1 if allowed, 0 if rejected.
     */
    private DefaultRedisScript<Long> createTokenBucketScript() {
        String lua = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local current = tonumber(redis.call('GET', key) or '0')
                if current >= capacity then
                    return 0
                end
                current = redis.call('INCR', key)
                if current == 1 then
                    redis.call('EXPIRE', key, window)
                end
                return 1
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }
}
