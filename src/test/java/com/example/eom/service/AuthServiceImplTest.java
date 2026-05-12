package com.example.eom.service;

import com.example.eom.config.JwtUtil;
import com.example.eom.domain.User;
import com.example.eom.domain.enums.Role;
import com.example.eom.dto.auth.AuthResponse;
import com.example.eom.dto.auth.LoginRequest;
import com.example.eom.dto.auth.RegisterRequest;
import com.example.eom.repository.UserRepository;
import com.example.eom.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void register_newEmail_savesUserAndReturnsToken() {
        var request = new RegisterRequest("user@example.com", "Secret@123");
        given(userRepository.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("Secret@123")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        given(jwtUtil.generateToken(1L, "user@example.com", Role.USER)).willReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsIllegalState() {
        var request = new RegisterRequest("dup@example.com", "Secret@123");
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email already registered");
    }

    @Test
    void login_validCredentials_returnsToken() {
        var request = new LoginRequest("user@example.com", "Secret@123");
        User user = User.builder().id(1L).email("user@example.com")
                .passwordHash("hashed").role(Role.USER).build();

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(jwtUtil.generateToken(1L, "user@example.com", Role.USER)).willReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user@example.com", "Secret@123"));
    }

    @Test
    void login_badCredentials_throwsBadCredentialsException() {
        var request = new LoginRequest("user@example.com", "wrong");
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
