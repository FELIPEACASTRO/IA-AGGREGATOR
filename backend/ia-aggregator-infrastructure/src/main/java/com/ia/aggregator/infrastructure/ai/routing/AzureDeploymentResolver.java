package com.ia.aggregator.infrastructure.ai.routing;

import java.util.Map;

/**
 * Resolves user-specified model names to Azure OpenAI deployment names.
 *
 * <p>Azure OpenAI does not expose raw models directly — instead, organizations
 * create named "deployments" in the Azure Portal that point to specific model versions.
 * A user might request {@code "gpt-4o"}, but the Azure deployment might be named
 * {@code "my-gpt4o-prod"} or {@code "gpt4o-eastus2"}.
 *
 * <p>This resolver maps friendly model names to deployment names via external configuration.
 * If no mapping is found, the model name is used as the deployment name (pass-through).
 *
 * <p>Configuration:
 * <pre>
 * app.ai.providers.azure-openai:
 *   deployment-mappings:
 *     gpt-4o: my-gpt4o-deployment
 *     gpt-4o-mini: my-mini-deployment
 *     text-embedding-3-large: my-embed-deployment
 * </pre>
 *
 * <p>Big O: O(1) lookup via HashMap.
 */
public class AzureDeploymentResolver {

    private final Map<String, String> deploymentMappings;

    /**
     * Creates a resolver with no custom mappings.
     * All model names will pass through as deployment names.
     */
    public AzureDeploymentResolver() {
        this.deploymentMappings = Map.of();
    }

    /**
     * Creates a resolver with custom deployment name mappings.
     *
     * @param deploymentMappings map of {modelFriendlyName -> deploymentName}
     */
    public AzureDeploymentResolver(Map<String, String> deploymentMappings) {
        this.deploymentMappings = deploymentMappings != null
                ? Map.copyOf(deploymentMappings)
                : Map.of();
    }

    /**
     * Returns the Azure deployment name for the given model name.
     * If no mapping is configured, the model name is returned as-is.
     *
     * @param modelName the model name specified in the request (e.g., {@code "gpt-4o"})
     * @return the configured deployment name, or {@code modelName} if no mapping exists
     */
    public String resolve(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Azure model name must not be blank");
        }
        return deploymentMappings.getOrDefault(modelName, modelName);
    }

    /**
     * Returns true if a custom mapping exists for the given model name.
     *
     * @param modelName the model name to check
     * @return true if a deployment mapping is configured
     */
    public boolean hasMappingFor(String modelName) {
        return deploymentMappings.containsKey(modelName);
    }
}
