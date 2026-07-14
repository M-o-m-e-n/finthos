package com.alahly.momkn.finthos.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    String username;

    @Email
    @NotBlank
    String email;

    @NotBlank
    @Size(min = 8, max = 128)
    String password;
}
