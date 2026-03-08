package com.ia.aggregator.infrastructure.ai.startup;

import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.infrastructure.security.SecretValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiProviderStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(AiProviderStartupValidator.class);

    private final List<AiModelProvider> providers;
    private final SecretValueProvider secretValueProvider;

    public AiProviderStartupValidator(List<AiModelProvider> providers, SecretValueProvider secretValueProvider) {
        this.providers = providers;
        this.secretValueProvider = secretValueProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logProviderReadiness() {
        providers.stream()
                .sorted((left, right) -> left.providerId().compareToIgnoreCase(right.providerId()))
                .forEach(provider -> {
                    List<String> missingSecrets = provider.requiredSecrets().stream()
                            .filter(secret -> !secretValueProvider.hasSecret(secret))
                            .toList();

                    if (missingSecrets.isEmpty() && provider.isConfigured()) {
                        log.info("Provider de IA pronto: provider={}, streaming={}, defaultModel={}",
                                provider.providerName(),
                                provider.supportsStreaming(),
                                provider.defaultModel());
                    } else {
                        log.warn("Provider de IA nao configurado: provider={}, missingSecrets={}",
                                provider.providerName(),
                                missingSecrets);
                    }
                });
    }
}
