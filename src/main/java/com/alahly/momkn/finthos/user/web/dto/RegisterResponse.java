package com.alahly.momkn.finthos.user.web.dto;

import lombok.Value;

@Value
public class RegisterResponse {
    UserResponse user;
    String token;
}
