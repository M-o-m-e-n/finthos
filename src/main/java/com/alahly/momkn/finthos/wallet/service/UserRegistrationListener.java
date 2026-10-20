package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.user.UserRegistered;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Gives every new user exactly one wallet (FR-2).
 * Deliberately a synchronous {@code @EventListener}, not an after-commit/async one: it runs
 * inside the registration transaction, so a user can never exist without a wallet.
 */
@Component
@RequiredArgsConstructor
class UserRegistrationListener {

    static final String DEFAULT_CURRENCY = "USD";

    private final WalletService walletService;

    @EventListener
    void on(UserRegistered event) {
        walletService.createForUser(event.userId(), DEFAULT_CURRENCY);
    }
}
