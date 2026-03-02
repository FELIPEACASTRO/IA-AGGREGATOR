package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.dto.ChatResponse;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.application.ai.port.out.AiRoutingTelemetryPort;
import com.ia.aggregator.application.ai.port.out.ChatModelRoutingPolicy;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatUseCaseImplTest {

    @Mock
    private AiModelProvider primaryProvider;
    @Mock
    private AiModelProvider secondaryProvider;
    @Mock
    private ChatModelRoutingPolicy routingPolicy;
    @Mock
    private AiRoutingTelemetryPort telemetryPort;

    @InjectMocks
    private ChatUseCaseImpl useCase;

    @Test
    void execute_shouldReturnPrimaryProviderResponseWhenAvailable() {
        useCase = new ChatUseCaseImpl(List.of(primaryProvider, secondaryProvider), routingPolicy, telemetryPort);
        ChatCommand command = new ChatCommand("hello", "gpt-4o-mini");

        when(routingPolicy.resolveOrderedModels(command)).thenReturn(List.of("gpt-4o-mini", "claude-3-5-haiku"));
        when(primaryProvider.supports("gpt-4o-mini")).thenReturn(true);
        when(secondaryProvider.supports("gpt-4o-mini")).thenReturn(false);
        when(primaryProvider.providerName()).thenReturn("primary");
        when(primaryProvider.generate("hello", "gpt-4o-mini")).thenReturn("primary-answer");

        ChatResponse response = useCase.execute(command);

        assertEquals("primary-answer", response.content());
        assertEquals("gpt-4o-mini", response.modelUsed());
        assertEquals("primary", response.providerUsed());
        assertFalse(response.fallbackUsed());
        assertEquals(1, response.attempts());

        verify(telemetryPort).recordAttempt("gpt-4o-mini", "primary");
        verify(telemetryPort).recordSuccess("gpt-4o-mini", "primary", false, 1);
    }

    @Test
    void execute_shouldFallbackToSecondaryProviderWhenPrimaryFails() {
        useCase = new ChatUseCaseImpl(List.of(primaryProvider, secondaryProvider), routingPolicy, telemetryPort);
        ChatCommand command = new ChatCommand("hello", "gpt-4o-mini");

        when(routingPolicy.resolveOrderedModels(command)).thenReturn(List.of("gpt-4o-mini", "claude-3-5-haiku"));
        when(primaryProvider.supports("gpt-4o-mini")).thenReturn(true);
        when(secondaryProvider.supports("gpt-4o-mini")).thenReturn(true);
        when(primaryProvider.providerName()).thenReturn("primary");
        when(secondaryProvider.providerName()).thenReturn("secondary");

        when(primaryProvider.generate("hello", "gpt-4o-mini"))
                .thenThrow(new TechnicalException(ErrorCode.AI_002, "primary unavailable"));
        when(secondaryProvider.generate("hello", "gpt-4o-mini")).thenReturn("secondary-answer");

        ChatResponse response = useCase.execute(command);

        assertEquals("secondary-answer", response.content());
        assertEquals("secondary", response.providerUsed());
        assertTrue(response.fallbackUsed());
        assertEquals(2, response.attempts());

        verify(telemetryPort).recordFailure("gpt-4o-mini", "primary", "AI_002");
        verify(telemetryPort).recordSuccess("gpt-4o-mini", "secondary", true, 2);
    }

    @Test
    void execute_shouldThrowNoSuitableModelWhenNoProviderSupportsModels() {
        useCase = new ChatUseCaseImpl(List.of(primaryProvider), routingPolicy, telemetryPort);
        ChatCommand command = new ChatCommand("hello", "unknown-model");

        when(routingPolicy.resolveOrderedModels(command)).thenReturn(List.of("unknown-model"));
        when(primaryProvider.supports("unknown-model")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(command));

        assertEquals(ErrorCode.AI_001, ex.getErrorCode());
    }
}
