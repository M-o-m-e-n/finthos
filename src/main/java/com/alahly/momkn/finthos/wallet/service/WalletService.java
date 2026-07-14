package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public Wallet getByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No wallet for user: " + userId));
    }

    public Wallet createForUser(UUID userId, String currency) {
        Wallet wallet = Wallet.create(userId, currency);
        return walletRepository.save(wallet);
    }

    public Wallet credit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        wallet.credit(amount);
        return walletRepository.save(wallet);
    }

    public Wallet debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        wallet.debit(amount);
        return walletRepository.save(wallet);
    }

    public BigDecimal getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        return wallet.getBalance();
    }
}
