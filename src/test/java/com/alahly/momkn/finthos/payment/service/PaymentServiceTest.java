package com.alahly.momkn.finthos.payment.service;

import com.alahly.momkn.finthos.common.error.InsufficientFundsException;
import com.alahly.momkn.finthos.common.error.ProcessorDeclinedException;
import com.alahly.momkn.finthos.common.error.ProcessorTimeoutException;
import com.alahly.momkn.finthos.integration.client.ProcessorClient;
import com.alahly.momkn.finthos.integration.client.ProcessorRequest;
import com.alahly.momkn.finthos.integration.client.ProcessorResponse;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import com.alahly.momkn.finthos.transaction.service.IdempotencyService;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.user.service.UserService;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private ProcessorClient processorClient;
    @Mock
    private WalletService walletService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserService userService;

    private PaymentService paymentService;

    private UUID userId;
    private UUID merchantId;
    private Wallet userWallet;
    private Wallet merchantWallet;
    private Transaction pendingTx;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(transactionService, processorClient, walletService,
                new IdempotencyService(transactionRepository), userService,
                TransactionOperations.withoutTransaction());

        userId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        userWallet = Wallet.create(userId, "USD");
        userWallet.credit(new BigDecimal("200.00"));
        merchantWallet = Wallet.create(merchantId, "USD");
        pendingTx = Transaction.create(TxType.TOP_UP, new BigDecimal("50.00"),
                userId + ":idem-1", null, userWallet.getId());
    }

    private void givenTopUpPending() {
        when(transactionRepository.findByIdempotencyKey(userId + ":idem-1")).thenReturn(Optional.empty());
        when(walletService.getByUserId(userId)).thenReturn(userWallet);
        when(transactionService.create(eq(TxType.TOP_UP), eq(new BigDecimal("50.00")),
                eq(userId + ":idem-1"), eq(null), eq(userWallet.getId())))
                .thenReturn(pendingTx);
    }

    private void givenPaymentPending() {
        when(transactionRepository.findByIdempotencyKey(userId + ":idem-2")).thenReturn(Optional.empty());
        when(walletService.getByUserId(userId)).thenReturn(userWallet);
        when(walletService.getByUserId(merchantId)).thenReturn(merchantWallet);
        when(transactionService.create(eq(TxType.PAYMENT), eq(new BigDecimal("30.00")),
                eq(userId + ":idem-2"), eq(userWallet.getId()), eq(merchantWallet.getId())))
                .thenReturn(pendingTx);
    }

    @Test
    void topUp_approved_createsPendingThenAuthorizesThenCreditsAndMarksSuccess() {
        givenTopUpPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenReturn(new ProcessorResponse(pendingTx.getId().toString(), "APPROVED", "AUTH-123"));

        paymentService.topUp(userId, new BigDecimal("50.00"), "idem-1");

        InOrder order = inOrder(transactionService, processorClient, walletService);
        order.verify(transactionService).create(any(), any(), anyString(), any(), any());
        order.verify(processorClient).authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class));
        order.verify(walletService).credit(eq(userWallet.getId()), eq(new BigDecimal("50.00")), eq(pendingTx.getId()));
        order.verify(transactionService).markSuccess(pendingTx);
        verify(transactionService, never()).markFailed(any());
    }

    @Test
    void topUp_declined_doesNotCreditAndMarksFailed() {
        givenTopUpPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorDeclinedException(pendingTx.getId().toString()));

        assertThrows(ProcessorDeclinedException.class,
                () -> paymentService.topUp(userId, new BigDecimal("50.00"), "idem-1"));

        verify(walletService, never()).credit(any(), any(), any());
        verify(transactionService).markFailed(pendingTx);
    }

    @Test
    void topUp_timeout_doesNotCreditAndMarksFailed() {
        givenTopUpPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorTimeoutException(pendingTx.getId().toString()));

        assertThrows(ProcessorTimeoutException.class,
                () -> paymentService.topUp(userId, new BigDecimal("50.00"), "idem-1"));

        verify(walletService, never()).credit(any(), any(), any());
        verify(transactionService).markFailed(pendingTx);
    }

    @Test
    void topUp_settlementFailsAfterApproval_marksFailed() {
        givenTopUpPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenReturn(new ProcessorResponse(pendingTx.getId().toString(), "APPROVED", "AUTH-123"));
        when(walletService.credit(any(), any(), any())).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class,
                () -> paymentService.topUp(userId, new BigDecimal("50.00"), "idem-1"));

        verify(transactionService).markFailed(pendingTx);
    }

    @Test
    void topUp_existingIdempotencyKey_returnsCachedResult() {
        Transaction existing = Transaction.create(TxType.TOP_UP, new BigDecimal("50.00"),
                userId + ":idem-1", null, userWallet.getId());
        existing.markPersisted();
        existing.markSuccess();
        when(walletService.getByUserId(userId)).thenReturn(userWallet);
        when(transactionRepository.findByIdempotencyKey(userId + ":idem-1")).thenReturn(Optional.of(existing));

        Transaction result = paymentService.topUp(userId, new BigDecimal("50.00"), "idem-1");

        assertThat(result).isSameAs(existing);
        verify(transactionService, never()).create(any(), any(), any(), any(), any());
        verify(processorClient, never()).authorizeWithCircuitBreaker(any(), any());
    }

    @Test
    void payMerchant_approved_debitsSenderCreditsReceiverAndMarksSuccess() {
        givenPaymentPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenReturn(new ProcessorResponse(pendingTx.getId().toString(), "APPROVED", "AUTH-456"));

        paymentService.payMerchant(userId, merchantId, new BigDecimal("30.00"), "idem-2");

        verify(userService).requireMerchant(merchantId);
        verify(walletService).debit(eq(userWallet.getId()), eq(new BigDecimal("30.00")), eq(pendingTx.getId()));
        verify(walletService).credit(eq(merchantWallet.getId()), eq(new BigDecimal("30.00")), eq(pendingTx.getId()));
        verify(transactionService).markSuccess(pendingTx);
    }

    @Test
    void payMerchant_insufficientFunds_failsBeforeCallingProcessor() {
        when(transactionRepository.findByIdempotencyKey(userId + ":idem-2")).thenReturn(Optional.empty());
        when(walletService.getByUserId(userId)).thenReturn(userWallet);
        when(walletService.getByUserId(merchantId)).thenReturn(merchantWallet);

        assertThrows(InsufficientFundsException.class,
                () -> paymentService.payMerchant(userId, merchantId, new BigDecimal("500.00"), "idem-2"));

        verify(processorClient, never()).authorizeWithCircuitBreaker(any(), any());
        verify(transactionService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void payMerchant_toSelf_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.payMerchant(userId, userId, new BigDecimal("30.00"), "idem-2"));

        verify(processorClient, never()).authorizeWithCircuitBreaker(any(), any());
    }

    @Test
    void payMerchant_declined_doesNotMoveFundsAndMarksFailed() {
        givenPaymentPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorDeclinedException(pendingTx.getId().toString()));

        assertThrows(ProcessorDeclinedException.class,
                () -> paymentService.payMerchant(userId, merchantId, new BigDecimal("30.00"), "idem-2"));

        verify(walletService, never()).debit(any(), any(), any());
        verify(walletService, never()).credit(any(), any(), any());
        verify(transactionService).markFailed(pendingTx);
    }

    @Test
    void payMerchant_timeout_doesNotMoveFundsAndMarksFailed() {
        givenPaymentPending();
        when(processorClient.authorizeWithCircuitBreaker(eq(pendingTx.getId()), any(ProcessorRequest.class)))
                .thenThrow(new ProcessorTimeoutException(pendingTx.getId().toString()));

        assertThrows(ProcessorTimeoutException.class,
                () -> paymentService.payMerchant(userId, merchantId, new BigDecimal("30.00"), "idem-2"));

        verify(walletService, never()).debit(any(), any(), any());
        verify(walletService, never()).credit(any(), any(), any());
        verify(transactionService).markFailed(pendingTx);
    }

    @Test
    void payMerchant_existingIdempotencyKey_returnsCachedResult() {
        Transaction existing = Transaction.create(TxType.PAYMENT, new BigDecimal("30.00"),
                userId + ":idem-2", userWallet.getId(), merchantWallet.getId());
        existing.markPersisted();
        existing.markSuccess();
        when(walletService.getByUserId(userId)).thenReturn(userWallet);
        when(walletService.getByUserId(merchantId)).thenReturn(merchantWallet);
        when(transactionRepository.findByIdempotencyKey(userId + ":idem-2")).thenReturn(Optional.of(existing));

        Transaction result = paymentService.payMerchant(userId, merchantId, new BigDecimal("30.00"), "idem-2");

        assertThat(result).isSameAs(existing);
        verify(transactionService, never()).create(any(), any(), any(), any(), any());
    }
}
