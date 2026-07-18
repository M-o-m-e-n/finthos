package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void create_savesTransactionWithPendingStatus() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.create(
                TxType.TRANSFER, new BigDecimal("25.00"), "key-1",
                UUID.randomUUID(), UUID.randomUUID());

        assertThat(result.getType()).isEqualTo(TxType.TRANSFER);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.getStatus()).isEqualTo(TxStatus.PENDING);
        assertThat(result.getIdempotencyKey()).isEqualTo("key-1");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void markSuccess_updatesStatusToSuccess() {
        Transaction tx = Transaction.create(TxType.TOP_UP, new BigDecimal("10.00"),
                "k", null, UUID.randomUUID());
        tx.markPersisted();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.markSuccess(tx);

        assertThat(tx.getStatus()).isEqualTo(TxStatus.SUCCESS);
        verify(transactionRepository).save(tx);
    }

    @Test
    void markFailed_updatesStatusToFailed() {
        Transaction tx = Transaction.create(TxType.TOP_UP, new BigDecimal("10.00"),
                "k", null, UUID.randomUUID());
        tx.markPersisted();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.markFailed(tx);

        assertThat(tx.getStatus()).isEqualTo(TxStatus.FAILED);
        verify(transactionRepository).save(tx);
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(transactionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.findById(UUID.randomUUID()));
    }

    @Test
    void findById_returnsTransaction() {
        Transaction tx = Transaction.create(TxType.TRANSFER, new BigDecimal("10.00"),
                "k", UUID.randomUUID(), UUID.randomUUID());
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        Transaction result = transactionService.findById(tx.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(tx.getId());
    }
}
