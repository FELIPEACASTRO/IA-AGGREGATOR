package com.ia.aggregator.application.chat.port.in;

import com.ia.aggregator.application.chat.port.out.ConversationRepository.ConversationRecord;
import com.ia.aggregator.application.chat.port.out.ConversationRepository.MessageRecord;

import java.util.List;
import java.util.UUID;

/**
 * Use case for conversation CRUD operations.
 */
public interface ConversationUseCase {

    ConversationRecord create(UUID userId, UUID orgId, String title, String model);

    List<ConversationRecord> listByUser(UUID userId);

    ConversationRecord getById(UUID conversationId);

    ConversationRecord rename(UUID conversationId, String title);

    ConversationRecord togglePin(UUID conversationId);

    void delete(UUID conversationId);

    void addMessage(UUID conversationId, String role, String content,
                    String modelUsed, String providerUsed);

    List<MessageRecord> getMessages(UUID conversationId);
}
