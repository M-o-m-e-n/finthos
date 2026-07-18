package com.alahly.momkn.finthos.payment.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class TxResult {

    private UUID transactionId;
    private String type;
    private BigDecimal amount;
    private String status;
    private UUID sourceWalletId;
    private UUID targetWalletId;
    private Instant createdAt;
    private BigDecimal walletBalance;
}
