package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.payment.CreatePaymentIntentRequest;
import com.example.eom.dto.payment.PaymentIntentResponse;
import com.example.eom.service.PaymentService;
import com.example.eom.service.impl.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PaymentController.class, StripeWebhookController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PaymentService paymentService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final PaymentIntentResponse MOCK_INTENT = new PaymentIntentResponse(
            "pi_test123", "pi_test123_secret_abc", 100L, 17998L, "usd");

    private static RequestPostProcessor asUser(String userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    // ── no auth ───────────────────────────────────────────────────────────────

    @Test
    void createIntent_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/payments/intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 100}"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/payments/intent ─────────────────────────────────────────────

    @Test
    void createIntent_validRequest_returns200() throws Exception {
        given(paymentService.createIntent(eq(1L), any())).willReturn(MOCK_INTENT);

        mockMvc.perform(post("/api/payments/intent").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePaymentIntentRequest(100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test123"))
                .andExpect(jsonPath("$.clientSecret").value("pi_test123_secret_abc"))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.amount").value(17998))
                .andExpect(jsonPath("$.currency").value("usd"));
    }

    @Test
    void createIntent_nullOrderId_returns400() throws Exception {
        mockMvc.perform(post("/api/payments/intent").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIntent_orderNotFound_returns404() throws Exception {
        given(paymentService.createIntent(eq(1L), any()))
                .willThrow(new EntityNotFoundException("Order not found: 999"));

        mockMvc.perform(post("/api/payments/intent").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createIntent_wrongUser_returns403() throws Exception {
        given(paymentService.createIntent(eq(2L), any()))
                .willThrow(new AccessDeniedException("Order does not belong to the current user"));

        mockMvc.perform(post("/api/payments/intent").with(asUser("2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createIntent_stripeError_returns409() throws Exception {
        given(paymentService.createIntent(eq(1L), any()))
                .willThrow(new IllegalStateException("Failed to create payment intent"));

        mockMvc.perform(post("/api/payments/intent").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 100}"))
                .andExpect(status().isConflict());
    }

    // ── POST /api/webhooks/stripe ─────────────────────────────────────────────

    @Test
    void webhook_validSignature_returns200() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_invalidSignature_returns400() throws Exception {
        willThrow(new IllegalArgumentException("Invalid webhook signature"))
                .given(paymentService).handleWebhookEvent(any(), any());

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "bad-sig")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhook_noAuth_returns200() throws Exception {
        // Webhook is public — no JWT required
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
