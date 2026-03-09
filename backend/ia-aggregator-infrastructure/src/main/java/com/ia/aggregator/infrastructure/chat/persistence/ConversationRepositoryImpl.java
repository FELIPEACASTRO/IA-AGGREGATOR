package com.ia.aggregator.infrastructure.chat.persistence;

import com.ia.aggregator.application.chat.port.out.ConversationRepository;
import com.ia.aggregator.infrastructure.chat.persistence.entity.ConversationJpaEntity;
import com.ia.aggregator.infrastructure.chat.persistence.entity.MessageJpaEntity;
import com.ia.aggregator.infrastructure.chat.persistence.repository.ConversationJpaRepository;
import com.ia.aggregator.infrastructure.chat.persistence.repository.MessageJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationJpaRepository conversationJpa;
    private final MessageJpaRepository messageJpa;

    public ConversationRepositoryImpl(ConversationJpaRepository conversationJpa,
                                       MessageJpaRepository messageJpa) {
        this.conversationJpa = conversationJpa;
        this.messageJpa = messageJpa;
    }

    @Override
    public void saveConversation(ConversationRecord conversation) {
        ConversationJpaEntity entity = conversationJpa.findByIdAndDeletedAtIsNull(conversation.id())
                .orElseGet(ConversationJpaEntity::new);

        entity.setId(conversation.id());
        entity.setOrgId(conversation.orgId());
        entity.setUserId(conversation.userId());
        entity.setTitle(conversation.title());
        entity.setModel(conversation.model());
        entity.setPinned(conversation.pinned());
        entity.setMessageCount(conversation.messageCount());
        entity.setTotalTokens(conversation.totalTokens());
        entity.setLastMessageAt(conversation.lastMessageAt());
        entity.setStatus("active");
        conversationJpa.save(entity);
    }

    @Override
    public Optional<ConversationRecord> findById(UUID id) {
        return conversationJpa.findByIdAndDeletedAtIsNull(id).map(this::toConversationRecord);
    }

    @Override
    public List<ConversationRecord> findByUser(UUID userId) {
        return conversationJpa.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .stream().map(this::toConversationRecord).toList();
    }

    @Override
    public void deleteConversation(UUID id) {
        conversationJpa.findByIdAndDeletedAtIsNull(id).ifPresent(entity -> {
            entity.setDeletedAt(Instant.now());
            conversationJpa.save(entity);
        });
    }

    @Override
    public void saveMessage(MessageRecord message) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setId(message.id());
        entity.setConversationId(message.conversationId());
        entity.setRole(message.role());
        entity.setContent(message.content());
        entity.setStatus("completed");
        entity.setModelUsed(message.modelUsed());
        entity.setProviderUsed(message.providerUsed());
        entity.setFallbackUsed(message.fallbackUsed());
        entity.setAttempts(message.attempts());
        messageJpa.save(entity);
    }

    @Override
    public List<MessageRecord> findMessagesByConversation(UUID conversationId) {
        return messageJpa.findByConversationIdOrderByCreatedAt(conversationId)
                .stream().map(this::toMessageRecord).toList();
    }

    private ConversationRecord toConversationRecord(ConversationJpaEntity entity) {
        return new ConversationRecord(
                entity.getId(), entity.getOrgId(), entity.getUserId(),
                entity.getTitle(), entity.getModel(), entity.isPinned(),
                entity.getMessageCount(), entity.getTotalTokens(),
                entity.getLastMessageAt(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private MessageRecord toMessageRecord(MessageJpaEntity entity) {
        return new MessageRecord(
                entity.getId(), entity.getConversationId(), entity.getRole(),
                entity.getContent(), entity.getModelUsed(), entity.getProviderUsed(),
                entity.isFallbackUsed(), entity.getAttempts(), entity.getCreatedAt()
        );
    }
}
