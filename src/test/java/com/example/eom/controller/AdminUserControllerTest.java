package com.example.eom.controller;

import com.example.eom.config.JwtAuthFilter;
import com.example.eom.config.JwtUtil;
import com.example.eom.config.SecurityConfig;
import com.example.eom.controller.admin.AdminUserController;
import com.example.eom.domain.enums.Role;
import com.example.eom.dto.user.AdminUserResponse;
import com.example.eom.exception.SelfDeactivationException;
import com.example.eom.service.UserService;
import com.example.eom.service.impl.CustomUserDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    private static final AdminUserResponse ACTIVE_USER = new AdminUserResponse(
            2L, "user@example.com", Role.USER, true, Instant.now());
    private static final AdminUserResponse INACTIVE_USER = new AdminUserResponse(
            2L, "user@example.com", Role.USER, false, Instant.now());

    private static RequestPostProcessor asAdmin(String adminId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // ── no auth ───────────────────────────────────────────────────────────────

    @Test
    void listAll_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/admin/users ──────────────────────────────────────────────────

    @Test
    void listAll_admin_returns200() throws Exception {
        given(userService.listAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(ACTIVE_USER)));

        mockMvc.perform(get("/api/admin/users").with(asAdmin("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    // ── PUT /api/admin/users/{id}/activate ────────────────────────────────────

    @Test
    void activate_existingUser_returns200() throws Exception {
        given(userService.activate(2L)).willReturn(ACTIVE_USER);

        mockMvc.perform(put("/api/admin/users/2/activate").with(asAdmin("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activate_notFound_returns404() throws Exception {
        given(userService.activate(99L))
                .willThrow(new EntityNotFoundException("User not found: 99"));

        mockMvc.perform(put("/api/admin/users/99/activate").with(asAdmin("1")))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/admin/users/{id}/deactivate ──────────────────────────────────

    @Test
    void deactivate_otherUser_returns200() throws Exception {
        given(userService.deactivate(eq(1L), eq(2L))).willReturn(INACTIVE_USER);

        mockMvc.perform(put("/api/admin/users/2/deactivate").with(asAdmin("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivate_self_returns422() throws Exception {
        willThrow(new SelfDeactivationException())
                .given(userService).deactivate(eq(1L), eq(1L));

        mockMvc.perform(put("/api/admin/users/1/deactivate").with(asAdmin("1")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deactivate_notFound_returns404() throws Exception {
        willThrow(new EntityNotFoundException("User not found: 99"))
                .given(userService).deactivate(eq(1L), eq(99L));

        mockMvc.perform(put("/api/admin/users/99/deactivate").with(asAdmin("1")))
                .andExpect(status().isNotFound());
    }
}
