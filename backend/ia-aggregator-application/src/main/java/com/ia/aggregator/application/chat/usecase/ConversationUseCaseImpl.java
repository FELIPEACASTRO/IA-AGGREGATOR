package com.ia.aggregator.application.chat.usecase;

import com.ia.aggregator.application.chat.port.in.ConversationUseCase;
import com.ia.aggregator.application.chat.port.out.ConversationRepository;
import com.ia.aggregator.application.chat.port.out.ConversationRepository.ConversationRecord;
import com.ia.aggregator.application.chat.port.out.ConversationRepository.MessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ConversationUseCaseImpl implements ConversationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConversationUseCaseImpl.class);

    private final ConversationRepository repository;

    public ConversationUseCaseImpl(ConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public ConversationRecord create(UUID userId, UUID orgId, String title, String model) {
        Instant now = Instant.now();
        ConversationRecord conversation = new ConversationRecord(
                UUID.randomUUID(), orgId, userId, title != null ? title : "Nova Conversa",
                model, false, 0, 0, now, now, now
        );
        repository.saveConversation(conversation);
        log.info("Conversation created: id={}, user={}", conversation.id(), userId);
        return conversation;
    }

    @Override
    public List<ConversationRecord> listByUser(UUID userId) {
        return repository.findByUser(userId);
    }

    @Override
    public ConversationRecord getById(UUID conversationId) {
        return repository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found: " + conversationId));
    }

    @Override
    public ConversationRecord rename(UUID conversationId, String title) {
        ConversationRecord existing = getById(conversationId);
        ConversationRecord updated = new ConversationRecord(
                existing.id(), existing.orgId(), existing.userId(),
                title.trim().isEmpty() ? existing.title() : title.trim(),
                existing.model(), existing.pinned(), existing.messageCount(),
                existing.totalTokens(), existing.lastMessageAt(),
                existing.createdAt(), Instant.now()
        );
        repository.saveConversation(updated);
        return updated;
    }

    @Override
    public ConversationRecord togglePin(UUID conversationId) {
        ConversationRecord existing = getById(conversationId);
        ConversationRecord updated = new ConversationRecord(
                existing.id(), existing.orgId(), existing.userId(),
                existing.title(), existing.model(), !existing.pinned(),
                existing.messageCount(), existing.totalTokens(),
                existing.lastMessageAt(), existing.createdAt(), Instant.now()
        );
        repository.saveConversation(updated);
        return updated;
    }

    @Override
    public void delete(UUID conversationId) {
        repository.deleteConversation(conversationId);
        log.info("Conversation deleted: id={}", conversationId);
    }

    @Override
    public void addMessage(UUID conversationId, String role, String content,
                           String modelUsed, String providerUsed) {
        MessageRecord message = new MessageRecord(
                UUID.randomUUID(), conversationId, role, content,
                modelUsed, providerUsed, false, 1, Instant.now()
        );
        repository.saveMessage(message);
    }

    @Override
    public List<MessageRecord> getMessages(UUID conversationId) {
        return repository.findMessagesByConversation(conversationId);
    }
}
