package com.ia.aggregator.presentation.preferences;

import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping("/preferences/me")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getPreferences(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userSettingsService.getPreferences(principal.getUserId())));
    }

    @PutMapping("/preferences/me")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UserPreferencesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                userSettingsService.updatePreferences(principal.getUserId(), request),
                "Preferences updated"
        ));
    }

    @GetMapping("/notifications/me")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getNotificationPreferences(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userSettingsService.getNotificationPreferences(principal.getUserId())));
    }

    @PutMapping("/notifications/me")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> updateNotificationPreferences(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NotificationPreferencesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                userSettingsService.updateNotificationPreferences(principal.getUserId(), request),
                "Notification preferences updated"
        ));
    }
}
