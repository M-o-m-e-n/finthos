package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public Transaction create(TxType type, BigDecimal amount, String idempotencyKey,
                               UUID sourceWalletId, UUID targetWalletId) {
        Transaction tx = Transaction.create(type, amount, idempotencyKey, sourceWalletId, targetWalletId);
        Transaction saved = transactionRepository.save(tx);
        saved.markPersisted();
        return saved;
    }

    public void markSuccess(Transaction tx) {
        tx.markSuccess();
        transactionRepository.save(tx);
    }

    public void markFailed(Transaction tx) {
        tx.markFailed();
        transactionRepository.save(tx);
    }

    public Transaction findById(UUID txId) {
        return transactionRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));
    }
}
