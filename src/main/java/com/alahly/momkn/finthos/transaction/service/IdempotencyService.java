package com.alahly.momkn.finthos.transaction.service;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import com.alahly.momkn.finthos.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class    IdempotencyService {

    private final TransactionRepository transactionRepository;

    public Optional<Transaction> findExisting(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }
}
