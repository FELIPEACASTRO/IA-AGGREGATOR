package com.ia.aggregator.infrastructure.knowledge;

import com.ia.aggregator.application.knowledge.port.out.EmbeddingPort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Stub (no-op) implementation of EmbeddingPort.
 * TODO: Replace with real embedding service implementation.
 */
@Component
public class EmbeddingAdapter implements EmbeddingPort {

    @Override
    public List<float[]> embed(List<String> texts, String model) {
        return Collections.nCopies(texts.size(), new float[0]);
    }

    @Override
    public float[] embedQuery(String text, String model) {
        return new float[0];
    }

    @Override
    public int getDimensions(String model) {
        return 0;
    }
}
