package com.ia.aggregator.domain.chat;

import java.time.Instant;
import java.util.UUID;

/**
 * A fork point in conversation history (edit or regenerate without losing original).
 */
public record ConversationFork(
        UUID id,
        UUID conversationId,
        UUID parentMessageId,
        UUID forkedMessageId,
        String reason,
        Instant createdAt
) {}
