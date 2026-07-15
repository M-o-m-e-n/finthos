package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.transaction.domain.LedgerEntry;
import com.alahly.momkn.finthos.transaction.repository.LedgerRepository;
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
    private final LedgerRepository ledgerRepository;

    public Wallet getByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No wallet for user: " + userId));
    }

    public Wallet createForUser(UUID userId, String currency) {
        Wallet wallet = Wallet.create(userId, currency);
        return walletRepository.save(wallet);
    }

    public Wallet credit(UUID walletId, BigDecimal amount, UUID transactionId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        wallet.credit(amount);
        Wallet saved = walletRepository.save(wallet);
        ledgerRepository.save(LedgerEntry.create(walletId, transactionId, amount, saved.getBalance()));
        return saved;
    }

    public Wallet debit(UUID walletId, BigDecimal amount, UUID transactionId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        wallet.debit(amount);
        Wallet saved = walletRepository.save(wallet);
        ledgerRepository.save(LedgerEntry.create(walletId, transactionId, amount.negate(), saved.getBalance()));
        return saved;
    }

    public BigDecimal getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        return wallet.getBalance();
    }
}
