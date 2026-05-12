package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.controller.admin.AdminProductController;
import com.example.eom.dto.product.CreateProductRequest;
import com.example.eom.dto.product.ProductResponse;
import com.example.eom.dto.product.UpdateProductRequest;
import com.example.eom.service.ProductService;
import com.example.eom.service.impl.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProductController.class, AdminProductController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final ProductResponse MOCK_PRODUCT = new ProductResponse(
            1L, "Running Shoes", "Lightweight trail shoes",
            new BigDecimal("89.99"), "footwear", null,
            true, Instant.now(), Instant.now());

    // ── Public endpoints ────────────────────────────────────────────────────

    @Test
    void listProducts_noFilters_returns200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(MOCK_PRODUCT));
        given(productService.listProducts(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Running Shoes"));
    }

    @Test
    void getById_existingProduct_returns200() throws Exception {
        given(productService.getById(1L)).willReturn(MOCK_PRODUCT);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.price").value(89.99));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        given(productService.getById(99L)).willThrow(new EntityNotFoundException("Product not found: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Admin endpoints — unauthenticated ──────────────────────────────────

    @Test
    void createProduct_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest("Shoes", null, new BigDecimal("89.99"), null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_userRole_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest("Shoes", null, new BigDecimal("89.99"), null, null))))
                .andExpect(status().isForbidden());
    }

    // ── Admin endpoints — authenticated as ADMIN ──────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_admin_validBody_returns201() throws Exception {
        given(productService.create(any())).willReturn(MOCK_PRODUCT);

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest("Running Shoes", "Lightweight",
                                        new BigDecimal("89.99"), "footwear", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Running Shoes"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_admin_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest("", null, new BigDecimal("89.99"), null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_admin_partialUpdate_returns200() throws Exception {
        given(productService.update(eq(1L), any())).willReturn(MOCK_PRODUCT);

        mockMvc.perform(patch("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProductRequest("Trail Shoes", null, null, null, null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_admin_notFound_returns404() throws Exception {
        given(productService.update(eq(99L), any()))
                .willThrow(new EntityNotFoundException("Product not found: 99"));

        mockMvc.perform(patch("/api/admin/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProductRequest("X", null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_admin_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_admin_notFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Product not found: 99"))
                .when(productService).delete(99L);

        mockMvc.perform(delete("/api/admin/products/99"))
                .andExpect(status().isNotFound());
    }
}
