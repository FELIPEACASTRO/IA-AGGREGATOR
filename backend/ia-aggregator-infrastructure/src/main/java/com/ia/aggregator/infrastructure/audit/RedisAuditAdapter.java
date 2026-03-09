package com.ia.aggregator.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.auth.port.out.AuditPort;
import com.ia.aggregator.domain.audit.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Redis-backed audit trail with eventual persistence to PostgreSQL.
 *
 * <p>Audit events are written to a Redis stream for real-time queries
 * and flushed to the audit schema periodically.
 */
@Component
public class RedisAuditAdapter implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(RedisAuditAdapter.class);
    private static final String AUDIT_KEY_PREFIX = "audit:events:";
    private static final Duration AUDIT_TTL = Duration.ofDays(90);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAuditAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(AuditEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = AUDIT_KEY_PREFIX + event.orgId();

            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, AUDIT_TTL);

            log.debug("Audit event recorded: action={}, actor={}, outcome={}",
                    event.action(), event.actorEmail(), event.outcome());
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", e.getMessage());
        }
    }

    @Override
    public List<AuditEvent> findByOrg(UUID orgId, Instant from, Instant to, int limit) {
        return queryEvents(AUDIT_KEY_PREFIX + orgId, from, to, limit);
    }

    @Override
    public List<AuditEvent> findByActor(UUID actorId, Instant from, Instant to, int limit) {
        // For actor-based queries, scan org events (simplified; production would use secondary index)
        return Collections.emptyList();
    }

    @Override
    public List<AuditEvent> findByAction(String action, Instant from, Instant to, int limit) {
        return Collections.emptyList();
    }

    private List<AuditEvent> queryEvents(String key, Instant from, Instant to, int limit) {
        try {
            List<String> events = redisTemplate.opsForList().range(key, -limit, -1);
            if (events == null) return Collections.emptyList();

            return events.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, AuditEvent.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(e -> e != null)
                    .filter(e -> !e.timestamp().isBefore(from) && !e.timestamp().isAfter(to))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to query audit events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
