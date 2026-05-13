package com.example.eom.service;

import com.example.eom.domain.User;
import com.example.eom.domain.enums.Role;
import com.example.eom.dto.user.AdminUserResponse;
import com.example.eom.exception.SelfDeactivationException;
import com.example.eom.repository.UserRepository;
import com.example.eom.service.impl.UserServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;

    private UserServiceImpl userService;

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID  = 2L;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    private User user(Long id, boolean active) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@example.com");
        u.setPasswordHash("hash");
        u.setRole(Role.USER);
        u.setActive(active);
        u.setCreatedAt(Instant.now());
        return u;
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    void deactivate_happyPath_setsActiveFalse() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, true)));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminUserResponse response = userService.deactivate(ADMIN_ID, USER_ID);

        assertThat(response.active()).isFalse();
        assertThat(response.id()).isEqualTo(USER_ID);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivate_selfDeactivation_throwsSelfDeactivationException() {
        assertThatThrownBy(() -> userService.deactivate(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(SelfDeactivationException.class);
    }

    @Test
    void deactivate_userNotFound_throwsEntityNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivate(ADMIN_ID, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── activate ──────────────────────────────────────────────────────────────

    @Test
    void activate_inactiveUser_setsActiveTrue() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID, false)));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminUserResponse response = userService.activate(USER_ID);

        assertThat(response.active()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void activate_userNotFound_throwsEntityNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activate(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
