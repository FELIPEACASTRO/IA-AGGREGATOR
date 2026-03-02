package com.ia.aggregator.infrastructure.event;

import com.ia.aggregator.application.auth.port.out.UserEventPublisher;
import com.ia.aggregator.domain.shared.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events via Spring ApplicationEventPublisher.
 */
@Component
public class SpringEventPublisher implements UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisher.class);
    private final ApplicationEventPublisher publisher;

    public SpringEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: {} [{}]", event.getEventType(), event.getEventId());
        publisher.publishEvent(event);
    }
}
