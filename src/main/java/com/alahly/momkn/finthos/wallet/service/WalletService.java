package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.transaction.service.LedgerService;
import com.alahly.momkn.finthos.common.error.EntityNotFoundException;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;

    public Wallet getByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("No wallet for user: " + userId));
    }

    public Wallet createForUser(UUID userId, String currency) {
        Wallet wallet = Wallet.create(userId, currency);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet credit(UUID walletId, BigDecimal amount, UUID transactionId) {
        Wallet wallet = getById(walletId);
        wallet.credit(amount);
        Wallet saved = walletRepository.save(wallet);
        ledgerService.record(walletId, transactionId, amount, saved.getBalance());
        return saved;
    }

    @Transactional
    public Wallet debit(UUID walletId, BigDecimal amount, UUID transactionId) {
        Wallet wallet = getById(walletId);
        wallet.debit(amount);
        Wallet saved = walletRepository.save(wallet);
        ledgerService.record(walletId, transactionId, amount.negate(), saved.getBalance());
        return saved;
    }

    public BigDecimal getBalance(UUID walletId) {
        return getById(walletId).getBalance();
    }

    private Wallet getById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + walletId));
    }
}
