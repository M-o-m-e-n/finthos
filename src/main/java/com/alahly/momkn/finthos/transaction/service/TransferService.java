package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final IdempotencyService idempotencyService;
    private final TransactionOperations transactionOperations;

    public Transaction transfer(UUID fromUserId, UUID toUserId, BigDecimal amount, String idempotencyKey) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        Wallet senderWallet = walletService.getByUserId(fromUserId);
        Wallet receiverWallet = walletService.getByUserId(toUserId);

        // The DB transaction is opened inside the idempotency guard so a duplicate-key race
        // rolls back cleanly and can then be resolved by reading the winning row.
        return idempotencyService.execute(
                new IdempotentRequest(fromUserId, idempotencyKey, TxType.TRANSFER, amount, receiverWallet.getId()),
                scopedKey -> transactionOperations.execute(status -> {
                    Transaction tx = transactionService.create(
                            TxType.TRANSFER, amount, scopedKey,
                            senderWallet.getId(), receiverWallet.getId());

                    walletService.debit(senderWallet.getId(), amount, tx.getId());
                    walletService.credit(receiverWallet.getId(), amount, tx.getId());

                    transactionService.markSuccess(tx);
                    return tx;
                }));
    }
}
