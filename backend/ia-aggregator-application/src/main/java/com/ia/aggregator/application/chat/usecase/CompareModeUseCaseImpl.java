package com.ia.aggregator.application.chat.usecase;

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
 */
@Service
public class CompareModeUseCaseImpl implements CompareModeUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompareModeUseCaseImpl.class);
    private static final int MAX_MODELS = 4;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

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
            // In a full implementation, this would use CapabilityRouter to resolve
            // the provider for the model and make the actual AI call.
            // For now, returns a placeholder that the infrastructure layer will wire.
            long latency = System.currentTimeMillis() - start;
            return new CompareResult(model, resolveProvider(model),
                    "Response placeholder for model: " + model,
                    latency, 0.0, 0, 0);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new CompareResult(model, resolveProvider(model),
                    "Error: " + e.getMessage(), latency, 0, 0, 0);
        }
    }

    private String resolveProvider(String model) {
        if (model.startsWith("gpt-") || model.startsWith("o1") || model.startsWith("o3") || model.startsWith("o4")) return "openai";
        if (model.startsWith("claude-")) return "anthropic";
        if (model.startsWith("gemini-")) return "gemini";
        if (model.startsWith("command-")) return "cohere";
        if (model.startsWith("llama-") || model.startsWith("mixtral")) return "groq";
        if (model.startsWith("deepseek-")) return "deepseek";
        return "unknown";
    }
}
