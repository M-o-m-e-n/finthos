package com.alahly.momkn.finthos.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AdminUserResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private boolean enabled;
    private Instant createdAt;
}
