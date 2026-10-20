package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.common.error.IdempotencyConflictException;
import com.alahly.momkn.finthos.common.error.IdempotencyConflictException.Reason;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    static final int MAX_KEY_LENGTH = 200;

    private final TransactionRepository transactionRepository;

    /**
     * Runs {@code action} at most once per (user, Idempotency-Key). The action receives the
     * user-scoped key to store on the transaction. A replay returns the stored transaction;
     * a concurrent duplicate that loses the race on the unique constraint is resolved the same way.
     */
    public Transaction execute(IdempotentRequest request, Function<String, Transaction> action) {
        String scopedKey = scopedKey(request);

        Optional<Transaction> existing = findReplay(scopedKey, request);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return action.apply(scopedKey);
        } catch (RuntimeException e) {
            if (!isDuplicateKey(e)) {
                throw e;
            }
            return findReplay(scopedKey, request).orElseThrow(() -> e);
        }
    }

    private Optional<Transaction> findReplay(String scopedKey, IdempotentRequest request) {
        return transactionRepository.findByIdempotencyKey(scopedKey)
                .map(tx -> verifyReplay(tx, request));
    }

    private Transaction verifyReplay(Transaction tx, IdempotentRequest request) {
        boolean sameRequest = tx.getType() == request.type()
                && tx.getAmount().compareTo(request.amount()) == 0
                && request.targetWalletId().equals(tx.getTargetWalletId());
        if (!sameRequest) {
            throw new IdempotencyConflictException(Reason.KEY_REUSED_WITH_DIFFERENT_REQUEST,
                    "Idempotency-Key was already used for a different request");
        }
        if (tx.getStatus() == TxStatus.PENDING) {
            throw new IdempotencyConflictException(Reason.REQUEST_IN_PROGRESS,
                    "A request with this Idempotency-Key is still being processed");
        }
        if (tx.getStatus() == TxStatus.FAILED) {
            throw new IdempotencyConflictException(Reason.PREVIOUS_ATTEMPT_FAILED,
                    "A previous request with this Idempotency-Key failed; retry with a new key");
        }
        return tx;
    }

    private static String scopedKey(IdempotentRequest request) {
        String key = request.idempotencyKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters");
        }
        return request.userId() + ":" + key;
    }

    private static boolean isDuplicateKey(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof DuplicateKeyException) {
                return true;
            }
        }
        return false;
    }
}
