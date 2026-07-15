package com.alahly.momkn.finthos.transaction.repository;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
