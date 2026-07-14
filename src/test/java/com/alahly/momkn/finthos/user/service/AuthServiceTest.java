package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.AuthenticationFailedException;
import com.alahly.momkn.finthos.common.security.JwtService;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.AuthResponse;
import com.alahly.momkn.finthos.user.web.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success_returnsToken() {
        User user = User.create("user1", "user@example.com", "HASH", Role.USER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(jwtService.generateToken("user@example.com", Role.USER)).thenReturn("JWT-TOKEN");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.getToken()).isEqualTo("JWT-TOKEN");
    }

    @Test
    void login_wrongPassword_throws() {
        User user = User.create("user1", "user@example.com", "HASH", Role.USER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("user@example.com", "wrong")));
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("unknown@example.com", "password123")));
    }
}
