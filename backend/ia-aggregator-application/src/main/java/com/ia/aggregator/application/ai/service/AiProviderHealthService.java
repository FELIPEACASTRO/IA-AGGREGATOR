package com.ia.aggregator.application.ai.service;

import com.ia.aggregator.application.ai.dto.AiHealthStatus;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiProviderHealthService {

    private final List<AiModelProvider> providers;

    public AiProviderHealthService(List<AiModelProvider> providers) {
        this.providers = providers;
    }

    public List<AiHealthStatus> listProviders() {
        return providers.stream()
                .map(AiModelProvider::healthCheck)
                .sorted((left, right) -> left.providerId().compareToIgnoreCase(right.providerId()))
                .toList();
    }

    public AiHealthStatus getProvider(String providerId) {
        return providers.stream()
                .filter(provider -> provider.providerId().equalsIgnoreCase(providerId))
                .findFirst()
                .map(AiModelProvider::healthCheck)
                .orElseThrow(() -> new BusinessException(ErrorCode.GEN_003, "Provider nao encontrado"));
    }
}
