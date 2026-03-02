package com.ia.aggregator.domain.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base marker interface for all domain events.
 */
public interface DomainEvent {

    UUID getEventId();

    Instant getOccurredAt();

    String getEventType();
}
