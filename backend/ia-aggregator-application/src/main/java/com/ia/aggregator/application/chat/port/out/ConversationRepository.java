package com.ia.aggregator.application.chat.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for conversation and message persistence.
 */
public interface ConversationRepository {

    void saveConversation(ConversationRecord conversation);

    Optional<ConversationRecord> findById(UUID id);

    List<ConversationRecord> findByUser(UUID userId);

    void deleteConversation(UUID id);

    void saveMessage(MessageRecord message);

    List<MessageRecord> findMessagesByConversation(UUID conversationId);

    record ConversationRecord(UUID id, UUID orgId, UUID userId, String title, String model,
                               boolean pinned, int messageCount, long totalTokens,
                               Instant lastMessageAt, Instant createdAt, Instant updatedAt) {}

    record MessageRecord(UUID id, UUID conversationId, String role, String content,
                          String modelUsed, String providerUsed, boolean fallbackUsed,
                          int attempts, Instant createdAt) {}
}
