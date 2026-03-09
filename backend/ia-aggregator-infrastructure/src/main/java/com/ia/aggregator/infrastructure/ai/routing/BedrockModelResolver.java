package com.ia.aggregator.infrastructure.ai.routing;

import java.util.Map;

/**
 * Resolves human-friendly model names to AWS Bedrock model ARNs (Amazon Resource Names).
 *
 * <p>AWS Bedrock requires fully-qualified model IDs in the format:
 * {@code {provider}.{model-name}-{version}:{variant}}
 * Users typically want to specify {@code "claude-3-5-sonnet"} or {@code "llama3-8b"};
 * this resolver maps those friendly names to Bedrock's required ARN format.
 *
 * <p>IMPORTANT: This map is configuration-driven, not hardcoded business logic.
 * Model versions change over time — the map can be overridden via
 * {@code app.ai.providers.bedrock.model-mappings} in application.yml.
 *
 * <p>Big O: O(1) lookup via HashMap.
 */
public class BedrockModelResolver {

    /**
     * Default friendly-name → Bedrock model ID mappings.
     * Based on Bedrock model IDs as of 2025; update via configuration as needed.
     * RULE: Never hardcode pricing or quota assumptions based on these model IDs.
     */
    private static final Map<String, String> DEFAULT_MAPPINGS = Map.ofEntries(
            // Anthropic Claude models on Bedrock
            Map.entry("claude-3-5-sonnet", "anthropic.claude-3-5-sonnet-20241022-v2:0"),
            Map.entry("claude-3-5-haiku", "anthropic.claude-3-5-haiku-20241022-v1:0"),
            Map.entry("claude-3-opus", "anthropic.claude-3-opus-20240229-v1:0"),
            Map.entry("claude-3-sonnet", "anthropic.claude-3-sonnet-20240229-v1:0"),
            Map.entry("claude-3-haiku", "anthropic.claude-3-haiku-20240307-v1:0"),

            // Meta Llama models on Bedrock
            Map.entry("llama3-8b", "meta.llama3-8b-instruct-v1:0"),
            Map.entry("llama3-70b", "meta.llama3-70b-instruct-v1:0"),
            Map.entry("llama3-1-8b", "meta.llama3-1-8b-instruct-v1:0"),
            Map.entry("llama3-1-70b", "meta.llama3-1-70b-instruct-v1:0"),
            Map.entry("llama3-2-1b", "us.meta.llama3-2-1b-instruct-v1:0"),
            Map.entry("llama3-2-3b", "us.meta.llama3-2-3b-instruct-v1:0"),

            // Amazon Titan models
            Map.entry("titan-text-express", "amazon.titan-text-express-v1"),
            Map.entry("titan-text-lite", "amazon.titan-text-lite-v1"),
            Map.entry("titan-embed-text", "amazon.titan-embed-text-v2:0"),
            Map.entry("titan-image-generator", "amazon.titan-image-generator-v2:0"),

            // Mistral models on Bedrock
            Map.entry("mistral-7b", "mistral.mistral-7b-instruct-v0:2"),
            Map.entry("mixtral-8x7b", "mistral.mixtral-8x7b-instruct-v0:1"),
            Map.entry("mistral-large", "mistral.mistral-large-2402-v1:0"),

            // AI21 on Bedrock
            Map.entry("jamba-1-5-mini", "ai21.jamba-1-5-mini-v1:0"),
            Map.entry("jamba-1-5-large", "ai21.jamba-1-5-large-v1:0"),

            // Cohere on Bedrock
            Map.entry("command-r", "cohere.command-r-v1:0"),
            Map.entry("command-r-plus", "cohere.command-r-plus-v1:0"),
            Map.entry("embed-english", "cohere.embed-english-v3"),
            Map.entry("embed-multilingual", "cohere.embed-multilingual-v3"),

            // Stability AI on Bedrock
            Map.entry("stability-sd3-large", "stability.sd3-large-v1:0"),
            Map.entry("stability-stable-image-ultra", "stability.stable-image-ultra-v1:0")
    );

    private final Map<String, String> modelMappings;

    /** Creates resolver with the default model mappings. */
    public BedrockModelResolver() {
        this.modelMappings = DEFAULT_MAPPINGS;
    }

    /**
     * Creates resolver with custom model mappings (for configuration injection).
     *
     * @param customMappings map of friendlyName -> bedrockModelId; merged with defaults
     */
    public BedrockModelResolver(Map<String, String> customMappings) {
        var merged = new java.util.HashMap<>(DEFAULT_MAPPINGS);
        if (customMappings != null) {
            merged.putAll(customMappings);
        }
        this.modelMappings = Map.copyOf(merged);
    }

    /**
     * Resolves a friendly model name to a Bedrock model ID.
     * If the name is already a valid Bedrock ARN (contains {@code .}), returns it as-is.
     *
     * @param friendlyName model name as specified by the caller
     * @return resolved Bedrock model ID, or the original name if not found in mappings
     */
    public String resolve(String friendlyName) {
        if (friendlyName == null || friendlyName.isBlank()) {
            throw new IllegalArgumentException("Bedrock model name must not be blank");
        }
        // If it already looks like a Bedrock model ID, pass through
        if (friendlyName.contains(".")) {
            return friendlyName;
        }
        return modelMappings.getOrDefault(friendlyName.toLowerCase(), friendlyName);
    }
}
