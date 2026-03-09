package com.ia.aggregator.infrastructure.ai.adapter;

import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.domain.ai.Capability;

import java.util.EnumSet;
import java.util.Set;

/**
 * Adapter that wraps existing {@link AiModelProvider} implementations
 * into the {@link MultiCapabilityProvider} interface without modifying them.
 *
 * <p>Design Pattern: Adapter — bridges the legacy single-capability interface
 * to the new multi-capability system. All 17 existing providers are wrapped
 * by this adapter, preserving backward compatibility.
 *
 * <p>SOLID: Open/Closed — existing providers are NOT modified; they are extended
 * through composition via this adapter.
 *
 * <p>Big O: O(1) — all method delegations are constant-time.
 */
public class LegacyProviderAdapter implements MultiCapabilityProvider, ChatCapable {

    private static final Set<Capability> LEGACY_CAPABILITIES = EnumSet.of(Capability.CHAT);

    private final AiModelProvider legacy;

    public LegacyProviderAdapter(AiModelProvider legacy) {
        this.legacy = legacy;
    }

    @Override
    public String providerName() {
        return legacy.providerName();
    }

    @Override
    public boolean supports(String model) {
        return legacy.supports(model);
    }

    @Override
    public String generate(String prompt, String model) {
        return legacy.generate(prompt, model);
    }

    @Override
    public Set<Capability> capabilities() {
        return LEGACY_CAPABILITIES;
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        String model = request.model();
        String content = legacy.generate(request.prompt(), model);
        return new ChatResult(content, model, legacy.providerName(), false, 1);
    }

    /**
     * Returns the wrapped legacy provider for direct access if needed.
     */
    public AiModelProvider unwrap() {
        return legacy;
    }
}
