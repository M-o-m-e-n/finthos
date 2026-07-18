package com.alahly.momkn.finthos.payment.service;

import com.alahly.momkn.finthos.integration.client.ProcessorClient;
import com.alahly.momkn.finthos.integration.client.ProcessorRequest;
import com.alahly.momkn.finthos.integration.client.ProcessorResponse;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxStatus;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.IdempotencyService;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionService transactionService;
    private final ProcessorClient processorClient;
    private final WalletService walletService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public Transaction topUp(UUID userId, BigDecimal amount, String idempotencyKey) {
        Optional<Transaction> existing = idempotencyService.findExisting(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Wallet wallet = walletService.getByUserId(userId);

        Transaction tx = transactionService.create(
                TxType.TOP_UP, amount, idempotencyKey, null, wallet.getId());

        try {
            ProcessorRequest req = new ProcessorRequest(tx.getId().toString(), amount, wallet.getCurrency());
            ProcessorResponse response = processorClient.authorizeWithRetry(tx.getId(), req);

            walletService.credit(wallet.getId(), amount, tx.getId());
            transactionService.markSuccess(tx);

            return tx;
        } catch (Exception e) {
            transactionService.markFailed(tx);
            throw e;
        }
    }

    @Transactional
    public Transaction payMerchant(UUID userId, UUID merchantId, BigDecimal amount, String idempotencyKey) {
        Optional<Transaction> existing = idempotencyService.findExisting(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Wallet senderWallet = walletService.getByUserId(userId);
        Wallet merchantWallet = walletService.getByUserId(merchantId);

        Transaction tx = transactionService.create(
                TxType.PAYMENT, amount, idempotencyKey,
                senderWallet.getId(), merchantWallet.getId());

        try {
            ProcessorRequest req = new ProcessorRequest(tx.getId().toString(), amount, senderWallet.getCurrency());
            ProcessorResponse response = processorClient.authorizeWithRetry(tx.getId(), req);

            walletService.debit(senderWallet.getId(), amount, tx.getId());
            walletService.credit(merchantWallet.getId(), amount, tx.getId());
            transactionService.markSuccess(tx);

            return tx;
        } catch (Exception e) {
            transactionService.markFailed(tx);
            throw e;
        }
    }
}
