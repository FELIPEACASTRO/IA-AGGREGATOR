package com.ia.aggregator.application.chat.usecase;

import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.application.chat.port.in.CompareModeUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Compare mode: sends the same prompt to multiple models in parallel.
 * Calls real AI providers via the injected AiModelProvider list.
 */
@Service
public class CompareModeUseCaseImpl implements CompareModeUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompareModeUseCaseImpl.class);
    private static final int MAX_MODELS = 4;

    private final List<AiModelProvider> providers;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CompareModeUseCaseImpl(List<AiModelProvider> providers) {
        this.providers = providers;
    }

    @Override
    public List<CompareResult> compare(UUID orgId, UUID userId, String prompt,
                                        List<String> models, String systemInstruction,
                                        Double temperature, Integer maxTokens) {
        if (models.size() > MAX_MODELS) {
            throw new IllegalArgumentException("Compare mode supports up to " + MAX_MODELS + " models");
        }

        log.info("Compare mode: org={}, models={}, promptLength={}",
                orgId, models, prompt.length());

        List<CompletableFuture<CompareResult>> futures = models.stream()
                .map(model -> CompletableFuture.supplyAsync(() ->
                        executeForModel(orgId, prompt, model, systemInstruction, temperature, maxTokens),
                        executor
                ))
                .toList();

        return futures.stream()
                .map(f -> {
                    try {
                        return f.join();
                    } catch (Exception e) {
                        log.warn("Compare mode: model execution failed: {}", e.getMessage());
                        return new CompareResult("unknown", "unknown",
                                "Error: " + e.getMessage(), 0, 0, 0, 0);
                    }
                })
                .toList();
    }

    private CompareResult executeForModel(UUID orgId, String prompt, String model,
                                           String systemInstruction, Double temperature,
                                           Integer maxTokens) {
        long start = System.currentTimeMillis();
        try {
            List<AiModelProvider> matchingProviders = providers.stream()
                    .filter(provider -> provider.supports(model))
                    .toList();

            if (matchingProviders.isEmpty()) {
                long latency = System.currentTimeMillis() - start;
                return new CompareResult(model, "none",
                        "Error: no provider found for model " + model,
                        latency, 0.0, 0, 0);
            }

            for (AiModelProvider provider : matchingProviders) {
                try {
                    String content = provider.generate(prompt, model);
                    long latency = System.currentTimeMillis() - start;
                    return new CompareResult(model, provider.providerName(),
                            content, latency, 0.0, 0, 0);
                } catch (Exception providerEx) {
                    log.warn("Compare mode: provider {} failed for model {}: {}",
                            provider.providerName(), model, providerEx.getMessage());
                }
            }

            long latency = System.currentTimeMillis() - start;
            return new CompareResult(model, "none",
                    "Error: all providers failed for model " + model,
                    latency, 0.0, 0, 0);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new CompareResult(model, "unknown",
                    "Error: " + e.getMessage(), latency, 0, 0, 0);
        }
    }
}
