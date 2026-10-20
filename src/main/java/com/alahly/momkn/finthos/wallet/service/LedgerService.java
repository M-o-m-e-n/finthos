package com.alahly.momkn.finthos.wallet.service;

import com.alahly.momkn.finthos.wallet.domain.LedgerEntry;
import com.alahly.momkn.finthos.wallet.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class LedgerService {

    private final LedgerRepository ledgerRepository;

    /** Must join the caller's transaction so the entry commits or rolls back with the balance change (NFR-3). */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry record(UUID walletId, UUID transactionId, BigDecimal delta, BigDecimal balanceAfter) {
        return ledgerRepository.save(LedgerEntry.create(walletId, transactionId, delta, balanceAfter));
    }
}
