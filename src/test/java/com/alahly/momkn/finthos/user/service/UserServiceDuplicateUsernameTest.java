package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.UsernameAlreadyExistsException;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.mapper.UserMapper;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceDuplicateUsernameTest {

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
    void register_duplicateUsername_throwsUsernameAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(walletService, never()).createForUser(any(), any());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("bob", "bob@example.com", "password123");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(com.alahly.momkn.finthos.common.error.EmailAlreadyExistsException.class,
                () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }
}
