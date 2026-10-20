package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.TxType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What a money-moving request is "about", used to detect an Idempotency-Key
 * being replayed with a different request.
 */
public record IdempotentRequest(UUID userId, String idempotencyKey, TxType type,
                                BigDecimal amount, UUID targetWalletId) {
}
