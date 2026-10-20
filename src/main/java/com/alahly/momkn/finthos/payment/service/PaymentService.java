package com.alahly.momkn.finthos.payment.service;

import com.alahly.momkn.finthos.integration.client.ProcessorClient;
import com.alahly.momkn.finthos.integration.client.ProcessorRequest;
import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.domain.TxType;
import com.alahly.momkn.finthos.transaction.service.IdempotencyService;
import com.alahly.momkn.finthos.transaction.service.TransactionService;
import com.alahly.momkn.finthos.user.service.UserService;
import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Processor-backed money flows. Deliberately not {@code @Transactional} as a whole:
 * <ol>
 *   <li>the PENDING row is committed before the processor is called,</li>
 *   <li>the processor call runs with no DB transaction (or connection) held,</li>
 *   <li>only "move funds + ledger + SUCCESS" runs in one DB transaction (NFR-3),</li>
 *   <li>on any failure the FAILED status is committed on its own, so it survives.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final TransactionService transactionService;
    private final ProcessorClient processorClient;
    private final WalletService walletService;
    private final IdempotencyService idempotencyService;
    private final UserService userService;
    private final TransactionOperations transactionOperations;

    public Transaction topUp(UUID userId, BigDecimal amount, String idempotencyKey) {
        Optional<Transaction> existing = idempotencyService.findExisting(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Wallet wallet = walletService.getByUserId(userId);

        Transaction tx = transactionService.create(
                TxType.TOP_UP, amount, idempotencyKey, null, wallet.getId());

        authorize(tx, wallet.getCurrency());
        return settle(tx, () -> walletService.credit(wallet.getId(), amount, tx.getId()));
    }

    public Transaction payMerchant(UUID userId, UUID merchantId, BigDecimal amount, String idempotencyKey) {
        Optional<Transaction> existing = idempotencyService.findExisting(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (userId.equals(merchantId)) {
            throw new IllegalArgumentException("Cannot pay yourself");
        }
        userService.requireMerchant(merchantId);

        Wallet senderWallet = walletService.getByUserId(userId);
        Wallet merchantWallet = walletService.getByUserId(merchantId);

        // Fail fast before asking the processor to authorize money the user doesn't have.
        senderWallet.ensureCanDebit(amount);

        Transaction tx = transactionService.create(
                TxType.PAYMENT, amount, idempotencyKey,
                senderWallet.getId(), merchantWallet.getId());

        authorize(tx, senderWallet.getCurrency());
        return settle(tx, () -> {
            walletService.debit(senderWallet.getId(), amount, tx.getId());
            walletService.credit(merchantWallet.getId(), amount, tx.getId());
        });
    }

    private void authorize(Transaction tx, String currency) {
        try {
            ProcessorRequest req = new ProcessorRequest(tx.getId().toString(), tx.getAmount(), currency);
            processorClient.authorizeWithCircuitBreaker(tx.getId(), req);
        } catch (RuntimeException e) {
            transactionService.markFailed(tx);
            throw e;
        }
    }

    private Transaction settle(Transaction tx, Runnable moveFunds) {
        try {
            return transactionOperations.execute(status -> {
                moveFunds.run();
                transactionService.markSuccess(tx);
                return tx;
            });
        } catch (RuntimeException e) {
            // The processor already approved but funds could not be moved (e.g. balance spent
            // concurrently). Needs reconciliation / a processor void, which is out of scope.
            log.error("Settlement failed after processor approval for tx {}", tx.getId(), e);
            transactionService.markFailed(tx);
            throw e;
        }
    }
}
