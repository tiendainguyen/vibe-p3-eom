package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.controller.admin.AdminWebhookController;
import com.example.eom.dto.webhook.CreateWebhookRequest;
import com.example.eom.dto.webhook.WebhookResponse;
import com.example.eom.service.WebhookService;
import com.example.eom.service.impl.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminWebhookController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class AdminWebhookControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean WebhookService webhookService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final WebhookResponse MOCK_RESPONSE = new WebhookResponse(
            1L, "https://example.com/hook",
            Set.of(WebhookService.EVENT_ORDER_PAID), true, Instant.now());

    // ── POST /api/admin/webhooks ──────────────────────────────────────────────

    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/hook\",\"eventTypes\":[\"order.paid\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/hook\",\"eventTypes\":[\"order.paid\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validRequest_returns201() throws Exception {
        given(webhookService.create(any())).willReturn(MOCK_RESPONSE);

        mockMvc.perform(post("/api/admin/webhooks").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateWebhookRequest("https://example.com/hook", Set.of("order.paid")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.url").value("https://example.com/hook"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_httpUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/webhooks").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://insecure.example.com\",\"eventTypes\":[\"order.paid\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_emptyEventTypes_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/webhooks").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/hook\",\"eventTypes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/admin/webhooks ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAll_returns200WithList() throws Exception {
        given(webhookService.listAll()).willReturn(List.of(MOCK_RESPONSE));

        mockMvc.perform(get("/api/admin/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("https://example.com/hook"));
    }

    // ── DELETE /api/admin/webhooks/{id} ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_existingId_returns204() throws Exception {
        willDoNothing().given(webhookService).delete(1L);

        mockMvc.perform(delete("/api/admin/webhooks/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_notFound_returns404() throws Exception {
        willThrow(new EntityNotFoundException("Webhook subscription not found: 99"))
                .given(webhookService).delete(99L);

        mockMvc.perform(delete("/api/admin/webhooks/99").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }
}
