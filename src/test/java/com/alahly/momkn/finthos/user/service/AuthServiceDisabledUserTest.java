package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.AuthenticationFailedException;
import com.alahly.momkn.finthos.common.security.JwtService;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceDisabledUserTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_disabledUser_throwsAuthenticationFailed() {
        User disabledUser = User.create("alice", "alice@example.com", "hash", Role.USER);
        disabledUser.setEnabled(false);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("alice@example.com", "password123")));
    }
}
