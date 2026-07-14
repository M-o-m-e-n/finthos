package com.alahly.momkn.finthos.wallet.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletTest {

    @Test
    void credit_increasesBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");
        wallet.credit(new BigDecimal("50.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void debit_decreasesBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");
        wallet.credit(new BigDecimal("100.00"));
        wallet.debit(new BigDecimal("30.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void debit_belowZero_throwsInsufficientFunds() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");
        wallet.credit(new BigDecimal("10.00"));
        assertThrows(InsufficientFundsException.class, () -> wallet.debit(new BigDecimal("11.00")));
        assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void debit_exactBalance_succeeds() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");
        wallet.credit(new BigDecimal("50.00"));
        wallet.debit(new BigDecimal("50.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void create_startsAtZero() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getVersion()).isEqualTo(0L);
    }
}
