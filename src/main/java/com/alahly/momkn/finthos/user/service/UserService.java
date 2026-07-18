package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.EmailAlreadyExistsException;
import com.alahly.momkn.finthos.common.error.UsernameAlreadyExistsException;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.mapper.UserMapper;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.user.web.dto.UserResponse;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final WalletService walletService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }
        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.create(request.getUsername(), request.getEmail(), passwordHash, Role.USER);
        User saved = userRepository.save(user);
        walletService.createForUser(saved.getId(), "USD");
        return userMapper.toResponse(saved);
    }
}
