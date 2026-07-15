package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransactionService transactionService;
    private final WalletService walletService;

    @Transactional
    public Transaction transfer(UUID fromUserId, UUID toUserId, BigDecimal amount, String idempotencyKey) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        Wallet senderWallet = walletService.getByUserId(fromUserId);
        Wallet receiverWallet = walletService.getByUserId(toUserId);

        Transaction tx = transactionService.create(
                TxType.TRANSFER, amount, idempotencyKey,
                senderWallet.getId(), receiverWallet.getId());

        walletService.debit(senderWallet.getId(), amount, tx.getId());
        walletService.credit(receiverWallet.getId(), amount, tx.getId());

        transactionService.markSuccess(tx);
        return tx;
    }
}
