package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.controller.admin.AdminOrderController;
import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.dto.order.UpdateOrderStatusRequest;
import com.example.eom.service.OrderService;
import com.example.eom.service.impl.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrderController.class, AdminOrderController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean OrderService orderService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final OrderResponse MOCK_ORDER = new OrderResponse(
            100L, 1L, OrderStatus.PENDING, List.of(),
            new BigDecimal("179.98"), null, Instant.now(), Instant.now());

    private static RequestPostProcessor asUser(String userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                "99", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // ── no auth ───────────────────────────────────────────────────────────────

    @Test
    void createOrder_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void listOrders_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    // ── POST /api/orders ──────────────────────────────────────────────────────

    @Test
    void createOrder_validCart_returns201() throws Exception {
        given(orderService.createFromCart(1L)).willReturn(MOCK_ORDER);

        mockMvc.perform(post("/api/orders").with(asUser("1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_emptyCart_returns400() throws Exception {
        given(orderService.createFromCart(1L))
                .willThrow(new IllegalArgumentException("Cannot create order from an empty cart"));

        mockMvc.perform(post("/api/orders").with(asUser("1")))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/orders ───────────────────────────────────────────────────────

    @Test
    void listOrders_authenticated_returns200() throws Exception {
        given(orderService.listByUser(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(MOCK_ORDER)));

        mockMvc.perform(get("/api/orders").with(asUser("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100));
    }

    // ── GET /api/orders/{id} ──────────────────────────────────────────────────

    @Test
    void getById_ownOrder_returns200() throws Exception {
        given(orderService.getById(1L, 100L)).willReturn(MOCK_ORDER);

        mockMvc.perform(get("/api/orders/100").with(asUser("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(179.98));
    }

    @Test
    void getById_wrongUser_returns403() throws Exception {
        given(orderService.getById(2L, 100L))
                .willThrow(new AccessDeniedException("Order does not belong to the current user"));

        mockMvc.perform(get("/api/orders/100").with(asUser("2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        given(orderService.getById(1L, 999L))
                .willThrow(new EntityNotFoundException("Order not found: 999"));

        mockMvc.perform(get("/api/orders/999").with(asUser("1")))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/orders/{id}/cancel ──────────────────────────────────────────

    @Test
    void cancel_pendingOrder_returns200() throws Exception {
        OrderResponse cancelled = new OrderResponse(100L, 1L, OrderStatus.CANCELLED, List.of(),
                new BigDecimal("179.98"), null, Instant.now(), Instant.now());
        given(orderService.cancel(1L, 100L)).willReturn(cancelled);

        mockMvc.perform(post("/api/orders/100/cancel").with(asUser("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_nonPendingOrder_returns409() throws Exception {
        given(orderService.cancel(1L, 100L))
                .willThrow(new IllegalStateException("Only PENDING orders can be cancelled"));

        mockMvc.perform(post("/api/orders/100/cancel").with(asUser("1")))
                .andExpect(status().isConflict());
    }

    // ── ADMIN: GET /api/admin/orders ──────────────────────────────────────────

    @Test
    void adminListOrders_adminRole_returns200() throws Exception {
        given(orderService.adminList(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(MOCK_ORDER)));

        mockMvc.perform(get("/api/admin/orders").with(asAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void adminListOrders_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/orders").with(asUser("1")))
                .andExpect(status().isForbidden());
    }

    // ── ADMIN: PUT /api/admin/orders/{id}/status ──────────────────────────────

    @Test
    void updateStatus_admin_validTransition_returns200() throws Exception {
        OrderResponse processing = new OrderResponse(100L, 1L, OrderStatus.PROCESSING, List.of(),
                new BigDecimal("179.98"), null, Instant.now(), Instant.now());
        given(orderService.updateStatus(eq(100L), any())).willReturn(processing);

        mockMvc.perform(put("/api/admin/orders/100/status").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrderStatusRequest(OrderStatus.PROCESSING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void updateStatus_admin_nullStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/100/status").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_admin_invalidTransition_returns409() throws Exception {
        given(orderService.updateStatus(eq(100L), any()))
                .willThrow(new IllegalStateException("Invalid status transition: PENDING → DELIVERED"));

        mockMvc.perform(put("/api/admin/orders/100/status").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isConflict());
    }
}
