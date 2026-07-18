package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.wallet.domain.InsufficientFundsException;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private WalletService walletService;
    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private TransferService transferService;

    private UUID senderUserId;
    private UUID receiverUserId;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private Transaction pendingTx;

    @BeforeEach
    void setUp() {
        senderUserId = UUID.randomUUID();
        receiverUserId = UUID.randomUUID();
        senderWallet = Wallet.create(senderUserId, "USD");
        senderWallet.credit(new BigDecimal("100.00"));
        receiverWallet = Wallet.create(receiverUserId, "USD");
        pendingTx = Transaction.create(TxType.TRANSFER, new BigDecimal("30.00"),
                "key-1", senderWallet.getId(), receiverWallet.getId());
    }

    @Test
    void transfer_debitsSenderCreditsReceiverAndMarksSuccess() {
        when(idempotencyService.findExisting("key-1")).thenReturn(Optional.empty());
        when(walletService.getByUserId(senderUserId)).thenReturn(senderWallet);
        when(walletService.getByUserId(receiverUserId)).thenReturn(receiverWallet);
        when(transactionService.create(eq(TxType.TRANSFER), eq(new BigDecimal("30.00")),
                eq("key-1"), eq(senderWallet.getId()), eq(receiverWallet.getId())))
                .thenReturn(pendingTx);

        Transaction result = transferService.transfer(senderUserId, receiverUserId,
                new BigDecimal("30.00"), "key-1");

        verify(walletService).debit(eq(senderWallet.getId()), eq(new BigDecimal("30.00")), eq(pendingTx.getId()));
        verify(walletService).credit(eq(receiverWallet.getId()), eq(new BigDecimal("30.00")), eq(pendingTx.getId()));
        verify(transactionService).markSuccess(pendingTx);
        assertThat(result).isNotNull();
    }

    @Test
    void transfer_insufficientFunds_throwsAndDoesNotCredit() {
        when(idempotencyService.findExisting("key-1")).thenReturn(Optional.empty());
        when(walletService.getByUserId(senderUserId)).thenReturn(senderWallet);
        when(walletService.getByUserId(receiverUserId)).thenReturn(receiverWallet);
        when(transactionService.create(eq(TxType.TRANSFER), eq(new BigDecimal("30.00")),
                eq("key-1"), eq(senderWallet.getId()), eq(receiverWallet.getId())))
                .thenReturn(pendingTx);
        when(walletService.debit(eq(senderWallet.getId()), eq(new BigDecimal("30.00")), eq(pendingTx.getId())))
                .thenThrow(new InsufficientFundsException(new BigDecimal("100.00"), new BigDecimal("30.00")));

        assertThrows(InsufficientFundsException.class,
                () -> transferService.transfer(senderUserId, receiverUserId,
                        new BigDecimal("30.00"), "key-1"));

        verify(walletService, never()).credit(any(), any(), any());
        verify(transactionService, never()).markSuccess(any());
    }

    @Test
    void transfer_selfTransfer_throwsImmediately() {
        assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(senderUserId, senderUserId,
                        new BigDecimal("10.00"), "key-1"));

        verify(transactionService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void transfer_existingIdempotencyKey_returnsCachedResult() {
        Transaction existing = Transaction.create(TxType.TRANSFER, new BigDecimal("30.00"),
                "key-1", senderWallet.getId(), receiverWallet.getId());
        existing.markPersisted();
        existing.markSuccess();
        when(idempotencyService.findExisting("key-1")).thenReturn(Optional.of(existing));

        Transaction result = transferService.transfer(senderUserId, receiverUserId,
                new BigDecimal("30.00"), "key-1");

        assertThat(result).isSameAs(existing);
        verify(transactionService, never()).create(any(), any(), any(), any(), any());
    }
}
