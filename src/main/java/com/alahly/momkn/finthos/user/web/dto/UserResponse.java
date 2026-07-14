package com.alahly.momkn.finthos.user.web.dto;

import com.alahly.momkn.finthos.user.domain.Role;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class UserResponse {

    UUID id;
    String username;
    String email;
    Role role;
    boolean enabled;
    Instant createdAt;
}
