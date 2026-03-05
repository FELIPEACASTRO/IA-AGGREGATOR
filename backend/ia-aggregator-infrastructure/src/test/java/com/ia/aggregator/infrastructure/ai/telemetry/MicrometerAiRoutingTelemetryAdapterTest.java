package com.ia.aggregator.infrastructure.ai.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MicrometerAiRoutingTelemetryAdapterTest {

    @Test
    void recordGuardrailBlocked_shouldIncrementTaggedCounter() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerAiRoutingTelemetryAdapter adapter = new MicrometerAiRoutingTelemetryAdapter(meterRegistry);

        adapter.recordGuardrailBlocked("output", "gpt-4o-mini", "openai", "GEN_002");

        Counter counter = meterRegistry.find("ai.guardrail.blocked")
                .tags("stage", "output", "model", "gpt-4o-mini", "provider", "openai", "reason", "GEN_002")
                .counter();

        assertNotNull(counter);
        assertEquals(1.0d, counter.count(), 0.0001d);
    }
}
