package com.alahly.momkn.finthos.transaction.repository;

import com.alahly.momkn.finthos.transaction.domain.Transaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends CrudRepository<Transaction, UUID>, PagingAndSortingRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT * FROM transactions " +
           "WHERE (source_wallet_id = :walletId OR target_wallet_id = :walletId) " +
           "AND (:type IS NULL OR type = :type) " +
           "AND (:fromDate IS NULL OR created_at >= :fromDate) " +
           "AND (:toDate IS NULL OR created_at <= :toDate) " +
           "ORDER BY created_at DESC " +
           "LIMIT :limit OFFSET :offset")
    List<Transaction> findByWalletAndFilters(UUID walletId, String type, Instant fromDate, Instant toDate,
                                             int limit, int offset);

    @Query("SELECT COUNT(*) FROM transactions " +
           "WHERE (source_wallet_id = :walletId OR target_wallet_id = :walletId) " +
           "AND (:type IS NULL OR type = :type) " +
           "AND (:fromDate IS NULL OR created_at >= :fromDate) " +
           "AND (:toDate IS NULL OR created_at <= :toDate)")
    long countByWalletAndFilters(UUID walletId, String type, Instant fromDate, Instant toDate);
}
