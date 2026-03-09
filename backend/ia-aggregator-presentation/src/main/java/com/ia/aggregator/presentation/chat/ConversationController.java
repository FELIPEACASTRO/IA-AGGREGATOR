package com.ia.aggregator.presentation.chat;

import com.ia.aggregator.application.chat.port.in.ConversationUseCase;
import com.ia.aggregator.application.chat.port.out.ConversationRepository.ConversationRecord;
import com.ia.aggregator.application.chat.port.out.ConversationRepository.MessageRecord;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/conversations")
public class ConversationController {

    private final ConversationUseCase conversationUseCase;

    public ConversationController(ConversationUseCase conversationUseCase) {
        this.conversationUseCase = conversationUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationRecord>> create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody CreateConversationRequest request) {
        UUID userId = UUID.fromString(user.getUsername());
        ConversationRecord conversation = conversationUseCase.create(
                userId, request.orgId(), request.title(), request.model());
        return ResponseEntity.ok(ApiResponse.ok(conversation));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationRecord>>> list(
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(conversationUseCase.listByUser(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationRecord>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(conversationUseCase.getById(id)));
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<ApiResponse<ConversationRecord>> rename(
            @PathVariable UUID id, @RequestBody RenameRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(conversationUseCase.rename(id, request.title())));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<ConversationRecord>> togglePin(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(conversationUseCase.togglePin(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        conversationUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Void>> addMessage(
            @PathVariable UUID id, @RequestBody AddMessageRequest request) {
        conversationUseCase.addMessage(id, request.role(), request.content(),
                request.modelUsed(), request.providerUsed());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageRecord>>> getMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(conversationUseCase.getMessages(id)));
    }

    record CreateConversationRequest(UUID orgId, String title, String model) {}
    record RenameRequest(String title) {}
    record AddMessageRequest(String role, String content, String modelUsed, String providerUsed) {}
}
