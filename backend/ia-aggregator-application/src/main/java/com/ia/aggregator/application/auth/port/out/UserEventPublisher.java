package com.ia.aggregator.application.auth.port.out;

import com.ia.aggregator.domain.shared.event.DomainEvent;

/**
 * Port for publishing domain events.
 * Implemented by infrastructure (Spring ApplicationEventPublisher).
 */
public interface UserEventPublisher {

    void publish(DomainEvent event);
}
