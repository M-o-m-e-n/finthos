package com.alahly.momkn.finthos.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ReversalResponse {

    private UUID id;
    private String type;
    private BigDecimal amount;
    private String status;
    private UUID sourceWalletId;
    private UUID targetWalletId;
    private Instant createdAt;
}
