package com.alahly.momkn.finthos.user.service;

import com.alahly.momkn.finthos.common.error.AuthenticationFailedException;
import com.alahly.momkn.finthos.common.security.JwtService;
import com.alahly.momkn.finthos.user.domain.Role;
import com.alahly.momkn.finthos.user.domain.User;
import com.alahly.momkn.finthos.user.repository.UserRepository;
import com.alahly.momkn.finthos.user.web.dto.AuthResponse;
import com.alahly.momkn.finthos.user.web.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("Account is disabled");
        }

        return new AuthResponse(jwtService.generateToken(user.getEmail(), user.getRole()));
    }

    public String generateToken(String email, Role role) {
        return jwtService.generateToken(email, role);
    }
}
