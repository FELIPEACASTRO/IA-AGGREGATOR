package com.ia.aggregator.presentation.chat;

import com.ia.aggregator.application.chat.port.in.CompareModeUseCase;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Compare Mode — send the same prompt to multiple models for side-by-side comparison.
 */
@RestController
@RequestMapping("/api/v1/chat/compare")
public class CompareModeController {

    private final CompareModeUseCase compareModeUseCase;

    public CompareModeController(CompareModeUseCase compareModeUseCase) {
        this.compareModeUseCase = compareModeUseCase;
    }

    @SuppressWarnings("unchecked")
    @PostMapping
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<List<CompareModeUseCase.CompareResult>> compare(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {

        String prompt = (String) body.get("prompt");
        List<String> models = (List<String>) body.get("models");
        String systemInstruction = (String) body.get("systemInstruction");
        Double temperature = body.containsKey("temperature") ?
                ((Number) body.get("temperature")).doubleValue() : null;
        Integer maxTokens = body.containsKey("maxTokens") ?
                ((Number) body.get("maxTokens")).intValue() : null;

        return ResponseEntity.ok(compareModeUseCase.compare(
                user.getOrgId(), user.getUserId(),
                prompt, models, systemInstruction, temperature, maxTokens
        ));
    }
}
