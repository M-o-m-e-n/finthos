package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.EmailAlreadyExistsException;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.mapper.UserMapper;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.user.web.dto.UserResponse;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private WalletService walletService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_savesNewUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest("user1", "user@example.com", "password123");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletService.createForUser(any(UUID.class), eq("USD")))
                .thenReturn(Wallet.create(UUID.randomUUID(), "USD"));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(UUID.randomUUID(), "user1", "user@example.com", Role.USER, true, Instant.now()));

        UserResponse result = userService.register(request);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getUsername()).isEqualTo("user1");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(walletService).createForUser(any(UUID.class), eq("USD"));
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("user1", "user@example.com", "password123");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }
}
