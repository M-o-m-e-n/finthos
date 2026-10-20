package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.common.error.EntityNotFoundException;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
        return insert(Transaction.create(type, amount, idempotencyKey, sourceWalletId, targetWalletId));
    }

    public Transaction createReversal(Transaction original, UUID adminId) {
        return insert(Transaction.reversalOf(original, adminId));
    }

    public void markSuccess(Transaction tx) {
        tx.markSuccess();
        transactionRepository.save(tx);
    }

    public void markFailed(Transaction tx) {
        tx.markFailed();
        transactionRepository.save(tx);
    }

    public void markReversed(Transaction tx) {
        tx.markPersisted();
        tx.markReversed();
        transactionRepository.save(tx);
    }

    public Transaction findById(UUID txId) {
        return transactionRepository.findById(txId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + txId));
    }

    /** Returns the transaction if the wallet is a party to it (or the caller is an admin). */
    public Transaction getVisibleTo(UUID txId, UUID walletId, boolean isAdmin) {
        Transaction tx = findById(txId);
        boolean isParty = walletId.equals(tx.getSourceWalletId()) || walletId.equals(tx.getTargetWalletId());
        if (!isAdmin && !isParty) {
            throw new AccessDeniedException("Transaction does not belong to this user");
        }
        return tx;
    }

    public boolean hasTransactions(UUID walletId) {
        return transactionRepository.existsForWallet(walletId);
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

    private Transaction insert(Transaction tx) {
        Transaction saved = transactionRepository.save(tx);
        saved.markPersisted();
        return saved;
    }
}
