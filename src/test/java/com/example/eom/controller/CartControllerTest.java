package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.dto.cart.AddToCartRequest;
import com.example.eom.dto.cart.CartResponse;
import com.example.eom.dto.cart.UpdateCartItemRequest;
import com.example.eom.exception.InsufficientStockException;
import com.example.eom.service.CartService;
import com.example.eom.service.impl.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CartService cartService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final CartResponse EMPTY_CART =
            new CartResponse(1L, List.of(), BigDecimal.ZERO);

    /** Mimics what JwtAuthFilter sets: String "1" as principal (userId). */
    private static RequestPostProcessor asUser(String userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    // ── no auth ───────────────────────────────────────────────────────────────

    @Test
    void getCart_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(1L, 1))))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/cart ─────────────────────────────────────────────────────────

    @Test
    void getCart_authenticated_returns200() throws Exception {
        given(cartService.getCart(1L)).willReturn(EMPTY_CART);

        mockMvc.perform(get("/api/cart").with(asUser("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.total").value(0));
    }

    // ── POST /api/cart/items ──────────────────────────────────────────────────

    @Test
    void addItem_validRequest_returns200() throws Exception {
        given(cartService.addItem(eq(1L), any())).willReturn(EMPTY_CART);

        mockMvc.perform(post("/api/cart/items").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(10L, 2))))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(10L, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_inactiveProduct_returns404() throws Exception {
        given(cartService.addItem(eq(1L), any()))
                .willThrow(new EntityNotFoundException("Product not found: 99"));

        mockMvc.perform(post("/api/cart/items").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(99L, 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItem_outOfStock_returns409() throws Exception {
        given(cartService.addItem(eq(1L), any()))
                .willThrow(new InsufficientStockException(10L, 5, 1));

        mockMvc.perform(post("/api/cart/items").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(10L, 5))))
                .andExpect(status().isConflict());
    }

    // ── PUT /api/cart/items/{productId} ───────────────────────────────────────

    @Test
    void updateItem_validRequest_returns200() throws Exception {
        given(cartService.updateItem(eq(1L), eq(10L), any())).willReturn(EMPTY_CART);

        mockMvc.perform(put("/api/cart/items/10").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(3))))
                .andExpect(status().isOk());
    }

    @Test
    void updateItem_itemNotInCart_returns404() throws Exception {
        given(cartService.updateItem(eq(1L), eq(99L), any()))
                .willThrow(new EntityNotFoundException("Item not in cart: product 99"));

        mockMvc.perform(put("/api/cart/items/99").with(asUser("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(1))))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/cart/items/{productId} ────────────────────────────────────

    @Test
    void removeItem_existingItem_returns200() throws Exception {
        given(cartService.removeItem(1L, 10L)).willReturn(EMPTY_CART);

        mockMvc.perform(delete("/api/cart/items/10").with(asUser("1")))
                .andExpect(status().isOk());
    }

    // ── DELETE /api/cart ──────────────────────────────────────────────────────

    @Test
    void clearCart_returns204() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/api/cart").with(asUser("1")))
                .andExpect(status().isNoContent());
    }
}
