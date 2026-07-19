package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    public void markPersisted(Transaction tx) {
        tx.markPersisted();
        transactionRepository.save(tx);
    }

    public Page<Transaction> listTransactions(UUID walletId, TxType type, Instant fromDate, Instant toDate,
                                               Pageable pageable) {
        String typeStr = (type != null) ? type.name() : null;
        List<Transaction> content = transactionRepository.findByWalletAndFilters(
                walletId, typeStr, fromDate, toDate,
                pageable.getPageSize(), (int) pageable.getOffset());
        long total = transactionRepository.countByWalletAndFilters(walletId, typeStr, fromDate, toDate);
        return new PageImpl<>(content, pageable, total);
    }
}
