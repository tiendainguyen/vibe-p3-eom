package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.controller.admin.AdminInventoryController;
import com.example.eom.dto.inventory.InventoryResponse;
import com.example.eom.dto.inventory.UpdateInventoryRequest;
import com.example.eom.exception.InsufficientStockException;
import com.example.eom.service.InventoryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInventoryController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class AdminInventoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean InventoryService inventoryService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final InventoryResponse MOCK_INVENTORY =
            new InventoryResponse(1L, 10L, 100, 5, 95, Instant.now());

    @Test
    void listInventory_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listInventory_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listInventory_admin_returns200() throws Exception {
        given(inventoryService.list(null)).willReturn(List.of(MOCK_INVENTORY));

        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(10))
                .andExpect(jsonPath("$[0].availableQuantity").value(95));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listInventory_admin_withProductIdFilter_returns200() throws Exception {
        given(inventoryService.list(10L)).willReturn(List.of(MOCK_INVENTORY));

        mockMvc.perform(get("/api/admin/inventory").param("productId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantityOnHand").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_admin_validRequest_returns200() throws Exception {
        given(inventoryService.updateStock(eq(10L), any())).willReturn(MOCK_INVENTORY);

        mockMvc.perform(put("/api/admin/inventory/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateInventoryRequest(100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_admin_negativeQty_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/inventory/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateInventoryRequest(-1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_admin_belowReserved_returns400() throws Exception {
        given(inventoryService.updateStock(eq(10L), any()))
                .willThrow(new IllegalArgumentException("Cannot set quantityOnHand below current reserved"));

        mockMvc.perform(put("/api/admin/inventory/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateInventoryRequest(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
