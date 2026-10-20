package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.common.error.IdempotencyConflictException;
import com.alahly.momkn.finthos.common.error.IdempotencyConflictException.Reason;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private IdempotencyService idempotencyService;

    private UUID userId;
    private UUID targetWalletId;
    private IdempotentRequest request;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(transactionRepository);
        userId = UUID.randomUUID();
        targetWalletId = UUID.randomUUID();
        request = new IdempotentRequest(userId, "key", TxType.TRANSFER, new BigDecimal("10.00"), targetWalletId);
    }

    private Transaction stored(BigDecimal amount) {
        Transaction tx = Transaction.create(TxType.TRANSFER, amount, userId + ":key", UUID.randomUUID(), targetWalletId);
        tx.markPersisted();
        return tx;
    }

    @Test
    void keyIsScopedToUser() {
        when(transactionRepository.findByIdempotencyKey(userId + ":key")).thenReturn(Optional.empty());

        String usedKey = idempotencyService.execute(request, scopedKey -> {
            assertThat(scopedKey).isEqualTo(userId + ":key");
            return stored(new BigDecimal("10.00"));
        }).getIdempotencyKey();

        assertThat(usedKey).isEqualTo(userId + ":key");
    }

    @Test
    void concurrentDuplicate_resolvesToWinningTransaction() {
        Transaction winner = stored(new BigDecimal("10.00"));
        winner.markSuccess();
        when(transactionRepository.findByIdempotencyKey(userId + ":key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));

        Transaction result = idempotencyService.execute(request, scopedKey -> {
            throw new RuntimeException(new DuplicateKeyException("unique violation"));
        });

        assertThat(result).isSameAs(winner);
    }

    @Test
    void sameKeyDifferentAmount_isRejected() {
        Transaction existing = stored(new BigDecimal("99.00"));
        existing.markSuccess();
        when(transactionRepository.findByIdempotencyKey(userId + ":key")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> idempotencyService.execute(request, k -> null))
                .isInstanceOf(IdempotencyConflictException.class)
                .extracting(e -> ((IdempotencyConflictException) e).getReason())
                .isEqualTo(Reason.KEY_REUSED_WITH_DIFFERENT_REQUEST);
    }

    @Test
    void replayOfFailedRequest_isRejected() {
        Transaction existing = stored(new BigDecimal("10.00"));
        existing.markFailed();
        when(transactionRepository.findByIdempotencyKey(userId + ":key")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> idempotencyService.execute(request, k -> null))
                .isInstanceOf(IdempotencyConflictException.class)
                .extracting(e -> ((IdempotencyConflictException) e).getReason())
                .isEqualTo(Reason.PREVIOUS_ATTEMPT_FAILED);
    }

    @Test
    void blankKey_isRejected() {
        IdempotentRequest blank = new IdempotentRequest(userId, " ", TxType.TRANSFER, BigDecimal.TEN, targetWalletId);

        assertThatThrownBy(() -> idempotencyService.execute(blank, k -> null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
