package com.ia.aggregator.presentation.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.presentation.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserSettingsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private UserSettingsController userSettingsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userSettingsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void getPreferences_shouldReturn200() throws Exception {
        authenticate();
        when(userSettingsService.getPreferences(userId))
                .thenReturn(new UserPreferencesResponse("Docker Admin", "pt-BR", "dark", "default"));

        try {
            mockMvc.perform(get("/api/v1/preferences/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fullName").value("Docker Admin"))
                    .andExpect(jsonPath("$.data.theme").value("dark"))
                    .andExpect(jsonPath("$.data.chatFontMode").value("default"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void updatePreferences_shouldReturn200() throws Exception {
        authenticate();
        UserPreferencesRequest request = new UserPreferencesRequest("Docker Admin", "pt-BR", "system", "sans");
        when(userSettingsService.updatePreferences(any(), any()))
                .thenReturn(new UserPreferencesResponse("Docker Admin", "pt-BR", "system", "sans"));

        try {
            mockMvc.perform(put("/api/v1/preferences/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.theme").value("system"))
                    .andExpect(jsonPath("$.data.chatFontMode").value("sans"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(userSettingsService).updatePreferences(any(), any());
    }

    @Test
    void updatePreferences_shouldValidatePayload() throws Exception {
        authenticate();

        try {
            mockMvc.perform(put("/api/v1/preferences/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fullName":"","locale":"invalid","theme":"blue","chatFontMode":"weird"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void getNotificationPreferences_shouldReturn200() throws Exception {
        authenticate();
        when(userSettingsService.getNotificationPreferences(userId))
                .thenReturn(new NotificationPreferencesResponse(true, true, true, true, true, false));

        try {
            mockMvc.perform(get("/api/v1/notifications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pushEnabled").value(true))
                    .andExpect(jsonPath("$.data.productUpdates").value(false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void updateNotificationPreferences_shouldReturn200() throws Exception {
        authenticate();
        NotificationPreferencesRequest request =
                new NotificationPreferencesRequest(true, false, true, false, true, false);
        when(userSettingsService.updateNotificationPreferences(any(), any()))
                .thenReturn(new NotificationPreferencesResponse(true, false, true, false, true, false));

        try {
            mockMvc.perform(put("/api/v1/notifications/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pushEnabled").value(false))
                    .andExpect(jsonPath("$.data.usageAlerts").value(false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate() {
        var principal = new AuthenticatedUser(
                userId,
                "dockeradmin@ia-aggregator.local",
                "hash",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,
                true
        );

        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
