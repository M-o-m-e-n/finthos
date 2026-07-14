package com.alahly.momkn.finthos.wallet.web.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
public class WalletResponse {

    UUID id;
    UUID userId;
    BigDecimal balance;
    String currency;
}
