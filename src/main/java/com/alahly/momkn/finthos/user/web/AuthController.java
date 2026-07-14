package com.alahly.momkn.finthos.user.web;

import com.alahly.momkn.finthos.user.service.AuthService;
import com.alahly.momkn.finthos.user.service.UserService;
import com.alahly.momkn.finthos.user.web.dto.AuthResponse;
import com.alahly.momkn.finthos.user.web.dto.LoginRequest;
import com.alahly.momkn.finthos.user.web.dto.RegisterRequest;
import com.alahly.momkn.finthos.user.web.dto.RegisterResponse;
import com.alahly.momkn.finthos.user.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        String token = authService.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(user, token));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
