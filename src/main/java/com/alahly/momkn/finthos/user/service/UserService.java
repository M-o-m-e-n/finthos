package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.EmailAlreadyExistsException;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.mapper.UserMapper;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.user.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.create(request.getUsername(), request.getEmail(), passwordHash, Role.USER);
        return userMapper.toResponse(userRepository.save(user));
    }
}
