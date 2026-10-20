package com.alahly.momkn.finthos.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 4 decimal places")
    private BigDecimal amount;
}
