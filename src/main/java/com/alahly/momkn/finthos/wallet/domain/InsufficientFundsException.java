package com.alahly.momkn.finthos.wallet.domain;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal balance, BigDecimal amount) {
        super("Insufficient funds: balance " + balance + ", attempted debit " + amount);
    }
}
